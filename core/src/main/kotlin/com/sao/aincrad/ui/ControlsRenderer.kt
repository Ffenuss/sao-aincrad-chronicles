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
        shapeRenderer.color = Color(0.05f, 0.07f, 0.11f, 0.35f)
        shapeRenderer.circle(center.x, center.y, controls.radius() + 12f, 36)
        shapeRenderer.color = Color(0.16f, 0.24f, 0.35f, 0.50f)
        shapeRenderer.circle(center.x, center.y, controls.radius(), 36)
        shapeRenderer.color = Color(0.34f, 0.52f, 0.74f, 0.22f)
        shapeRenderer.circle(center.x, center.y, controls.radius() - 8f, 30)
        shapeRenderer.color = Color(0.82f, 0.94f, 1f, 0.30f)
        shapeRenderer.circle(controls.knob().x, controls.knob().y, controls.radius() * 0.44f, 28)
        shapeRenderer.color = Color(0.26f, 0.40f, 0.56f, 0.55f)
        shapeRenderer.circle(controls.knob().x, controls.knob().y, controls.radius() * 0.33f, 22)

        for (button in controls.buttons()) {
            val palette = paletteFor(button.id, button.pressed)
            val cx = button.rect.x + button.rect.width * 0.5f
            val cy = button.rect.y + button.rect.height * 0.5f
            val radius = button.rect.width * 0.52f
            shapeRenderer.color = palette.shadow
            shapeRenderer.circle(cx, cy - 2f, radius + 4f, 32)
            shapeRenderer.color = palette.base
            shapeRenderer.circle(cx, cy, radius, 32)
            shapeRenderer.color = palette.inner
            shapeRenderer.circle(cx, cy, radius * 0.78f, 28)
            shapeRenderer.color = palette.glow
            shapeRenderer.circle(cx, cy + radius * 0.24f, radius * 0.28f, 20)
            drawIcon(shapeRenderer, button.id, cx, cy, radius * 0.75f, palette.icon)
        }
    }

    fun drawLabels(batch: SpriteBatch, controls: VirtualControls) {
        val bag = controls.buttons().firstOrNull { it.id == "inventory" } ?: return
        val text = "BAG"
        font.color = Color(0.93f, 0.94f, 0.90f, 0.95f)
        layout.setText(font, text)
        font.draw(
            batch,
            text,
            bag.rect.x + (bag.rect.width - layout.width) * 0.5f,
            bag.rect.y + bag.rect.height + 14f,
        )
    }

    private data class ButtonPalette(
        val shadow: Color,
        val base: Color,
        val inner: Color,
        val glow: Color,
        val icon: Color,
    )

    private fun paletteFor(id: String, pressed: Boolean): ButtonPalette {
        return when (id) {
            "attack" -> if (pressed) {
                ButtonPalette(Color(0.44f, 0.08f, 0.10f, 0.62f), Color(0.86f, 0.22f, 0.26f, 0.92f), Color(0.95f, 0.44f, 0.40f, 0.92f), Color(1f, 0.86f, 0.76f, 0.70f), Color(1f, 0.96f, 0.92f, 0.95f))
            } else {
                ButtonPalette(Color(0.20f, 0.08f, 0.10f, 0.52f), Color(0.48f, 0.16f, 0.20f, 0.86f), Color(0.62f, 0.24f, 0.30f, 0.86f), Color(0.92f, 0.58f, 0.54f, 0.52f), Color(0.98f, 0.91f, 0.87f, 0.92f))
            }
            "dodge" -> if (pressed) {
                ButtonPalette(Color(0.08f, 0.20f, 0.28f, 0.58f), Color(0.26f, 0.58f, 0.82f, 0.90f), Color(0.46f, 0.74f, 0.95f, 0.90f), Color(0.86f, 0.96f, 1f, 0.68f), Color(0.90f, 0.98f, 1f, 0.95f))
            } else {
                ButtonPalette(Color(0.07f, 0.13f, 0.20f, 0.48f), Color(0.18f, 0.34f, 0.52f, 0.84f), Color(0.30f, 0.48f, 0.66f, 0.84f), Color(0.64f, 0.82f, 0.98f, 0.50f), Color(0.86f, 0.96f, 1f, 0.90f))
            }
            "inventory" -> if (pressed) {
                ButtonPalette(Color(0.28f, 0.20f, 0.06f, 0.56f), Color(0.74f, 0.58f, 0.20f, 0.90f), Color(0.88f, 0.72f, 0.34f, 0.90f), Color(1f, 0.96f, 0.72f, 0.68f), Color(1f, 0.98f, 0.90f, 0.95f))
            } else {
                ButtonPalette(Color(0.16f, 0.12f, 0.06f, 0.46f), Color(0.46f, 0.36f, 0.16f, 0.84f), Color(0.58f, 0.46f, 0.24f, 0.84f), Color(0.90f, 0.80f, 0.48f, 0.52f), Color(0.98f, 0.92f, 0.78f, 0.92f))
            }
            else -> if (pressed) {
                ButtonPalette(Color(0.10f, 0.10f, 0.18f, 0.58f), Color(0.33f, 0.36f, 0.64f, 0.90f), Color(0.48f, 0.56f, 0.82f, 0.90f), Color(0.82f, 0.88f, 1f, 0.68f), Color(0.95f, 0.97f, 1f, 0.95f))
            } else {
                ButtonPalette(Color(0.08f, 0.09f, 0.14f, 0.48f), Color(0.20f, 0.24f, 0.40f, 0.84f), Color(0.30f, 0.35f, 0.56f, 0.84f), Color(0.66f, 0.75f, 0.98f, 0.50f), Color(0.88f, 0.93f, 1f, 0.92f))
            }
        }
    }

    private fun drawIcon(shapeRenderer: ShapeRenderer, id: String, cx: Float, cy: Float, size: Float, color: Color) {
        shapeRenderer.color = color
        when (id) {
            "attack" -> {
                shapeRenderer.rect(cx - size * 0.08f, cy - size * 0.42f, size * 0.16f, size * 0.76f)
                shapeRenderer.rect(cx - size * 0.32f, cy + size * 0.16f, size * 0.64f, size * 0.12f)
                shapeRenderer.rect(cx + size * 0.08f, cy + size * 0.30f, size * 0.18f, size * 0.08f)
            }
            "skill1" -> {
                shapeRenderer.rect(cx - size * 0.36f, cy - size * 0.04f, size * 0.72f, size * 0.10f)
                shapeRenderer.rect(cx - size * 0.18f, cy - size * 0.28f, size * 0.36f, size * 0.56f)
            }
            "skill2" -> {
                shapeRenderer.rect(cx - size * 0.30f, cy - size * 0.30f, size * 0.60f, size * 0.14f)
                shapeRenderer.rect(cx - size * 0.30f, cy - size * 0.07f, size * 0.60f, size * 0.14f)
                shapeRenderer.rect(cx - size * 0.30f, cy + size * 0.16f, size * 0.60f, size * 0.14f)
            }
            "skill3" -> {
                shapeRenderer.rect(cx - size * 0.34f, cy - size * 0.34f, size * 0.16f, size * 0.68f)
                shapeRenderer.rect(cx - size * 0.02f, cy - size * 0.34f, size * 0.16f, size * 0.68f)
                shapeRenderer.rect(cx + size * 0.18f, cy - size * 0.34f, size * 0.16f, size * 0.68f)
            }
            "dodge" -> {
                shapeRenderer.rect(cx - size * 0.34f, cy - size * 0.08f, size * 0.68f, size * 0.16f)
                shapeRenderer.rect(cx + size * 0.16f, cy - size * 0.20f, size * 0.20f, size * 0.40f)
            }
            "inventory" -> {
                shapeRenderer.rect(cx - size * 0.32f, cy - size * 0.20f, size * 0.64f, size * 0.40f)
                shapeRenderer.rect(cx - size * 0.16f, cy + size * 0.20f, size * 0.32f, size * 0.10f)
            }
        }
    }
}
