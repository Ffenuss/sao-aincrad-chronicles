package com.sao.aincrad.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.sao.aincrad.maps.FloorMap
import kotlin.math.sin

class FloorBackdropRenderer {
    fun draw(shape: ShapeRenderer, floor: FloorMap, time: Float) {
        val bounds = floor.worldBounds
        val palette = paletteFor(floor.floorNumber)

        shape.color = palette.base
        shape.rect(bounds.x, bounds.y, bounds.width, bounds.height)

        drawPattern(shape, bounds.x, bounds.y, bounds.width, bounds.height, palette, time)

        when (floor.floorNumber) {
            1 -> drawTown(shape, floor)
            2 -> drawRaidFields(shape, floor)
            10, 22, 25, 35 -> drawForestRoute(shape, floor)
            47, 50, 55 -> drawCityAndLabyrinth(shape, floor)
            67, 74, 75, 90, 100 -> drawFortressFront(shape, floor)
            else -> drawCrossRoad(shape, floor, palette)
        }
        if (floor.map.layers.get("ground") == null) {
            drawCollisionLandmarks(shape, floor, palette)
        }

        floor.exitZone?.let { exit ->
            val pulse = 0.55f + (sin((time * 3.2f).toDouble()) * 0.2f).toFloat()
            shape.color = Color(palette.exit.r, palette.exit.g, palette.exit.b, 0.9f)
            shape.rect(exit.x - 4f, exit.y - 4f, exit.width + 8f, exit.height + 8f)
            shape.color = Color(1f, 1f, 1f, pulse)
            shape.rect(exit.x + 10f, exit.y + 10f, (exit.width - 20f).coerceAtLeast(8f), (exit.height - 20f).coerceAtLeast(8f))
        }
    }

    private fun drawPattern(shape: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, p: FloorPalette, time: Float) {
        val patch = 96f
        var row = 0
        var py = y
        while (py < y + h) {
            var col = 0
            var px = x
            while (px < x + w) {
                val wave = (sin((time * 0.15f + row * 0.4f + col * 0.3f).toDouble()) * 0.02).toFloat()
                val c = if ((row + col) % 2 == 0) p.detailA else p.detailB
                shape.color = Color((c.r + wave).coerceIn(0f, 1f), (c.g + wave).coerceIn(0f, 1f), (c.b + wave).coerceIn(0f, 1f), 1f)
                shape.rect(px, py, patch, patch)
                px += patch
                col += 1
            }
            py += patch
            row += 1
        }
    }

    private fun drawTown(shape: ShapeRenderer, floor: FloorMap) {
        val b = floor.worldBounds
        shape.color = Color(0.57f, 0.56f, 0.50f, 1f)
        shape.rect(b.x + b.width * 0.40f, b.y + 64f, 84f, b.height - 128f)
        shape.rect(b.x + 64f, b.y + b.height * 0.44f, b.width - 128f, 84f)
        drawCrossRoad(shape, floor, paletteFor(1))
        drawFountain(shape, b.x + b.width * 0.47f, b.y + b.height * 0.48f)
        drawGate(shape, b.x + b.width - 240f, b.y + b.height - 240f)
        drawMarketRow(shape, b.x + 110f, b.y + b.height * 0.61f, 4)
    }

    private fun drawRaidFields(shape: ShapeRenderer, floor: FloorMap) {
        val b = floor.worldBounds
        shape.color = Color(0.39f, 0.43f, 0.30f, 1f)
        shape.rect(b.x + 56f, b.y + b.height * 0.48f, b.width - 112f, 64f)
        shape.rect(b.x + b.width * 0.6f, b.y + 56f, 64f, b.height - 112f)
        drawStones(shape, b, 0.18f)
        drawCampfire(shape, b.x + b.width * 0.32f, b.y + b.height * 0.58f)
    }

    private fun drawForestRoute(shape: ShapeRenderer, floor: FloorMap) {
        val b = floor.worldBounds
        shape.color = Color(0.24f, 0.33f, 0.21f, 1f)
        shape.rect(b.x + 64f, b.y + b.height * 0.42f, b.width - 128f, 74f)
        shape.rect(b.x + b.width * 0.28f, b.y + 64f, 74f, b.height - 128f)
        drawTrees(shape, b)
        drawRuins(shape, b.x + b.width * 0.58f, b.y + b.height * 0.50f)
    }

    private fun drawCityAndLabyrinth(shape: ShapeRenderer, floor: FloorMap) {
        val b = floor.worldBounds
        shape.color = Color(0.33f, 0.33f, 0.36f, 1f)
        shape.rect(b.x + 64f, b.y + b.height * 0.48f, b.width - 128f, 82f)
        shape.rect(b.x + b.width * 0.5f - 42f, b.y + 64f, 84f, b.height - 128f)
        drawStones(shape, b, 0.12f)
        drawPlaza(shape, b.x + b.width * 0.52f, b.y + b.height * 0.52f)
    }

    private fun drawFortressFront(shape: ShapeRenderer, floor: FloorMap) {
        val b = floor.worldBounds
        shape.color = Color(0.28f, 0.26f, 0.30f, 1f)
        shape.rect(b.x + 64f, b.y + b.height * 0.50f, b.width - 128f, 96f)
        shape.rect(b.x + b.width * 0.5f - 48f, b.y + 64f, 96f, b.height - 128f)
        shape.color = Color(0.36f, 0.34f, 0.40f, 1f)
        shape.rect(b.x + b.width * 0.5f - 220f, b.y + b.height * 0.5f - 220f, 440f, 440f)
        shape.color = Color(0.45f, 0.44f, 0.50f, 1f)
        shape.rect(b.x + b.width * 0.5f - 140f, b.y + b.height * 0.5f - 140f, 280f, 280f)
        drawGate(shape, b.x + b.width * 0.5f - 70f, b.y + b.height * 0.5f + 110f)
    }

    private fun drawCrossRoad(shape: ShapeRenderer, floor: FloorMap, p: FloorPalette) {
        val b = floor.worldBounds
        shape.color = p.road
        shape.rect(b.x + 56f, b.y + b.height * 0.46f, b.width - 112f, 72f)
        shape.rect(b.x + b.width * 0.46f, b.y + 56f, 72f, b.height - 112f)
        shape.color = p.obstacle
        shape.rect(b.x + b.width * 0.5f - 168f, b.y + b.height * 0.5f - 168f, 336f, 336f)
    }

    private fun drawTrees(shape: ShapeRenderer, b: com.badlogic.gdx.math.Rectangle) {
        var x = b.x + 120f
        while (x < b.x + b.width - 160f) {
            drawTree(shape, x, b.y + 120f)
            drawTree(shape, x, b.y + b.height - 200f)
            x += 140f
        }
    }

    private fun drawStones(shape: ShapeRenderer, b: com.badlogic.gdx.math.Rectangle, alpha: Float) {
        shape.color = Color(0.75f, 0.78f, 0.84f, alpha)
        var y = b.y + 120f
        while (y < b.y + b.height - 120f) {
            shape.rect(b.x + 120f, y, 42f, 18f)
            shape.rect(b.x + b.width - 170f, y + 36f, 36f, 16f)
            y += 140f
        }
    }

    private fun drawTree(shape: ShapeRenderer, x: Float, y: Float) {
        shape.color = Color(0.30f, 0.21f, 0.11f, 1f)
        shape.rect(x + 10f, y, 12f, 18f)
        shape.color = Color(0.18f, 0.54f, 0.24f, 1f)
        shape.rect(x, y + 14f, 34f, 28f)
        shape.color = Color(0.24f, 0.62f, 0.30f, 1f)
        shape.rect(x + 6f, y + 24f, 22f, 16f)
    }

    private fun drawCollisionLandmarks(shape: ShapeRenderer, floor: FloorMap, palette: FloorPalette) {
        val bounds = floor.worldBounds
        floor.collisionRects.forEach { rect ->
            if (isWorldBorder(rect, bounds)) return@forEach
            if (rect.width < 24f || rect.height < 24f) return@forEach
            val isWall = rect.width >= 200f || rect.height >= 200f
            val base = if (isWall) palette.obstacle else Color(palette.obstacle.r * 0.9f, palette.obstacle.g * 0.9f, palette.obstacle.b * 0.9f, 1f)
            shape.color = Color(base.r, base.g, base.b, 0.78f)
            shape.rect(rect.x, rect.y, rect.width, rect.height)
            shape.color = Color((base.r + 0.08f).coerceAtMost(1f), (base.g + 0.08f).coerceAtMost(1f), (base.b + 0.08f).coerceAtMost(1f), 0.45f)
            shape.rect(rect.x + 4f, rect.y + rect.height - 8f, (rect.width - 8f).coerceAtLeast(4f), 4f)
            shape.color = Color(base.r * 0.75f, base.g * 0.75f, base.b * 0.75f, 0.35f)
            shape.rect(rect.x + 2f, rect.y + 2f, 4f, (rect.height - 4f).coerceAtLeast(2f))
        }
    }

    private fun drawFountain(shape: ShapeRenderer, x: Float, y: Float) {
        shape.color = Color(0.42f, 0.44f, 0.48f, 1f)
        shape.rect(x, y, 96f, 96f)
        shape.color = Color(0.28f, 0.73f, 0.95f, 1f)
        shape.rect(x + 12f, y + 12f, 72f, 72f)
        shape.color = Color(0.84f, 0.95f, 1f, 0.65f)
        shape.rect(x + 24f, y + 24f, 48f, 48f)
    }

    private fun drawGate(shape: ShapeRenderer, x: Float, y: Float) {
        shape.color = Color(0.34f, 0.26f, 0.16f, 1f)
        shape.rect(x, y, 140f, 20f)
        shape.rect(x + 10f, y + 20f, 16f, 64f)
        shape.rect(x + 114f, y + 20f, 16f, 64f)
        shape.color = Color(0.82f, 0.72f, 0.36f, 1f)
        shape.rect(x + 18f, y + 72f, 104f, 10f)
    }

    private fun drawMarketRow(shape: ShapeRenderer, startX: Float, y: Float, count: Int) {
        repeat(count) { i ->
            val x = startX + i * 78f
            shape.color = Color(0.43f, 0.32f, 0.19f, 1f)
            shape.rect(x, y, 54f, 44f)
            shape.color = if (i % 2 == 0) Color(0.88f, 0.37f, 0.31f, 1f) else Color(0.31f, 0.53f, 0.88f, 1f)
            shape.rect(x - 2f, y + 34f, 58f, 12f)
        }
    }

    private fun drawCampfire(shape: ShapeRenderer, x: Float, y: Float) {
        shape.color = Color(0.36f, 0.28f, 0.20f, 1f)
        shape.rect(x, y, 70f, 22f)
        shape.color = Color(1f, 0.60f, 0.24f, 0.90f)
        shape.rect(x + 25f, y + 18f, 20f, 18f)
        shape.color = Color(1f, 0.84f, 0.42f, 0.85f)
        shape.rect(x + 30f, y + 24f, 10f, 12f)
    }

    private fun drawRuins(shape: ShapeRenderer, x: Float, y: Float) {
        shape.color = Color(0.42f, 0.47f, 0.40f, 1f)
        shape.rect(x, y, 120f, 24f)
        shape.rect(x + 10f, y + 24f, 22f, 56f)
        shape.rect(x + 88f, y + 24f, 22f, 56f)
    }

    private fun drawPlaza(shape: ShapeRenderer, x: Float, y: Float) {
        shape.color = Color(0.52f, 0.52f, 0.56f, 1f)
        shape.rect(x - 80f, y - 80f, 160f, 160f)
        shape.color = Color(0.41f, 0.43f, 0.47f, 1f)
        shape.rect(x - 36f, y - 36f, 72f, 72f)
    }

    private fun isWorldBorder(rect: Rectangle, bounds: Rectangle): Boolean {
        val touchesLeft = rect.x <= bounds.x + 0.1f && rect.height >= bounds.height - 64f
        val touchesRight = rect.x + rect.width >= bounds.x + bounds.width - 0.1f && rect.height >= bounds.height - 64f
        val touchesBottom = rect.y <= bounds.y + 0.1f && rect.width >= bounds.width - 64f
        val touchesTop = rect.y + rect.height >= bounds.y + bounds.height - 0.1f && rect.width >= bounds.width - 64f
        return touchesLeft || touchesRight || touchesBottom || touchesTop
    }

    private fun paletteFor(floor: Int): FloorPalette {
        return when {
            floor >= 100 -> FloorPalette(Color(0.15f, 0.10f, 0.18f, 1f), Color(0.20f, 0.13f, 0.25f, 1f), Color(0.17f, 0.11f, 0.22f, 1f), Color(0.38f, 0.30f, 0.46f, 1f), Color(0.30f, 0.22f, 0.36f, 1f), Color(0.88f, 0.72f, 0.96f, 1f))
            floor >= 75 -> FloorPalette(Color(0.13f, 0.16f, 0.20f, 1f), Color(0.18f, 0.22f, 0.28f, 1f), Color(0.15f, 0.19f, 0.24f, 1f), Color(0.30f, 0.35f, 0.42f, 1f), Color(0.24f, 0.28f, 0.34f, 1f), Color(0.62f, 0.86f, 1f, 1f))
            floor >= 50 -> FloorPalette(Color(0.16f, 0.15f, 0.12f, 1f), Color(0.22f, 0.20f, 0.16f, 1f), Color(0.19f, 0.17f, 0.14f, 1f), Color(0.36f, 0.31f, 0.23f, 1f), Color(0.28f, 0.24f, 0.18f, 1f), Color(0.94f, 0.81f, 0.44f, 1f))
            floor >= 25 -> FloorPalette(Color(0.12f, 0.18f, 0.14f, 1f), Color(0.17f, 0.25f, 0.18f, 1f), Color(0.14f, 0.22f, 0.16f, 1f), Color(0.27f, 0.36f, 0.24f, 1f), Color(0.20f, 0.29f, 0.19f, 1f), Color(0.95f, 0.92f, 0.48f, 1f))
            else -> FloorPalette(Color(0.14f, 0.19f, 0.14f, 1f), Color(0.19f, 0.26f, 0.18f, 1f), Color(0.16f, 0.23f, 0.17f, 1f), Color(0.32f, 0.38f, 0.27f, 1f), Color(0.24f, 0.30f, 0.22f, 1f), Color(0.96f, 0.87f, 0.36f, 1f))
        }
    }
}

private data class FloorPalette(
    val base: Color,
    val detailA: Color,
    val detailB: Color,
    val road: Color,
    val obstacle: Color,
    val exit: Color,
)
