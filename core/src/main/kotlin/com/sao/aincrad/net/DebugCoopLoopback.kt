package com.sao.aincrad.net

import com.badlogic.gdx.math.MathUtils
import com.sao.aincrad.controllers.WorldState

data class RemoteGhostState(
    val x: Float,
    val y: Float,
    val hp: Int,
    val facing: String,
)

/**
 * Local host/client loopback harness for early co-op iteration.
 * Runs both peers in one process and exercises protocol paths.
 */
class DebugCoopLoopback(
    private val roomCode: String = "dev-room-1",
) {
    private val host = CoopSessionManager(
        transport = LocalLoopbackTransport(),
        localPlayerId = "host_p1",
        sessionId = "session_dev_001",
    )
    private val client = CoopSessionManager(
        transport = LocalLoopbackTransport(),
        localPlayerId = "client_p2",
        sessionId = "session_dev_001",
    )

    private var simTime = 0f
    private var remoteX = 120f
    private var remoteY = 120f
    private var remoteFacing = "DOWN"
    private var remoteHp = 100
    private var remoteFromSnapshot: RemoteGhostState? = null

    init {
        host.host(roomCode)
        client.join(roomCode)
        host.setReady(true)
        client.setReady(true)
    }

    fun update(delta: Float, state: WorldState) {
        simTime += delta

        val moveX = MathUtils.sin(simTime * 0.9f)
        val moveY = MathUtils.cos(simTime * 0.7f)
        client.enqueueLocalInput(
            PlayerInputCommand(
                playerId = "client_p2",
                moveX = moveX,
                moveY = moveY,
                attackPressed = false,
                dodgePressed = false,
            ),
        )

        client.update(delta)
        host.update(delta)

        for (input in host.drainIncomingInputs()) {
            val speed = 80f * delta
            remoteX += input.moveX * speed
            remoteY += input.moveY * speed
            if (kotlin.math.abs(input.moveX) > kotlin.math.abs(input.moveY)) {
                remoteFacing = if (input.moveX >= 0f) "RIGHT" else "LEFT"
            } else if (kotlin.math.abs(input.moveY) > 0.01f) {
                remoteFacing = if (input.moveY >= 0f) "UP" else "DOWN"
            }
        }

        remoteX = remoteX.coerceIn(state.floor.worldBounds.x, state.floor.worldBounds.x + state.floor.worldBounds.width - 32f)
        remoteY = remoteY.coerceIn(state.floor.worldBounds.y, state.floor.worldBounds.y + state.floor.worldBounds.height - 32f)

        val snapshot = WorldSnapshot(
            tick = host.sessionState().localTick,
            floorNumber = state.floor.floorNumber,
            players = listOf(
                ReplicatedPlayerState(
                    playerId = "host_p1",
                    x = state.player.position.x,
                    y = state.player.position.y,
                    hp = state.player.stats.hp,
                    facing = state.player.facing.name,
                ),
                ReplicatedPlayerState(
                    playerId = "client_p2",
                    x = remoteX,
                    y = remoteY,
                    hp = remoteHp,
                    facing = remoteFacing,
                ),
            ),
            enemies = state.enemies.mapIndexed { index, enemy ->
                ReplicatedEnemyState(
                    enemyId = "e_$index",
                    x = enemy.position.x,
                    y = enemy.position.y,
                    hp = enemy.hp,
                    isDead = enemy.isDead,
                )
            },
        )
        host.publishSnapshot(snapshot)
        host.update(0f)
        client.update(0f)

        val clientSnapshot = client.consumeLatestSnapshot()
        remoteFromSnapshot = clientSnapshot
            ?.players
            ?.firstOrNull { it.playerId == "client_p2" }
            ?.let { RemoteGhostState(it.x, it.y, it.hp, it.facing) }
    }

    fun remoteGhost(): RemoteGhostState? = remoteFromSnapshot

    fun debugLines(): List<String> {
        val hostState = host.sessionState()
        val clientState = client.sessionState()
        return listOf(
            "Co-op loopback ON",
            "Lobby ready: ${if (host.allReady()) "YES" else "NO"}",
            "Host tick ${hostState.localTick} sent ${hostState.sentMessages} recv ${hostState.receivedMessages}",
            "Client tick ${clientState.localTick} sent ${clientState.sentMessages} recv ${clientState.receivedMessages}",
            "Remote ghost: ${remoteFromSnapshot?.x?.toInt() ?: 0}, ${remoteFromSnapshot?.y?.toInt() ?: 0}",
        )
    }
}
