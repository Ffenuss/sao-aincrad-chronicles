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
        shapeRenderer.color = Color(0.02f, 0.03f, 0.06f, 0.90f)
        shapeRenderer.rect(x, y, panelWidth, panelHeight)
        shapeRenderer.color = Color(0.14f, 0.21f, 0.30f, 0.92f)
        shapeRenderer.rect(x + 2f, y + 2f, panelWidth - 4f, panelHeight - 4f)
        shapeRenderer.color = Color(0.05f, 0.08f, 0.12f, 0.95f)
        shapeRenderer.rect(x + 4f, y + 4f, panelWidth - 8f, panelHeight - 8f)
        shapeRenderer.color = Color(0.08f, 0.12f, 0.18f, 0.96f)
        shapeRenderer.rect(x + 8f, y + panelHeight - 54f, panelWidth - 16f, 42f)
        shapeRenderer.color = Color(0.58f, 0.76f, 0.93f, 0.22f)
        shapeRenderer.rect(x + 8f, y + panelHeight - 16f, panelWidth - 16f, 3f)

        for (row in 0 until inventory.rows) {
            for (column in 0 until inventory.columns) {
                val slotX = x + 16f + column * (SLOT + GAP)
                val slotY = y + 16f + (inventory.rows - 1 - row) * (SLOT + GAP)
                shapeRenderer.color = Color(0.06f, 0.08f, 0.12f, 1f)
                shapeRenderer.rect(slotX, slotY, SLOT, SLOT)
                shapeRenderer.color = Color(0.22f, 0.31f, 0.42f, 1f)
                shapeRenderer.rect(slotX + 2f, slotY + 2f, SLOT - 4f, SLOT - 4f)
                shapeRenderer.color = Color(0.57f, 0.78f, 0.96f, 0.24f)
                shapeRenderer.rect(slotX + 3f, slotY + SLOT - 9f, SLOT - 6f, 3f)
            }
        }

        val equipX = x + panelWidth + EQUIP_PANEL_GAP
        val equipW = EQUIP_PANEL_WIDTH
        val equipH = panelHeight
        shapeRenderer.color = Color(0.02f, 0.03f, 0.06f, 0.90f)
        shapeRenderer.rect(equipX, y, equipW, equipH)
        shapeRenderer.color = Color(0.15f, 0.20f, 0.29f, 0.96f)
        shapeRenderer.rect(equipX + 2f, y + 2f, equipW - 4f, equipH - 4f)
        shapeRenderer.color = Color(0.06f, 0.09f, 0.14f, 0.96f)
        shapeRenderer.rect(equipX + 6f, y + 6f, equipW - 12f, equipH - 12f)
        shapeRenderer.color = Color(0.08f, 0.12f, 0.18f, 0.96f)
        shapeRenderer.rect(equipX + 10f, y + equipH - 50f, equipW - 20f, 34f)
        shapeRenderer.color = Color(0.58f, 0.76f, 0.93f, 0.22f)
        shapeRenderer.rect(equipX + 10f, y + equipH - 20f, equipW - 20f, 3f)
    }

    fun drawText(batch: SpriteBatch, sprites: PixelSpriteAtlas, inventory: Inventory, gold: Int, width: Float, height: Float) {
        val bounds = panelBounds(inventory, width, height)
        val x = bounds.x
        val y = bounds.y
        val panelWidth = bounds.width
        val panelHeight = bounds.height
        font.color = Color(0.92f, 0.97f, 1f, 1f)
        font.draw(batch, "INVENTORY", x + 16f, y + panelHeight - 18f)
        font.draw(batch, "COL $gold", x + panelWidth - 98f, y + panelHeight - 18f)
        font.color = Color(0.76f, 0.86f, 0.97f, 1f)
        font.draw(batch, "E equip", x + 16f, y + panelHeight - 38f)
        font.draw(batch, "Q use", x + 104f, y + panelHeight - 38f)

        val slots = inventory.allSlots()
        for (index in slots.indices) {
            val stack = slots[index] ?: continue
            val column = index % inventory.columns
            val row = index / inventory.columns
            val slotX = x + 16f + column * (SLOT + GAP)
            val slotY = y + 16f + (inventory.rows - 1 - row) * (SLOT + GAP)
            batch.draw(sprites.lootFrame(stack.item.type), slotX + 16f, slotY + 24f, 16f, 16f)
            font.color = Color(0.92f, 0.96f, 1f, 1f)
            font.draw(batch, stack.item.name.take(7), slotX + 5f, slotY + 22f)
            font.color = Color(0.78f, 0.93f, 0.83f, 1f)
            font.draw(batch, "x${stack.quantity}", slotX + 5f, slotY + 12f)
        }

        font.color = Color(0.92f, 0.97f, 1f, 1f)
        val equipX = x + panelWidth + 28f
        font.draw(batch, "EQUIPMENT", equipX, y + panelHeight - 22f)
        var equipY = y + 44f
        for (slot in EquipmentSlot.values()) {
            val item = inventory.equipped(slot)?.name ?: "-"
            font.color = Color(0.75f, 0.86f, 0.98f, 1f)
            font.draw(batch, "${slot.name}:", equipX, equipY)
            font.color = Color(0.94f, 0.96f, 0.90f, 1f)
            font.draw(batch, item.take(18), equipX + 86f, equipY)
            equipY += 18f
        }
    }

    private fun panelBounds(inventory: Inventory, width: Float, height: Float): PanelBounds {
        val panelWidth = inventory.columns * SLOT + (inventory.columns - 1) * GAP + 32f
        val panelHeight = inventory.rows * SLOT + (inventory.rows - 1) * GAP + 64f
        val totalWidth = panelWidth + EQUIP_PANEL_GAP + EQUIP_PANEL_WIDTH
        val baseX = (width - totalWidth) * 0.5f
        return PanelBounds(baseX, (height - panelHeight) * 0.5f, panelWidth, panelHeight)
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
        const val EQUIP_PANEL_WIDTH = 270f
        const val EQUIP_PANEL_GAP = 14f
    }
}
