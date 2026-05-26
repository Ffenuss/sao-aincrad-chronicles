package com.sao.aincrad.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveNetTransportTest {
    private class FakeTransport : NetTransport {
        override var isConnected: Boolean = false
        var lastEndpoint: String = ""
        var connectCalls: Int = 0

        override fun connect(endpoint: String, playerId: String): Result<Unit> {
            connectCalls += 1
            lastEndpoint = endpoint
            isConnected = true
            return Result.success(Unit)
        }

        override fun disconnect() {
            isConnected = false
        }

        override fun send(message: NetEnvelope): Result<Unit> = Result.success(Unit)

        override fun pollIncoming(maxMessages: Int): List<NetEnvelope> = emptyList()
    }

    @Test
    fun `adaptive transport routes loopback endpoints`() {
        val loopback = FakeTransport()
        val lan = FakeTransport()
        val adaptive = AdaptiveNetTransport(loopback = loopback, lan = lan)

        assertTrue(adaptive.connect("host|ROOM_X", "p1").isSuccess)
        assertEquals(1, loopback.connectCalls)
        assertEquals(0, lan.connectCalls)
        assertEquals("host|ROOM_X", loopback.lastEndpoint)
    }

    @Test
    fun `adaptive transport routes lan endpoints`() {
        val loopback = FakeTransport()
        val lan = FakeTransport()
        val adaptive = AdaptiveNetTransport(loopback = loopback, lan = lan)

        assertTrue(adaptive.connect("join|lan:127.0.0.1:42042", "p2").isSuccess)
        assertEquals(0, loopback.connectCalls)
        assertEquals(1, lan.connectCalls)
        assertEquals("join|127.0.0.1:42042", lan.lastEndpoint)
    }
}
