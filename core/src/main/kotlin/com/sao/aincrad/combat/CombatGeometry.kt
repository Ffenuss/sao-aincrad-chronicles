package com.sao.aincrad.combat

import com.badlogic.gdx.math.Rectangle

object CombatGeometry {
    fun attackHitbox(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        facing: String,
        range: Float = 18f,
    ): Rectangle {
        val halfW = width * 0.5f
        val halfH = height * 0.5f
        val centerX = x + halfW
        val centerY = y + halfH
        return when (facing.uppercase()) {
            "UP" -> Rectangle(x, y + height, width, range)
            "DOWN" -> Rectangle(x, y - range, width, range)
            "LEFT" -> Rectangle(x - range, y, range, height)
            "RIGHT" -> Rectangle(x + width, y, range, height)
            "UP_RIGHT" -> Rectangle(centerX + 2f, centerY + 2f, range, range)
            "DOWN_RIGHT" -> Rectangle(centerX + 2f, centerY - range - 2f, range, range)
            "DOWN_LEFT" -> Rectangle(centerX - range - 2f, centerY - range - 2f, range, range)
            "UP_LEFT" -> Rectangle(centerX - range - 2f, centerY + 2f, range, range)
            else -> Rectangle(x, y - range, width, range)
        }
    }
}
