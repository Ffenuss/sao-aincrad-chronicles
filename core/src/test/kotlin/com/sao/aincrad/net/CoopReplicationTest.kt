package com.sao.aincrad.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoopReplicationTest {
    @Test
    fun `snapshot from host reaches client after match starts`() {
        val host = CoopCoordinator(localPlayerId = "host_r1", sessionId = "session_replication", ticksPerSecond = 20)
        val client = CoopCoordinator(localPlayerId = "client_r1", sessionId = "session_replication", ticksPerSecond = 20)

        try {
            assertTrue(host.host("ROOM_REPL").isSuccess)
            assertTrue(client.join("ROOM_REPL").isSuccess)
            pump(host, client, ticks = 4)

            host.setReady(true)
            client.setReady(true)
            pump(host, client, ticks = 6)
            host.requestMatchStart()
            pump(host, client, ticks = 8)

            assertEquals(CoopCoordinator.Phase.MATCH, host.state().phase)
            assertEquals(CoopCoordinator.Phase.MATCH, client.state().phase)

            host.publishSnapshot(
                WorldSnapshot(
                    tick = host.state().localTick,
                    floorNumber = 1,
                    players = listOf(
                        ReplicatedPlayerState(
                            playerId = "host_r1",
                            x = 640f,
                            y = 420f,
                            hp = 100,
                            facing = "UP",
                            actionState = "ATTACKING",
                        ),
                    ),
                    enemies = listOf(
                        ReplicatedEnemyState(
                            enemyId = "spawn_1",
                            x = 700f,
                            y = 380f,
                            hp = 12,
                            isDead = false,
                        ),
                    ),
                ),
            )
            pump(host, client, ticks = 6)

            val snapshot = client.consumeLatestSnapshot()
            assertNotNull(snapshot)
            assertEquals(1, snapshot.players.size)
            assertEquals("host_r1", snapshot.players.first().playerId)
            assertEquals(640f, snapshot.players.first().x)
            assertEquals(420f, snapshot.players.first().y)
            assertEquals("ATTACKING", snapshot.players.first().actionState)
            assertEquals(1, snapshot.enemies.size)
            assertEquals("spawn_1", snapshot.enemies.first().enemyId)
            assertEquals(12, snapshot.enemies.first().hp)
        } finally {
            host.disconnect()
            client.disconnect()
        }
    }

    private fun pump(host: CoopCoordinator, client: CoopCoordinator, ticks: Int) {
        repeat(ticks) {
            host.update(0.05f)
            client.update(0.05f)
        }
    }
}
