package com.sao.aincrad.net

class LanEndpointConfig(
    hosts: List<String> = listOf("10.0.2.2", "127.0.0.1", "192.168.0.100"),
    initialPort: Int = 42042,
) {
    private val hostPool = hosts.map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("10.0.2.2") }
    private var hostIndex = 0
    private var port = initialPort.coerceIn(1024, 65535)

    fun currentHost(): String = hostPool[hostIndex]

    fun currentPort(): Int = port

    fun cycleHostForward(): String {
        hostIndex = (hostIndex + 1) % hostPool.size
        return currentHost()
    }

    fun cycleHostBackward(): String {
        hostIndex = if (hostIndex == 0) hostPool.lastIndex else hostIndex - 1
        return currentHost()
    }

    fun incrementPort(step: Int = 1): Int {
        port = (port + step).coerceAtMost(65535)
        return port
    }

    fun decrementPort(step: Int = 1): Int {
        port = (port - step).coerceAtLeast(1024)
        return port
    }

    fun hostRoomCode(): String = "lan:$port"

    fun joinRoomCode(): String = "lan:${currentHost()}:$port"
}
