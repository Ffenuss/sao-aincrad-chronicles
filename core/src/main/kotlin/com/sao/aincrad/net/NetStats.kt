package com.sao.aincrad.net

data class NetStats(
    val rttMs: Int = 0,
    val packetLossPercent: Float = 0f,
    val localTick: Long = 0,
    val remoteTick: Long = 0,
    val desyncFrames: Int = 0,
)

