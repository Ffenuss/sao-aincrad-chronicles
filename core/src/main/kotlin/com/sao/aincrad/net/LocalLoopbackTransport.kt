package com.sao.aincrad.net

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-memory transport for local co-op/network simulation.
 * Useful for fast iteration before real socket/WebRTC transport is added.
 */
class LocalLoopbackTransport : NetTransport {
    override var isConnected: Boolean = false
        private set

    private var endpoint: String = ""
    private var playerId: String = ""
    private val inbox = ConcurrentLinkedQueue<NetEnvelope>()

    override fun connect(endpoint: String, playerId: String): Result<Unit> {
        if (isConnected) return Result.success(Unit)
        this.endpoint = normalizeEndpoint(endpoint)
        this.playerId = playerId
        LocalLoopbackBus.register(this.endpoint, playerId, inbox)
        isConnected = true
        return Result.success(Unit)
    }

    override fun disconnect() {
        if (!isConnected) return
        LocalLoopbackBus.unregister(endpoint, playerId)
        inbox.clear()
        isConnected = false
        endpoint = ""
        playerId = ""
    }

    override fun send(message: NetEnvelope): Result<Unit> {
        if (!isConnected) return Result.failure(IllegalStateException("Transport is not connected"))
        LocalLoopbackBus.broadcast(endpoint, playerId, message)
        return Result.success(Unit)
    }

    override fun pollIncoming(maxMessages: Int): List<NetEnvelope> {
        if (maxMessages <= 0) return emptyList()
        val out = ArrayList<NetEnvelope>(maxMessages)
        repeat(maxMessages) {
            val next = inbox.poll() ?: return@repeat
            out += next
        }
        return out
    }

    private fun normalizeEndpoint(endpoint: String): String {
        val rawRoom = endpoint.substringAfter('|', endpoint)
        return rawRoom.substringAfter("lan:", rawRoom)
    }
}

private object LocalLoopbackBus {
    private val rooms = ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<NetEnvelope>>>()

    fun register(endpoint: String, playerId: String, queue: ConcurrentLinkedQueue<NetEnvelope>) {
        val room = rooms.computeIfAbsent(endpoint) { ConcurrentHashMap() }
        room[playerId] = queue
    }

    fun unregister(endpoint: String, playerId: String) {
        val room = rooms[endpoint] ?: return
        room.remove(playerId)
        if (room.isEmpty()) {
            rooms.remove(endpoint)
        }
    }

    fun broadcast(endpoint: String, senderId: String, message: NetEnvelope) {
        val room = rooms[endpoint] ?: return
        room.forEach { (targetId, queue) ->
            if (targetId != senderId) {
                queue.offer(message)
            }
        }
    }
}
