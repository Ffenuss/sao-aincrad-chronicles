package com.sao.aincrad.net

interface NetTransport {
    val isConnected: Boolean
    fun connect(endpoint: String, playerId: String): Result<Unit>
    fun disconnect()
    fun send(message: NetEnvelope): Result<Unit>
    fun pollIncoming(maxMessages: Int = 64): List<NetEnvelope>
}

