package com.sao.aincrad.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.sao.aincrad.entities.BossEnemy
import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.maps.FloorMap

class HudRenderer(
    private val font: BitmapFont,
    private val layout: GlyphLayout,
) {
    fun drawPanels(shapeRenderer: ShapeRenderer, width: Float, height: Float) {
        shapeRenderer.color = Color(0f, 0f, 0f, 0.35f)
        shapeRenderer.rect(16f, height - 152f, 540f, 124f)
        shapeRenderer.rect(width - 240f, height - 88f, 224f, 60f)
    }

    fun drawText(
        batch: SpriteBatch,
        width: Float,
        height: Float,
        player: PlayerEntity,
        floor: FloorMap,
        objective: String,
        pickupToast: String,
        pickupToastTimer: Float,
        boss: BossEnemy?,
        debugLines: List<String> = emptyList(),
    ) {
        font.color = Color.WHITE
        font.draw(batch, "HP ${player.stats.hp}/${player.stats.maxHp}", 28f, height - 42f)
        font.draw(batch, "MP ${player.stats.mp}/${player.stats.maxMp}", 28f, height - 64f)
        font.draw(batch, "EXP ${player.stats.exp}/${player.stats.nextLevelExp}  Col ${player.stats.gold}", 28f, height - 86f)
        font.draw(batch, "SP ${player.stats.statPoints} Z STR X AGI C INT V DEF", 28f, height - 108f)
        font.draw(batch, "Objective: $objective", 28f, height - 128f)
        font.draw(batch, "Floor ${floor.floorNumber} / 100", width - 214f, height - 48f)
        font.draw(batch, floor.floorName, width - 214f, height - 70f)

        if (pickupToastTimer > 0f) {
            font.color = Color(0.9f, 1f, 0.72f, pickupToastTimer.coerceIn(0f, 1f))
            layout.setText(font, pickupToast)
            font.draw(batch, pickupToast, (width - layout.width) * 0.5f, height - 132f)
        }

        if (boss != null) {
            font.color = Color.WHITE
            layout.setText(font, boss.title())
            font.draw(batch, boss.title(), (width - layout.width) * 0.5f, height - 18f)
        }

        if (debugLines.isNotEmpty()) {
            var y = height - 170f
            font.color = Color(0.65f, 0.95f, 1f, 1f)
            debugLines.forEach { line ->
                font.draw(batch, line, 28f, y)
                y -= 18f
            }
        }
    }

    fun drawWorldBars(
        shapeRenderer: ShapeRenderer,
        cameraCenterX: Float,
        cameraTopY: Float,
        player: PlayerEntity,
        enemies: List<EnemyEntity>,
    ) {
        drawBar(shapeRenderer, player.position.x, player.position.y + PlayerEntity.HEIGHT + 10f, 32f, 5f, player.stats.hp.toFloat() / player.stats.maxHp, Color.RED)
        drawBar(shapeRenderer, player.position.x, player.position.y + PlayerEntity.HEIGHT + 16f, 32f, 4f, player.stats.mp.toFloat() / player.stats.maxMp, Color.BLUE)

        for (enemy in enemies) {
            if (enemy.isDead) continue
            drawBar(shapeRenderer, enemy.position.x - 2f, enemy.position.y + enemy.bounds.height + 6f, enemy.bounds.width + 4f, 4f, enemy.hp.toFloat() / enemy.maxHp, Color(0.92f, 0.18f, 0.18f, 1f))
        }

        enemies.filterIsInstance<BossEnemy>().firstOrNull { !it.isDead }?.let { boss ->
            drawBar(shapeRenderer, cameraCenterX - 120f, cameraTopY - 34f, 240f, 10f, boss.hp.toFloat() / boss.maxHp, Color(0.95f, 0.10f, 0.12f, 1f))
            if (boss.shieldTimer > 0f) {
                drawBar(shapeRenderer, boss.position.x - 4f, boss.position.y + boss.bounds.height + 14f, boss.bounds.width + 8f, 4f, boss.shieldTimer / 1.6f, Color(0.65f, 0.88f, 1f, 1f))
            }
        }
    }

    fun drawMiniMap(
        shapeRenderer: ShapeRenderer,
        width: Float,
        height: Float,
        floor: FloorMap,
        playerBounds: Rectangle,
        enemies: List<EnemyEntity>,
    ) {
        val size = 104f
        val x = width - size - 18f
        val y = height - size - 104f
        shapeRenderer.color = Color(0f, 0f, 0f, 0.55f)
        shapeRenderer.rect(x, y, size, size)
        shapeRenderer.color = Color(0.18f, 0.21f, 0.24f, 1f)
        shapeRenderer.rect(x + 4f, y + 4f, size - 8f, size - 8f)

        fun mapPoint(rect: Rectangle): Pair<Float, Float> {
            val px = x + 4f + (rect.x / floor.worldBounds.width) * (size - 8f)
            val py = y + 4f + (rect.y / floor.worldBounds.height) * (size - 8f)
            return px to py
        }

        val playerPoint = mapPoint(playerBounds)
        shapeRenderer.color = Color(0.3f, 0.75f, 1f, 1f)
        shapeRenderer.rect(playerPoint.first, playerPoint.second, 4f, 4f)
        shapeRenderer.color = Color(1f, 0.2f, 0.2f, 1f)
        enemies.filter { !it.isDead }.forEach {
            val point = mapPoint(it.bounds)
            shapeRenderer.rect(point.first, point.second, 3f, 3f)
        }
        shapeRenderer.color = Color(1f, 0.86f, 0.28f, 1f)
        floor.exitZone?.let {
            val point = mapPoint(it)
            shapeRenderer.rect(point.first, point.second, 5f, 5f)
        }
    }

    private fun drawBar(shapeRenderer: ShapeRenderer, x: Float, y: Float, width: Float, height: Float, progress: Float, color: Color) {
        val clamped = progress.coerceIn(0f, 1f)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        shapeRenderer.rect(x, y, width, height)
        shapeRenderer.color = color
        shapeRenderer.rect(x + 1f, y + 1f, (width - 2f) * clamped, height - 2f)
    }
}
