package com.sao.aincrad.net

import kotlin.test.Test
import kotlin.test.assertEquals

class LanEndpointConfigTest {
    @Test
    fun `builds host and join room codes`() {
        val config = LanEndpointConfig(hosts = listOf("10.0.2.2"), initialPort = 42042)
        assertEquals("lan:42042", config.hostRoomCode())
        assertEquals("lan:10.0.2.2:42042", config.joinRoomCode())
    }

    @Test
    fun `cycles hosts and clamps port`() {
        val config = LanEndpointConfig(hosts = listOf("a", "b"), initialPort = 1024)
        assertEquals("a", config.currentHost())
        assertEquals("b", config.cycleHostForward())
        assertEquals("a", config.cycleHostForward())
        assertEquals("b", config.cycleHostBackward())

        config.decrementPort(9999)
        assertEquals(1024, config.currentPort())
        config.incrementPort(70000)
        assertEquals(65535, config.currentPort())
    }
}
