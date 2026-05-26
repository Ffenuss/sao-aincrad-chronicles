package com.sao.aincrad.maps

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.graphics.Texture

data class EnemySpawn(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val respawnSeconds: Float = 12f,
)

data class NpcSpawn(
    val id: String,
    val name: String,
    val role: String,
    val x: Float,
    val y: Float,
    val dialog: String,
)

data class FloorMap(
    val floorNumber: Int,
    val floorName: String,
    val map: TiledMap,
    val renderer: OrthogonalTiledMapRenderer,
    val collisionRects: List<Rectangle>,
    val worldBounds: Rectangle,
    val playerSpawn: Rectangle,
    val enemySpawns: List<EnemySpawn>,
    val npcSpawns: List<NpcSpawn>,
    val exitZone: Rectangle?,
    val exitTargetFloor: Int?,
    val hasImageBackdrop: Boolean,
) {
    fun dispose() {
        renderer.dispose()
        map.dispose()
    }

    companion object {
        fun load(floorNumber: Int): FloorMap {
            val candidatePath = "maps/floor${floorNumber}.tmx"
            val mapPath = if (Gdx.files.internal(candidatePath).exists()) candidatePath else "maps/floor1.tmx"
            val map = TmxMapLoader().load(mapPath)
            var hasImageBackdrop = false
            map.layers.forEach { layer ->
                if (layer is TiledMapImageLayer) {
                    hasImageBackdrop = true
                    layer.textureRegion.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                }
            }
            map.tileSets.forEach { tileSet ->
                tileSet.forEach { tile ->
                    tile.textureRegion?.texture?.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                }
            }
            val tileWidth = map.properties.get("tilewidth", Int::class.java) ?: 32
            val tileHeight = map.properties.get("tileheight", Int::class.java) ?: 32
            val mapWidth = map.properties.get("width", Int::class.java) ?: 100
            val mapHeight = map.properties.get("height", Int::class.java) ?: 100
            val floorName = map.properties.get("floorName", String::class.java) ?: "Unknown Floor"
            val worldBounds = Rectangle(0f, 0f, mapWidth * tileWidth.toFloat(), mapHeight * tileHeight.toFloat())
            val renderer = OrthogonalTiledMapRenderer(map, 1f)

            val collisionRects = extractRectangles(map.layers.get("collision")) + extractBlockedTiles(map.layers.get("blocked"))
            val spawnRect = extractRectangles(map.layers.get("spawn")).firstOrNull()
                ?: Rectangle(64f, 64f, 32f, 32f)
            val enemySpawns = extractEnemySpawns(map.layers.get("enemy_spawns"))
            val npcSpawns = extractNpcSpawns(map.layers.get("npcs"))
            val exitInfo = extractExit(map.layers.get("exits"))

            if (Gdx.app != null) {
                Gdx.app.log("FloorMap", "Loaded Floor $floorNumber with ${enemySpawns.size} enemy spawns")
            }

            return FloorMap(
                floorNumber = floorNumber,
                floorName = floorName,
                map = map,
                renderer = renderer,
                collisionRects = collisionRects,
                worldBounds = worldBounds,
                playerSpawn = spawnRect,
                enemySpawns = enemySpawns,
                npcSpawns = npcSpawns,
                exitZone = exitInfo.first,
                exitTargetFloor = exitInfo.second,
                hasImageBackdrop = hasImageBackdrop,
            )
        }

        private fun extractRectangles(layer: MapLayer?): List<Rectangle> {
            val objects = layer?.objects ?: return emptyList()
            val result = mutableListOf<Rectangle>()
            for (mapObject in objects) {
                if (mapObject is RectangleMapObject) {
                    val rect = mapObject.rectangle
                    result += Rectangle(rect.x, rect.y, rect.width, rect.height)
                }
            }
            return result
        }

        private fun extractEnemySpawns(layer: MapLayer?): List<EnemySpawn> {
            val objects = layer?.objects ?: return emptyList()
            val result = mutableListOf<EnemySpawn>()
            for (mapObject in objects) {
                if (mapObject is RectangleMapObject) {
                    val rect = mapObject.rectangle
                    val type = mapObject.properties.get("type", String::class.java) ?: "slime"
                    val respawn = when (val raw = mapObject.properties.get("respawn")) {
                        is Number -> raw.toFloat()
                        is String -> raw.toFloatOrNull() ?: 12f
                        else -> 12f
                    }
                    val rawName = mapObject.name
                    val spawnId = if (rawName.isNullOrBlank()) "spawn_${result.size}" else rawName
                    result += EnemySpawn(
                        id = spawnId,
                        type = type,
                        x = rect.x,
                        y = rect.y,
                        respawnSeconds = respawn,
                    )
                }
            }
            return result
        }

        private fun extractNpcSpawns(layer: MapLayer?): List<NpcSpawn> {
            val objects = layer?.objects ?: return emptyList()
            val result = mutableListOf<NpcSpawn>()
            for (mapObject in objects) {
                if (mapObject is RectangleMapObject) {
                    val rect = mapObject.rectangle
                    val rawName = mapObject.name
                    result += NpcSpawn(
                        id = if (rawName.isNullOrBlank()) "npc_${result.size}" else rawName,
                        name = mapObject.properties.get("name", String::class.java) ?: rawName ?: "NPC",
                        role = mapObject.properties.get("role", String::class.java) ?: "dialog",
                        x = rect.x,
                        y = rect.y,
                        dialog = mapObject.properties.get("dialog", String::class.java) ?: "...",
                    )
                }
            }
            return result
        }

        private fun extractBlockedTiles(layer: MapLayer?): List<Rectangle> {
            val tileLayer = layer as? TiledMapTileLayer ?: return emptyList()
            val result = mutableListOf<Rectangle>()
            val tileWidth = tileLayer.tileWidth.toFloat()
            val tileHeight = tileLayer.tileHeight.toFloat()
            for (y in 0 until tileLayer.height) {
                for (x in 0 until tileLayer.width) {
                    if (tileLayer.getCell(x, y) != null) {
                        result += Rectangle(x * tileWidth, y * tileHeight, tileWidth, tileHeight)
                    }
                }
            }
            return result
        }

        private fun extractExit(layer: MapLayer?): Pair<Rectangle?, Int?> {
            val objects = layer?.objects ?: return null to null
            for (mapObject in objects) {
                if (mapObject is RectangleMapObject) {
                    val rect = mapObject.rectangle
                    val toFloor = when (val raw = mapObject.properties.get("toFloor")) {
                        is Number -> raw.toInt()
                        is String -> raw.toIntOrNull()
                        else -> null
                    }
                    return Rectangle(rect.x, rect.y, rect.width, rect.height) to toFloor
                }
            }
            return null to null
        }
    }
}
