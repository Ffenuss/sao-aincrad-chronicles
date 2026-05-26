package com.sao.aincrad.net

/**
 * Chooses transport by endpoint scheme:
 * - `host|lan:PORT` / `join|lan:HOST:PORT` -> UDP LAN transport
 * - everything else -> in-memory loopback transport
 */
class AdaptiveNetTransport(
    private val loopback: NetTransport = LocalLoopbackTransport(),
    private val lan: NetTransport = UdpLanTransport(),
) : NetTransport {
    private var active: NetTransport? = null

    override val isConnected: Boolean
        get() = active?.isConnected == true

    override fun connect(endpoint: String, playerId: String): Result<Unit> {
        val role = endpoint.substringBefore('|', "")
        val room = endpoint.substringAfter('|', endpoint)
        val useLan = room.startsWith("lan:")
        val transport = if (useLan) lan else loopback
        val strippedRoom = room.removePrefix("lan:")
        val transportEndpoint = if (role.isBlank()) strippedRoom else "$role|$strippedRoom"
        val result = transport.connect(transportEndpoint, playerId)
        if (result.isSuccess) {
            active = transport
        }
        return result
    }

    override fun disconnect() {
        active?.disconnect()
        active = null
    }

    override fun send(message: NetEnvelope): Result<Unit> {
        val transport = active ?: return Result.failure(IllegalStateException("Transport is not connected"))
        return transport.send(message)
    }

    override fun pollIncoming(maxMessages: Int): List<NetEnvelope> {
        val transport = active ?: return emptyList()
        return transport.pollIncoming(maxMessages)
    }
}
