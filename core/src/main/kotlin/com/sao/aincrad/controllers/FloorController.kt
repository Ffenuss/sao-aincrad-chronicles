package com.sao.aincrad.controllers

import com.sao.aincrad.entities.NpcEntity
import com.sao.aincrad.maps.FloorMap
import com.sao.aincrad.systems.EnemySpawnSystem
import com.sao.aincrad.utils.AudioManager
import com.sao.aincrad.utils.SaveManager

class FloorController(
    private val audioManager: AudioManager,
    @Suppress("UNUSED_PARAMETER") private val saveManager: SaveManager,
) {
    private var spawnSystem: EnemySpawnSystem = EnemySpawnSystem(emptyList())

    fun bootstrap(state: WorldState) {
        spawnSystem = EnemySpawnSystem(state.floor.enemySpawns)
        state.enemies.clear()
        state.enemies += spawnSystem.initialEnemies()
        state.npcs.clear()
        state.npcs += state.floor.npcSpawns.map { NpcEntity(it) }
        audioManager.playFloorMusic(state.floor.floorNumber)
    }

    fun currentSpawnSystem(): EnemySpawnSystem = spawnSystem

    fun loadFloor(state: WorldState, floorNumber: Int, autoSave: Boolean = true) {
        val oldFloor = state.floor
        state.floor = FloorMap.load(floorNumber)
        oldFloor.dispose()

        state.player.position.set(state.floor.playerSpawn.x, state.floor.playerSpawn.y)
        state.player.bounds.setPosition(state.player.position)
        state.player.stopMovement()
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
        return when (currentFloor) {
            1 -> 25
            25 -> 50
            50 -> 75
            75 -> 100
            else -> 1
        }
    }
}
