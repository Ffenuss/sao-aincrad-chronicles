package com.sao.aincrad.net

class FixedTickClock(
    ticksPerSecond: Int = 20,
) {
    private val step = 1f / ticksPerSecond.coerceAtLeast(1).toFloat()
    private var accumulator = 0f
    var tick: Long = 0
        private set

    fun consume(deltaSeconds: Float, onTick: (tick: Long, dt: Float) -> Unit) {
        accumulator += deltaSeconds.coerceAtLeast(0f)
        while (accumulator >= step) {
            tick += 1
            onTick(tick, step)
            accumulator -= step
        }
    }
}

