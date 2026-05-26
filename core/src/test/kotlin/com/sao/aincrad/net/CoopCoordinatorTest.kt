package com.sao.aincrad.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoopCoordinatorTest {
    @Test
    fun `host and client can ready up and start match`() {
        val host = CoopCoordinator(localPlayerId = "host_1", sessionId = "session_test", ticksPerSecond = 20)
        val client = CoopCoordinator(localPlayerId = "client_1", sessionId = "session_test", ticksPerSecond = 20)

        try {
            assertTrue(host.host("ROOM_A").isSuccess)
            assertTrue(client.join("ROOM_A").isSuccess)
            pump(host, client, ticks = 4)

            host.setReady(true)
            client.setReady(true)
            pump(host, client, ticks = 6)

            val hostLobby = host.state()
            val clientLobby = client.state()
            assertEquals(CoopCoordinator.Phase.LOBBY, hostLobby.phase)
            assertEquals(CoopCoordinator.Phase.LOBBY, clientLobby.phase)
            assertTrue(hostLobby.allReady)
            assertTrue(clientLobby.allReady)

            client.requestMatchStart()
            pump(host, client, ticks = 8)

            assertEquals(CoopCoordinator.Phase.MATCH, host.state().phase)
            assertEquals(CoopCoordinator.Phase.MATCH, client.state().phase)
        } finally {
            host.disconnect()
            client.disconnect()
        }
    }

    @Test
    fun `match does not start while not all players ready`() {
        val host = CoopCoordinator(localPlayerId = "host_2", sessionId = "session_test_2", ticksPerSecond = 20)
        val client = CoopCoordinator(localPlayerId = "client_2", sessionId = "session_test_2", ticksPerSecond = 20)

        try {
            assertTrue(host.host("ROOM_B").isSuccess)
            assertTrue(client.join("ROOM_B").isSuccess)
            pump(host, client, ticks = 4)

            host.setReady(true)
            pump(host, client, ticks = 6)
            client.requestMatchStart()
            pump(host, client, ticks = 8)

            assertEquals(CoopCoordinator.Phase.LOBBY, host.state().phase)
            assertEquals(CoopCoordinator.Phase.LOBBY, client.state().phase)
            assertFalse(host.state().allReady)
        } finally {
            host.disconnect()
            client.disconnect()
        }
    }

    @Test
    fun `disconnect resets coordinator state`() {
        val host = CoopCoordinator(localPlayerId = "host_3", sessionId = "session_test_3", ticksPerSecond = 20)
        try {
            assertTrue(host.host("ROOM_C").isSuccess)
            host.setReady(true)
            host.disconnect()

            val state = host.state()
            assertEquals(CoopCoordinator.Phase.OFFLINE, state.phase)
            assertFalse(state.isConnected)
            assertEquals("", state.roomCode)
            assertFalse(state.localReady)
        } finally {
            host.disconnect()
        }
    }

    @Test
    fun `gameplay events are delivered during match`() {
        val host = CoopCoordinator(localPlayerId = "host_4", sessionId = "session_test_4", ticksPerSecond = 20)
        val client = CoopCoordinator(localPlayerId = "client_4", sessionId = "session_test_4", ticksPerSecond = 20)
        try {
            assertTrue(host.host("ROOM_D").isSuccess)
            assertTrue(client.join("ROOM_D").isSuccess)
            pump(host, client, ticks = 4)

            host.setReady(true)
            client.setReady(true)
            host.requestMatchStart()
            pump(host, client, ticks = 10)
            assertEquals(CoopCoordinator.Phase.MATCH, host.state().phase)
            assertEquals(CoopCoordinator.Phase.MATCH, client.state().phase)

            host.sendGameplayEvent(eventType = "combat_hit", targetId = "spawn_1", value = 17)
            pump(host, client, ticks = 6)

            val events = client.drainGameplayEvents()
            assertTrue(events.isNotEmpty())
            val hit = events.firstOrNull { it.eventType == "combat_hit" }
            assertNotNull(hit)
            assertEquals("spawn_1", hit.targetId)
            assertEquals(17, hit.value)
        } finally {
            host.disconnect()
            client.disconnect()
        }
    }

    @Test
    fun `rtt is measured after peers connect`() {
        val host = CoopCoordinator(localPlayerId = "host_5", sessionId = "session_test_5", ticksPerSecond = 20)
        val client = CoopCoordinator(localPlayerId = "client_5", sessionId = "session_test_5", ticksPerSecond = 20)
        try {
            assertTrue(host.host("ROOM_E").isSuccess)
            assertTrue(client.join("ROOM_E").isSuccess)
            pump(host, client, ticks = 40)

            val hostState = host.state()
            val clientState = client.state()
            val hostRtt = assertNotNull(hostState.averageRttMs)
            val clientRtt = assertNotNull(clientState.averageRttMs)
            assertTrue(hostRtt >= 0f)
            assertTrue(clientRtt >= 0f)
            assertTrue(hostState.packetLossPercent >= 0f)
            assertTrue(clientState.packetLossPercent >= 0f)
        } finally {
            host.disconnect()
            client.disconnect()
        }
    }

    @Test
    fun `reconnect works within 30 second window`() {
        var now = 1_000L
        val host = CoopCoordinator(
            localPlayerId = "host_6",
            sessionId = "session_test_6",
            ticksPerSecond = 20,
            nowMillis = { now },
        )
        try {
            assertTrue(host.host("ROOM_F").isSuccess)
            assertTrue(host.state().isConnected)

            host.disconnect()
            val offline = host.state()
            assertEquals(CoopCoordinator.Phase.OFFLINE, offline.phase)
            assertTrue(offline.reconnectAvailable)

            now += 10_000L
            assertTrue(host.reconnect().isSuccess)
            val reconnected = host.state()
            assertTrue(reconnected.isConnected)
            assertEquals(CoopCoordinator.Phase.LOBBY, reconnected.phase)
        } finally {
            host.disconnect()
        }
    }

    @Test
    fun `reconnect expires after 30 seconds`() {
        var now = 5_000L
        val host = CoopCoordinator(
            localPlayerId = "host_7",
            sessionId = "session_test_7",
            ticksPerSecond = 20,
            nowMillis = { now },
        )
        try {
            assertTrue(host.host("ROOM_G").isSuccess)
            host.disconnect()
            assertTrue(host.state().reconnectAvailable)

            now += 31_000L
            assertFalse(host.state().reconnectAvailable)
            assertTrue(host.reconnect().isFailure)
            assertFalse(host.state().isConnected)
        } finally {
            host.disconnect()
        }
    }

    @Test
    fun `host times out in match when remote peer disappears`() {
        val host = CoopCoordinator(localPlayerId = "host_8", sessionId = "session_test_8", ticksPerSecond = 20)
        val client = CoopCoordinator(localPlayerId = "client_8", sessionId = "session_test_8", ticksPerSecond = 20)
        try {
            assertTrue(host.host("ROOM_H").isSuccess)
            assertTrue(client.join("ROOM_H").isSuccess)
            pump(host, client, ticks = 4)

            host.setReady(true)
            client.setReady(true)
            host.requestMatchStart()
            pump(host, client, ticks = 10)
            assertEquals(CoopCoordinator.Phase.MATCH, host.state().phase)

            client.disconnect()
            repeat(220) { host.update(0.05f) }

            val hostState = host.state()
            assertEquals(CoopCoordinator.Phase.OFFLINE, hostState.phase)
            assertTrue(hostState.timedOut)
            assertTrue(hostState.reconnectAvailable)
            assertTrue(host.reconnect().isSuccess)
            val reconnected = host.state()
            assertTrue(reconnected.isConnected)
            assertEquals(CoopCoordinator.Phase.MATCH, reconnected.phase)
            assertFalse(reconnected.timedOut)
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
