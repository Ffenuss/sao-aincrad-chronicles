package com.sao.aincrad.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class UdpLanTransport : NetTransport {
    override var isConnected: Boolean = false
        private set

    private enum class Mode { HOST, CLIENT }

    private var mode: Mode? = null
    private var socket: DatagramSocket? = null
    private var playerId: String = ""
    private val inbox = ConcurrentLinkedQueue<NetEnvelope>()
    private var clientTarget: InetSocketAddress? = null
    private val hostPeers = ConcurrentHashMap<String, InetSocketAddress>()

    override fun connect(endpoint: String, playerId: String): Result<Unit> {
        if (isConnected) return Result.success(Unit)
        val role = endpoint.substringBefore('|', "")
        val spec = endpoint.substringAfter('|', endpoint)
        return try {
            this.playerId = playerId
            when (role) {
                "host" -> {
                    val bind = parseHostSpec(spec)
                    socket = DatagramSocket(bind)
                    mode = Mode.HOST
                }
                "join" -> {
                    val target = parseJoinSpec(spec)
                    val s = DatagramSocket()
                    s.soTimeout = 1
                    socket = s
                    clientTarget = target
                    mode = Mode.CLIENT
                }
                else -> return Result.failure(IllegalArgumentException("Unknown endpoint role: $role"))
            }
            socket?.soTimeout = 1
            isConnected = true
            Result.success(Unit)
        } catch (t: Throwable) {
            disconnect()
            Result.failure(t)
        }
    }

    override fun disconnect() {
        socket?.close()
        socket = null
        mode = null
        clientTarget = null
        hostPeers.clear()
        inbox.clear()
        isConnected = false
        playerId = ""
    }

    override fun send(message: NetEnvelope): Result<Unit> {
        val s = socket ?: return Result.failure(IllegalStateException("Transport is not connected"))
        return try {
            val payload = encodeEnvelope(message)
            val bytes = payload.toByteArray(Charsets.UTF_8)
            when (mode) {
                Mode.CLIENT -> {
                    val target = clientTarget ?: return Result.failure(IllegalStateException("Client target is missing"))
                    s.send(DatagramPacket(bytes, bytes.size, target.address, target.port))
                }
                Mode.HOST -> {
                    hostPeers.values.forEach { peer ->
                        s.send(DatagramPacket(bytes, bytes.size, peer.address, peer.port))
                    }
                }
                null -> return Result.failure(IllegalStateException("Transport mode is missing"))
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override fun pollIncoming(maxMessages: Int): List<NetEnvelope> {
        if (maxMessages <= 0) return emptyList()
        val s = socket ?: return emptyList()
        repeat(maxMessages) {
            val buf = ByteArray(8192)
            val packet = DatagramPacket(buf, buf.size)
            try {
                s.receive(packet)
                val raw = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                val envelope = decodeEnvelope(raw) ?: return@repeat
                if (envelope.senderId == playerId) return@repeat
                if (mode == Mode.HOST) {
                    hostPeers[envelope.senderId] = InetSocketAddress(packet.address, packet.port)
                }
                inbox.offer(envelope)
            } catch (_: Throwable) {
                return@repeat
            }
        }
        val out = ArrayList<NetEnvelope>(maxMessages)
        repeat(maxMessages) {
            val next = inbox.poll() ?: return@repeat
            out += next
        }
        return out
    }

    private fun parseHostSpec(spec: String): InetSocketAddress {
        val raw = spec.ifBlank { "42042" }
        return if (raw.contains(':')) {
            val host = raw.substringBefore(':').ifBlank { "0.0.0.0" }
            val port = raw.substringAfter(':').toIntOrNull() ?: 42042
            InetSocketAddress(InetAddress.getByName(host), port)
        } else {
            val port = raw.toIntOrNull() ?: 42042
            InetSocketAddress("0.0.0.0", port)
        }
    }

    private fun parseJoinSpec(spec: String): InetSocketAddress {
        val raw = spec.ifBlank { "127.0.0.1:42042" }
        return if (raw.contains(':')) {
            val host = raw.substringBefore(':').ifBlank { "127.0.0.1" }
            val port = raw.substringAfter(':').toIntOrNull() ?: 42042
            InetSocketAddress(InetAddress.getByName(host), port)
        } else {
            val port = raw.toIntOrNull() ?: 42042
            InetSocketAddress("127.0.0.1", port)
        }
    }

    private fun encodeEnvelope(envelope: NetEnvelope): String {
        val payloadBase64 = Base64.getEncoder().encodeToString(envelope.payload.toByteArray(Charsets.UTF_8))
        return listOf(
            envelope.protocolVersion.toString(),
            envelope.sessionId,
            envelope.senderId,
            envelope.sequence.toString(),
            envelope.tick.toString(),
            envelope.type.name,
            payloadBase64,
        ).joinToString("\t")
    }

    private fun decodeEnvelope(raw: String): NetEnvelope? {
        val parts = raw.split("\t", limit = 7)
        if (parts.size < 7) return null
        val payload = runCatching {
            String(Base64.getDecoder().decode(parts[6]), Charsets.UTF_8)
        }.getOrNull() ?: return null
        return NetEnvelope(
            protocolVersion = parts[0].toIntOrNull() ?: NET_PROTOCOL_VERSION,
            sessionId = parts[1],
            senderId = parts[2],
            sequence = parts[3].toLongOrNull() ?: return null,
            tick = parts[4].toLongOrNull() ?: 0L,
            type = runCatching { NetMessageType.valueOf(parts[5]) }.getOrNull() ?: return null,
            payload = payload,
        )
    }
}
