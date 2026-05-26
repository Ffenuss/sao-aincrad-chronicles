package com.sao.aincrad.net

class CoopCoordinator(
    private val localPlayerId: String,
    private val sessionId: String,
    ticksPerSecond: Int = 20,
    transport: NetTransport = AdaptiveNetTransport(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    enum class Phase {
        OFFLINE,
        LOBBY,
        MATCH,
    }

    data class State(
        val phase: Phase = Phase.OFFLINE,
        val roomCode: String = "",
        val isHost: Boolean = false,
        val isConnected: Boolean = false,
        val localReady: Boolean = false,
        val remoteReadyCount: Int = 0,
        val allReady: Boolean = false,
        val localTick: Long = 0,
        val messagesIn: Long = 0,
        val messagesOut: Long = 0,
        val averageRttMs: Float? = null,
        val packetLossPercent: Float = 0f,
        val tickDrift: Long = 0L,
        val desyncWarning: Boolean = false,
        val remoteTrafficSeen: Boolean = false,
        val timedOut: Boolean = false,
        val reconnectAvailable: Boolean = false,
        val reconnectRemainingMs: Long = 0L,
    )

    companion object {
        const val EVENT_MATCH_START_REQUEST = "match_start_request"
        const val EVENT_MATCH_STARTED = "match_started"
        private const val RECONNECT_WINDOW_MS = 30_000L
        private const val REMOTE_TIMEOUT_TICKS = 200L
    }

    private val session = CoopSessionManager(
        transport = transport,
        localPlayerId = localPlayerId,
        sessionId = sessionId,
        ticksPerSecond = ticksPerSecond,
    )

    private var roomCode: String = ""
    private var phase: Phase = Phase.OFFLINE
    private var localStartRequested = false
    private val incomingGameplayEvents = ArrayDeque<GameEventMessage>()
    private var reconnectDeadlineMs: Long = 0L
    private var reconnectRoomCode: String = ""
    private var reconnectAsHost: Boolean = false
    private var reconnectPhase: Phase = Phase.LOBBY
    private var reconnectLocalReady: Boolean = false
    private var lastDisconnectTimedOut = false

    fun host(roomCode: String): Result<Unit> {
        val result = session.host(roomCode)
        if (result.isSuccess) {
            this.roomCode = roomCode
            phase = Phase.LOBBY
            session.setReady(false)
            clearReconnectWindow()
            lastDisconnectTimedOut = false
        }
        return result
    }

    fun join(roomCode: String): Result<Unit> {
        val result = session.join(roomCode)
        if (result.isSuccess) {
            this.roomCode = roomCode
            phase = Phase.LOBBY
            session.setReady(false)
            clearReconnectWindow()
            lastDisconnectTimedOut = false
        }
        return result
    }

    fun disconnect() {
        captureReconnectWindowIfPossible()
        session.disconnect()
        phase = Phase.OFFLINE
        roomCode = ""
        localStartRequested = false
        incomingGameplayEvents.clear()
        lastDisconnectTimedOut = false
    }

    fun reconnect(): Result<Unit> {
        val remainingMs = reconnectRemainingMs()
        if (remainingMs <= 0L || reconnectRoomCode.isBlank()) {
            return Result.failure(IllegalStateException("Reconnect window expired"))
        }
        val restorePhase = reconnectPhase
        val restoreReady = reconnectLocalReady
        val result = if (reconnectAsHost) host(reconnectRoomCode) else join(reconnectRoomCode)
        return result.onSuccess {
            phase = restorePhase
            session.setReady(restoreReady)
            localStartRequested = false
            lastDisconnectTimedOut = false
        }
    }

    fun setReady(ready: Boolean) {
        if (phase != Phase.LOBBY) return
        session.setReady(ready)
    }

    fun requestMatchStart() {
        if (phase != Phase.LOBBY) return
        localStartRequested = true
        session.sendGameEvent(EVENT_MATCH_START_REQUEST)
    }

    fun sendGameplayEvent(
        eventType: String,
        targetId: String? = null,
        value: Int? = null,
    ) {
        if (phase != Phase.MATCH) return
        session.sendGameEvent(eventType = eventType, targetId = targetId, value = value)
    }

    fun enqueueInput(input: PlayerInputCommand) {
        if (phase != Phase.MATCH) return
        session.enqueueLocalInput(input)
    }

    fun consumeLatestSnapshot(): WorldSnapshot? = session.consumeLatestSnapshot()

    fun drainIncomingInputs(): List<PlayerInputCommand> = session.drainIncomingInputs()

    fun publishSnapshot(snapshot: WorldSnapshot) {
        if (phase != Phase.MATCH) return
        session.publishSnapshot(snapshot)
    }

    fun drainGameplayEvents(): List<GameEventMessage> {
        if (incomingGameplayEvents.isEmpty()) return emptyList()
        val out = ArrayList<GameEventMessage>(incomingGameplayEvents.size)
        while (incomingGameplayEvents.isNotEmpty()) {
            out += incomingGameplayEvents.removeFirst()
        }
        return out
    }

    fun update(deltaSeconds: Float) {
        session.update(deltaSeconds)
        val events = session.drainIncomingEvents()
        events.filterTo(incomingGameplayEvents) {
            it.eventType != EVENT_MATCH_START_REQUEST && it.eventType != EVENT_MATCH_STARTED
        }

        val sessionState = session.sessionState()
        val hostControlsStart = sessionState.role == CoopSessionManager.Role.HOST
        if (phase == Phase.LOBBY && hostControlsStart && sessionState.localReady && session.allReady()) {
            val requestedByAnyone = localStartRequested || events.any { it.eventType == EVENT_MATCH_START_REQUEST }
            if (requestedByAnyone) {
                session.sendGameEvent(EVENT_MATCH_STARTED)
                phase = Phase.MATCH
                localStartRequested = false
                return
            }
        }

        if (phase == Phase.LOBBY && events.any { it.eventType == EVENT_MATCH_STARTED }) {
            phase = Phase.MATCH
            localStartRequested = false
        }

        val timeoutReached = phase == Phase.MATCH &&
            sessionState.isConnected &&
            sessionState.remoteTrafficSeen &&
            sessionState.localTick - sessionState.lastRemoteTick >= REMOTE_TIMEOUT_TICKS
        if (timeoutReached) {
            lastDisconnectTimedOut = true
            disconnectInternal()
        }
    }

    fun state(): State {
        val s = session.sessionState()
        val reconnectRemainingMs = reconnectRemainingMs()
        return State(
            phase = phase,
            roomCode = roomCode,
            isHost = s.role == CoopSessionManager.Role.HOST,
            isConnected = s.isConnected,
            localReady = s.localReady,
            remoteReadyCount = s.remoteReadyCount,
            allReady = s.localReady && session.allReady(),
            localTick = s.localTick,
            messagesIn = s.receivedMessages,
            messagesOut = s.sentMessages,
            averageRttMs = s.averageRttMs,
            packetLossPercent = s.packetLossPercent,
            tickDrift = s.tickDrift,
            desyncWarning = s.desyncWarning,
            remoteTrafficSeen = s.remoteTrafficSeen,
            timedOut = lastDisconnectTimedOut,
            reconnectAvailable = reconnectRemainingMs > 0L,
            reconnectRemainingMs = reconnectRemainingMs,
        )
    }

    private fun disconnectInternal() {
        captureReconnectWindowIfPossible()
        session.disconnect()
        phase = Phase.OFFLINE
        roomCode = ""
        localStartRequested = false
        incomingGameplayEvents.clear()
    }

    private fun captureReconnectWindowIfPossible() {
        val s = session.sessionState()
        if (!s.isConnected || roomCode.isBlank()) return
        reconnectRoomCode = roomCode
        reconnectAsHost = s.role == CoopSessionManager.Role.HOST
        reconnectPhase = phase
        reconnectLocalReady = s.localReady
        reconnectDeadlineMs = nowMillis() + RECONNECT_WINDOW_MS
    }

    private fun clearReconnectWindow() {
        reconnectDeadlineMs = 0L
        reconnectRoomCode = ""
        reconnectAsHost = false
        reconnectPhase = Phase.LOBBY
        reconnectLocalReady = false
    }

    private fun reconnectRemainingMs(): Long {
        if (reconnectDeadlineMs <= 0L) return 0L
        return (reconnectDeadlineMs - nowMillis()).coerceAtLeast(0L)
    }
}
