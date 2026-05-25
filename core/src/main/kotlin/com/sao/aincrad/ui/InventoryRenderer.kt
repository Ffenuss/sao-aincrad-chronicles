package com.sao.aincrad.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.sao.aincrad.items.EquipmentSlot
import com.sao.aincrad.items.Inventory
import com.sao.aincrad.render.PixelSpriteAtlas

class InventoryRenderer(private val font: BitmapFont) {
    fun drawPanel(shapeRenderer: ShapeRenderer, inventory: Inventory, width: Float, height: Float) {
        val bounds = panelBounds(inventory, width, height)
        val x = bounds.x
        val y = bounds.y
        val panelWidth = bounds.width
        val panelHeight = bounds.height
        shapeRenderer.color = Color(0.03f, 0.04f, 0.05f, 0.88f)
        shapeRenderer.rect(x, y, panelWidth, panelHeight)
        shapeRenderer.color = Color(0.75f, 0.82f, 0.92f, 0.35f)
        shapeRenderer.rect(x + 2f, y + 2f, panelWidth - 4f, panelHeight - 4f)
        shapeRenderer.color = Color(0.03f, 0.04f, 0.05f, 0.92f)
        shapeRenderer.rect(x + 4f, y + 4f, panelWidth - 8f, panelHeight - 8f)

        for (row in 0 until inventory.rows) {
            for (column in 0 until inventory.columns) {
                val slotX = x + 16f + column * (SLOT + GAP)
                val slotY = y + 16f + (inventory.rows - 1 - row) * (SLOT + GAP)
                shapeRenderer.color = Color(0.14f, 0.15f, 0.18f, 1f)
                shapeRenderer.rect(slotX, slotY, SLOT, SLOT)
                shapeRenderer.color = Color(0.35f, 0.38f, 0.44f, 1f)
                shapeRenderer.rect(slotX + 2f, slotY + 2f, SLOT - 4f, SLOT - 4f)
            }
        }
    }

    fun drawText(batch: SpriteBatch, sprites: PixelSpriteAtlas, inventory: Inventory, gold: Int, width: Float, height: Float) {
        val bounds = panelBounds(inventory, width, height)
        val x = bounds.x
        val y = bounds.y
        val panelWidth = bounds.width
        val panelHeight = bounds.height
        font.color = Color.WHITE
        font.draw(batch, "Inventory", x + 16f, y + panelHeight - 18f)
        font.draw(batch, "Col $gold", x + panelWidth - 82f, y + panelHeight - 18f)
        font.draw(batch, "E equip", x + 16f, y + panelHeight - 38f)
        font.draw(batch, "Q use", x + 92f, y + panelHeight - 38f)

        val slots = inventory.allSlots()
        for (index in slots.indices) {
            val stack = slots[index] ?: continue
            val column = index % inventory.columns
            val row = index / inventory.columns
            val slotX = x + 16f + column * (SLOT + GAP)
            val slotY = y + 16f + (inventory.rows - 1 - row) * (SLOT + GAP)
            batch.draw(sprites.lootFrame(stack.item.type), slotX + 18f, slotY + 28f, 14f, 14f)
            font.draw(batch, stack.item.name.take(6), slotX + 5f, slotY + 24f)
            font.draw(batch, "x${stack.quantity}", slotX + 5f, slotY + 14f)
        }

        var equipY = y + 44f
        for (slot in EquipmentSlot.values()) {
            val item = inventory.equipped(slot)?.name ?: "-"
            font.draw(batch, "${slot.name}: $item", x + panelWidth + 12f, equipY)
            equipY += 18f
        }
    }

    private fun panelBounds(inventory: Inventory, width: Float, height: Float): PanelBounds {
        val panelWidth = inventory.columns * SLOT + (inventory.columns - 1) * GAP + 32f
        val panelHeight = inventory.rows * SLOT + (inventory.rows - 1) * GAP + 64f
        return PanelBounds((width - panelWidth) * 0.5f, (height - panelHeight) * 0.5f, panelWidth, panelHeight)
    }

    private data class PanelBounds(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )

    private companion object {
        const val SLOT = 48f
        const val GAP = 6f
    }
}
