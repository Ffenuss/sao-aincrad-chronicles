package com.sao.aincrad.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

data class DamagePopup(
    val text: String,
    val position: Vector2,
    val color: Color,
    val velocityY: Float = 26f,
    var life: Float = 0.9f,
)
