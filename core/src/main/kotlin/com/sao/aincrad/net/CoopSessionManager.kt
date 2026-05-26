package com.sao.aincrad.net

class CoopSessionManager(
    private val transport: NetTransport,
    private val localPlayerId: String,
    private val sessionId: String,
    ticksPerSecond: Int = 20,
) {
    enum class Role {
        HOST,
        CLIENT,
    }

    data class SessionState(
        val isConnected: Boolean = false,
        val role: Role? = null,
        val endpoint: String = "",
        val localTick: Long = 0,
        val lastRemoteTick: Long = 0,
        val receivedMessages: Long = 0,
        val sentMessages: Long = 0,
        val localReady: Boolean = false,
        val remoteReadyCount: Int = 0,
        val averageRttMs: Float? = null,
        val packetLossPercent: Float = 0f,
        val tickDrift: Long = 0L,
        val desyncWarning: Boolean = false,
        val remoteTrafficSeen: Boolean = false,
    )

    private val tickClock = FixedTickClock(ticksPerSecond)
    private val pingIntervalTicks = maxOf(1, ticksPerSecond)
    private var role: Role? = null
    private var endpoint: String = ""
    private var sequence: Long = 0
    private var pingSequence: Long = 0
    private var lastPingTick: Long = Long.MIN_VALUE
    private var receivedMessages: Long = 0
    private var sentMessages: Long = 0
    private var lastRemoteTick: Long = 0
    private var averageRttMs: Float = -1f
    private var estimatedDroppedPackets: Long = 0
    private val lastSequenceBySender = mutableMapOf<String, Long>()
    private var remoteTrafficSeen = false
    private var localReady = false
    private val remoteReadyByPlayerId = mutableMapOf<String, Boolean>()
    private val pendingPingSentNanos = mutableMapOf<Long, Long>()

    private val pendingInputs = ArrayDeque<PlayerInputCommand>()
    private val incomingInputs = ArrayDeque<PlayerInputCommand>()
    private val incomingEvents = ArrayDeque<GameEventMessage>()
    private var latestSnapshot: WorldSnapshot? = null

    fun host(roomCode: String): Result<Unit> {
        role = Role.HOST
        endpoint = roomCode
        return transport.connect("host|$roomCode", localPlayerId)
    }

    fun join(roomCode: String): Result<Unit> {
        role = Role.CLIENT
        endpoint = roomCode
        return transport.connect("join|$roomCode", localPlayerId)
    }

    fun disconnect() {
        transport.disconnect()
        role = null
        endpoint = ""
        localReady = false
        remoteReadyByPlayerId.clear()
        pendingInputs.clear()
        incomingInputs.clear()
        latestSnapshot = null
        pendingPingSentNanos.clear()
        averageRttMs = -1f
        lastPingTick = Long.MIN_VALUE
        estimatedDroppedPackets = 0
        lastSequenceBySender.clear()
        remoteTrafficSeen = false
    }

    fun setReady(ready: Boolean) {
        localReady = ready
        if (!transport.isConnected) return
        send(NetMessageType.HELLO, tickClock.tick, serializeHello(localPlayerId, ready))
    }

    fun allReady(minPlayers: Int = 2): Boolean {
        if (!localReady) return false
        val readyRemote = remoteReadyByPlayerId.values.count { it }
        return (1 + readyRemote) >= minPlayers
    }

    fun enqueueLocalInput(input: PlayerInputCommand) {
        pendingInputs.addLast(input)
    }

    fun consumeLatestSnapshot(): WorldSnapshot? {
        val snapshot = latestSnapshot
        latestSnapshot = null
        return snapshot
    }

    fun drainIncomingEvents(): List<GameEventMessage> {
        if (incomingEvents.isEmpty()) return emptyList()
        val result = ArrayList<GameEventMessage>(incomingEvents.size)
        while (incomingEvents.isNotEmpty()) {
            result += incomingEvents.removeFirst()
        }
        return result
    }

    fun sendGameEvent(
        eventType: String,
        actorId: String = localPlayerId,
        targetId: String? = null,
        value: Int? = null,
    ) {
        if (!transport.isConnected) return
        val event = GameEventMessage(
            eventId = "${eventType}_${tickClock.tick}_${sequence + 1}",
            eventType = eventType,
            actorId = actorId,
            targetId = targetId,
            value = value,
        )
        send(NetMessageType.GAME_EVENT, tickClock.tick, serializeGameEvent(event))
    }

    fun drainIncomingInputs(): List<PlayerInputCommand> {
        if (incomingInputs.isEmpty()) return emptyList()
        val result = ArrayList<PlayerInputCommand>(incomingInputs.size)
        while (incomingInputs.isNotEmpty()) {
            result += incomingInputs.removeFirst()
        }
        return result
    }

    fun publishSnapshot(snapshot: WorldSnapshot) {
        if (!transport.isConnected) return
        send(NetMessageType.STATE_SNAPSHOT, snapshot.tick, serializeSnapshot(snapshot))
    }

    fun update(deltaSeconds: Float) {
        if (!transport.isConnected) return

        tickClock.consume(deltaSeconds) { tick, _ ->
            maybeSendPing(tick)
            flushOutgoing(tick)
            processIncoming()
        }
    }

    fun sessionState(): SessionState {
        val drift = tickClock.tick - lastRemoteTick
        return SessionState(
            isConnected = transport.isConnected,
            role = role,
            endpoint = endpoint,
            localTick = tickClock.tick,
            lastRemoteTick = lastRemoteTick,
            receivedMessages = receivedMessages,
            sentMessages = sentMessages,
            localReady = localReady,
            remoteReadyCount = remoteReadyByPlayerId.values.count { it },
            averageRttMs = averageRttMs.takeIf { it >= 0f },
            packetLossPercent = packetLossPercent(),
            tickDrift = drift,
            desyncWarning = kotlin.math.abs(drift) >= 10L,
            remoteTrafficSeen = remoteTrafficSeen,
        )
    }

    private fun flushOutgoing(tick: Long) {
        while (pendingInputs.isNotEmpty()) {
            val input = pendingInputs.removeFirst()
            send(NetMessageType.INPUT_COMMAND, tick, serializeInput(input))
        }
    }

    private fun processIncoming() {
        transport.pollIncoming(128).forEach { envelope ->
            if (envelope.sessionId != sessionId || envelope.protocolVersion != NET_PROTOCOL_VERSION) return@forEach
            receivedMessages += 1
            trackIncomingSequence(envelope.senderId, envelope.sequence)
            remoteTrafficSeen = true
            lastRemoteTick = maxOf(lastRemoteTick, envelope.tick)
            when (envelope.type) {
                NetMessageType.INPUT_COMMAND -> {
                    deserializeInput(envelope.payload)?.let { incomingInputs += it }
                }
                NetMessageType.STATE_SNAPSHOT -> {
                    latestSnapshot = deserializeSnapshot(envelope.payload)
                }
                NetMessageType.ACK,
                NetMessageType.PING,
                -> {
                    val pingSeq = envelope.payload.toLongOrNull() ?: return@forEach
                    send(NetMessageType.PONG, tickClock.tick, pingSeq.toString())
                }
                NetMessageType.PONG -> {
                    val pingSeq = envelope.payload.toLongOrNull() ?: return@forEach
                    val startedAt = pendingPingSentNanos.remove(pingSeq) ?: return@forEach
                    val rttMs = ((System.nanoTime() - startedAt).toDouble() / 1_000_000.0).toFloat().coerceAtLeast(0f)
                    averageRttMs = if (averageRttMs < 0f) {
                        rttMs
                    } else {
                        averageRttMs * 0.8f + rttMs * 0.2f
                    }
                }
                NetMessageType.HELLO -> {
                    val hello = deserializeHello(envelope.payload) ?: return@forEach
                    if (hello.playerId != localPlayerId) {
                        remoteReadyByPlayerId[hello.playerId] = hello.ready
                    }
                }
                NetMessageType.GAME_EVENT -> {
                    deserializeGameEvent(envelope.payload)?.let { incomingEvents += it }
                }
            }
        }
    }

    private fun send(type: NetMessageType, tick: Long, payload: String) {
        val envelope = NetEnvelope(
            sessionId = sessionId,
            senderId = localPlayerId,
            sequence = ++sequence,
            tick = tick,
            type = type,
            payload = payload,
        )
        transport.send(envelope).onSuccess {
            sentMessages += 1
        }
    }

    private fun maybeSendPing(tick: Long) {
        if (!transport.isConnected) return
        if (lastPingTick != Long.MIN_VALUE && (tick - lastPingTick) < pingIntervalTicks) return
        val pingSeq = ++pingSequence
        pendingPingSentNanos[pingSeq] = System.nanoTime()
        send(NetMessageType.PING, tick, pingSeq.toString())
        lastPingTick = tick
    }

    private fun trackIncomingSequence(senderId: String, sequence: Long) {
        val previous = lastSequenceBySender[senderId]
        if (previous != null && sequence > previous + 1) {
            estimatedDroppedPackets += (sequence - previous - 1)
        }
        if (previous == null || sequence > previous) {
            lastSequenceBySender[senderId] = sequence
        }
    }

    private fun packetLossPercent(): Float {
        val totalExpected = receivedMessages + estimatedDroppedPackets
        if (totalExpected <= 0L) return 0f
        return (estimatedDroppedPackets.toDouble() * 100.0 / totalExpected.toDouble()).toFloat()
    }

    private fun serializeInput(input: PlayerInputCommand): String {
        return listOf(
            input.playerId,
            input.moveX.toString(),
            input.moveY.toString(),
            input.attackPressed.toString(),
            input.dodgePressed.toString(),
            input.skillSlot?.toString() ?: "",
        ).joinToString("|")
    }

    private fun deserializeInput(raw: String): PlayerInputCommand? {
        val parts = raw.split("|")
        val playerId = parts.getOrNull(0)?.ifBlank { return null } ?: return null
        return PlayerInputCommand(
            playerId = playerId,
            moveX = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
            moveY = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
            attackPressed = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
            dodgePressed = parts.getOrNull(4)?.toBooleanStrictOrNull() ?: false,
            skillSlot = parts.getOrNull(5)?.toIntOrNull(),
        )
    }

    private fun serializeSnapshot(snapshot: WorldSnapshot): String {
        val playersRaw = snapshot.players.joinToString("~") {
            listOf(it.playerId, it.x, it.y, it.hp, it.facing, it.actionState).joinToString(",")
        }
        val enemiesRaw = snapshot.enemies.joinToString("~") {
            listOf(it.enemyId, it.x, it.y, it.hp, it.isDead).joinToString(",")
        }
        return listOf(snapshot.tick, snapshot.floorNumber, playersRaw, enemiesRaw).joinToString("|")
    }

    private fun deserializeSnapshot(raw: String): WorldSnapshot {
        val parts = raw.split("|")
        val tick = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val floor = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val players = parts.getOrNull(2)
            ?.split("~")
            ?.mapNotNull { item ->
                if (item.isBlank()) return@mapNotNull null
                val fields = item.split(",")
                val id = fields.getOrNull(0) ?: return@mapNotNull null
                ReplicatedPlayerState(
                    playerId = id,
                    x = fields.getOrNull(1)?.toFloatOrNull() ?: 0f,
                    y = fields.getOrNull(2)?.toFloatOrNull() ?: 0f,
                    hp = fields.getOrNull(3)?.toIntOrNull() ?: 0,
                    facing = fields.getOrNull(4) ?: "DOWN",
                    actionState = fields.getOrNull(5) ?: "IDLE",
                )
            }
            ?: emptyList()
        val enemies = parts.getOrNull(3)
            ?.split("~")
            ?.mapNotNull { item ->
                if (item.isBlank()) return@mapNotNull null
                val fields = item.split(",")
                val id = fields.getOrNull(0) ?: return@mapNotNull null
                ReplicatedEnemyState(
                    enemyId = id,
                    x = fields.getOrNull(1)?.toFloatOrNull() ?: 0f,
                    y = fields.getOrNull(2)?.toFloatOrNull() ?: 0f,
                    hp = fields.getOrNull(3)?.toIntOrNull() ?: 0,
                    isDead = fields.getOrNull(4)?.toBooleanStrictOrNull() ?: false,
                )
            }
            ?: emptyList()
        return WorldSnapshot(
            tick = tick,
            floorNumber = floor,
            players = players,
            enemies = enemies,
        )
    }

    private fun serializeGameEvent(event: GameEventMessage): String {
        return listOf(
            event.eventId,
            event.eventType,
            event.actorId ?: "",
            event.targetId ?: "",
            event.value?.toString() ?: "",
        ).joinToString("|")
    }

    private fun deserializeGameEvent(raw: String): GameEventMessage? {
        val parts = raw.split("|")
        val eventId = parts.getOrNull(0)?.ifBlank { return null } ?: return null
        val eventType = parts.getOrNull(1)?.ifBlank { return null } ?: return null
        return GameEventMessage(
            eventId = eventId,
            eventType = eventType,
            actorId = parts.getOrNull(2)?.ifBlank { null },
            targetId = parts.getOrNull(3)?.ifBlank { null },
            value = parts.getOrNull(4)?.toIntOrNull(),
        )
    }

    private data class HelloState(
        val playerId: String,
        val ready: Boolean,
    )

    private fun serializeHello(playerId: String, ready: Boolean): String {
        return "$playerId|$ready"
    }

    private fun deserializeHello(raw: String): HelloState? {
        val parts = raw.split("|")
        val playerId = parts.getOrNull(0)?.ifBlank { return null } ?: return null
        val ready = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: false
        return HelloState(playerId, ready)
    }
}
