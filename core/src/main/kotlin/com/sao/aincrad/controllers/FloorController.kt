package com.sao.aincrad.controllers

import com.badlogic.gdx.math.Rectangle
import com.sao.aincrad.entities.NpcEntity
import com.sao.aincrad.maps.FloorMap
import com.sao.aincrad.systems.EnemySpawnSystem
import com.sao.aincrad.utils.AudioManager
import com.sao.aincrad.utils.SaveManager

class FloorController(
    private val audioManager: AudioManager,
    @Suppress("UNUSED_PARAMETER") private val saveManager: SaveManager,
) {
    private val route = listOf(1, 2, 10, 22, 25, 35, 47, 50, 55, 67, 74, 75, 90, 100)
    private var spawnSystem: EnemySpawnSystem = EnemySpawnSystem(emptyList())

    fun bootstrap(state: WorldState) {
        placePlayerAtSafeSpawn(state)
        spawnSystem = EnemySpawnSystem(state.floor.enemySpawns)
        state.enemies.clear()
        state.enemies += spawnSystem.initialEnemies()
        state.npcs.clear()
        state.npcs += state.floor.npcSpawns.map { NpcEntity(it) }
        audioManager.playFloorMusic(state.floor.floorNumber)
    }

    fun currentSpawnSystem(): EnemySpawnSystem = spawnSystem

    fun respawnPlayer(state: WorldState) {
        placePlayerAtSafeSpawn(state)
    }

    fun loadFloor(state: WorldState, floorNumber: Int, autoSave: Boolean = true) {
        val oldFloor = state.floor
        state.floor = FloorMap.load(floorNumber)
        oldFloor.dispose()

        placePlayerAtSafeSpawn(state)
        state.lootDrops.clear()
        state.damagePopups.clear()

        spawnSystem = EnemySpawnSystem(state.floor.enemySpawns)
        state.enemies.clear()
        state.enemies += spawnSystem.initialEnemies()
        state.npcs.clear()
        state.npcs += state.floor.npcSpawns.map { NpcEntity(it) }

        state.ui.pickupToast = "Floor ${state.floor.floorNumber}: ${state.floor.floorName}"
        state.ui.pickupToastTimer = 2.2f
        audioManager.playFloorMusic(state.floor.floorNumber)
        if (state.floor.floorNumber == 100) {
            audioManager.playSfx("boss_roar")
        }
    }

    fun nextFloorFor(currentFloor: Int): Int {
        val index = route.indexOf(currentFloor)
        if (index < 0) return 1
        return route[(index + 1) % route.size]
    }

    private fun placePlayerAtSafeSpawn(state: WorldState) {
        val spawn = state.floor.playerSpawn
        val startX = spawn.x.coerceIn(
            state.floor.worldBounds.x,
            state.floor.worldBounds.x + state.floor.worldBounds.width - state.player.bounds.width,
        )
        val startY = spawn.y.coerceIn(
            state.floor.worldBounds.y,
            state.floor.worldBounds.y + state.floor.worldBounds.height - state.player.bounds.height,
        )
        val safe = findNearestFreePosition(
            startX = startX,
            startY = startY,
            width = state.player.bounds.width,
            height = state.player.bounds.height,
            collisions = state.floor.collisionRects,
            world = state.floor.worldBounds,
        )
        state.player.position.set(safe.first, safe.second)
        state.player.bounds.setPosition(state.player.position)
        state.player.stopMovement()
    }

    private fun findNearestFreePosition(
        startX: Float,
        startY: Float,
        width: Float,
        height: Float,
        collisions: List<Rectangle>,
        world: Rectangle,
    ): Pair<Float, Float> {
        val probe = Rectangle(startX, startY, width, height)
        if (collisions.none { probe.overlaps(it) }) {
            return startX to startY
        }
        val step = 16f
        for (ring in 1..30) {
            val radius = ring * step
            for (dy in -ring..ring) {
                val y = startY + dy * step
                val x1 = startX - radius
                val x2 = startX + radius
                if (isFree(x1, y, width, height, collisions, world)) return x1 to y
                if (isFree(x2, y, width, height, collisions, world)) return x2 to y
            }
            for (dx in (-ring + 1) until ring) {
                val x = startX + dx * step
                val y1 = startY - radius
                val y2 = startY + radius
                if (isFree(x, y1, width, height, collisions, world)) return x to y1
                if (isFree(x, y2, width, height, collisions, world)) return x to y2
            }
        }
        return startX to startY
    }

    private fun isFree(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        collisions: List<Rectangle>,
        world: Rectangle,
    ): Boolean {
        val clampedX = x.coerceIn(world.x, world.x + world.width - width)
        val clampedY = y.coerceIn(world.y, world.y + world.height - height)
        val probe = Rectangle(clampedX, clampedY, width, height)
        return collisions.none { probe.overlaps(it) }
    }
}
