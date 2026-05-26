package com.sao.aincrad.net

/**
 * Protocol version for online co-op networking payloads.
 * Increment on any incompatible payload change.
 */
const val NET_PROTOCOL_VERSION: Int = 1

enum class NetMessageType {
    HELLO,
    INPUT_COMMAND,
    STATE_SNAPSHOT,
    GAME_EVENT,
    ACK,
    PING,
    PONG,
}

data class NetEnvelope(
    val protocolVersion: Int = NET_PROTOCOL_VERSION,
    val sessionId: String,
    val senderId: String,
    val sequence: Long,
    val tick: Long,
    val type: NetMessageType,
    val payload: String,
)

