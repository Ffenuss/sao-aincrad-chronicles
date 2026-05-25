package com.sao.aincrad.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class ControlsRenderer(
    private val font: BitmapFont,
    private val layout: GlyphLayout,
) {
    fun drawPanels(shapeRenderer: ShapeRenderer, controls: VirtualControls) {
        val center = controls.center()
        shapeRenderer.color = Color(1f, 1f, 1f, 0.12f)
        shapeRenderer.circle(center.x, center.y, controls.radius(), 32)
        shapeRenderer.color = Color(1f, 1f, 1f, 0.26f)
        shapeRenderer.circle(controls.knob().x, controls.knob().y, controls.radius() * 0.42f, 24)

        for (button in controls.buttons()) {
            shapeRenderer.color = if (button.pressed) Color(0.98f, 0.92f, 0.45f, 0.8f) else Color(0.18f, 0.18f, 0.22f, 0.8f)
            shapeRenderer.rect(button.rect.x, button.rect.y, button.rect.width, button.rect.height)
            shapeRenderer.color = Color(1f, 1f, 1f, 0.2f)
            shapeRenderer.rect(button.rect.x + 3f, button.rect.y + 3f, button.rect.width - 6f, button.rect.height - 6f)
        }
    }

    fun drawLabels(batch: SpriteBatch, controls: VirtualControls) {
        font.color = Color.WHITE
        for (button in controls.buttons()) {
            layout.setText(font, button.label)
            val textX = button.rect.x + (button.rect.width - layout.width) * 0.5f
            val textY = button.rect.y + (button.rect.height + layout.height) * 0.5f - 4f
            font.draw(batch, button.label, textX, textY)
        }
    }
}
