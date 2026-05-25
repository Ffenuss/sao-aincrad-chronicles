package com.sao.aincrad.maps

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle

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
) {
    fun dispose() {
        renderer.dispose()
        map.dispose()
    }

    companion object {
        fun load(floorNumber: Int): FloorMap {
            val mapPath = when (floorNumber) {
                25 -> "maps/floor25.tmx"
                50 -> "maps/floor50.tmx"
                75 -> "maps/floor75.tmx"
                100 -> "maps/floor100.tmx"
                else -> "maps/floor1.tmx"
            }
            val map = TmxMapLoader().load(mapPath)
            val tileWidth = map.properties.get("tilewidth", Int::class.java) ?: 32
            val tileHeight = map.properties.get("tileheight", Int::class.java) ?: 32
            val mapWidth = map.properties.get("width", Int::class.java) ?: 100
            val mapHeight = map.properties.get("height", Int::class.java) ?: 100
            val floorName = map.properties.get("floorName", String::class.java) ?: "Unknown Floor"
            val worldBounds = Rectangle(0f, 0f, mapWidth * tileWidth.toFloat(), mapHeight * tileHeight.toFloat())
            val renderer = OrthogonalTiledMapRenderer(map, 1f)

            val collisionRects = extractRectangles(map.layers.get("collision"))
            val spawnRect = extractRectangles(map.layers.get("spawn")).firstOrNull()
                ?: Rectangle(64f, 64f, 32f, 32f)
            val enemySpawns = extractEnemySpawns(map.layers.get("enemy_spawns"))
            val npcSpawns = extractNpcSpawns(map.layers.get("npcs"))
            val exitZone = extractRectangles(map.layers.get("exits")).firstOrNull()

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
                exitZone = exitZone,
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
    }
}
