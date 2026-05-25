package com.sao.aincrad.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class DialogRenderer(private val font: BitmapFont) {
    fun draw(batch: SpriteBatch, width: Float, speaker: String, text: String) {
        val x = 40f
        val y = 82f
        font.color = Color(1f, 0.94f, 0.72f, 1f)
        font.draw(batch, "$speaker:", x + 12f, y + 42f)
        font.color = Color.WHITE
        font.draw(batch, text.take(110), x + 12f, y + 22f, width - 104f, 1, true)
    }
}
