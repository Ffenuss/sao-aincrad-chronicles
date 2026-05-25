package com.sao.aincrad.systems

import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.DarkElfEnemy
import com.sao.aincrad.entities.BossEnemy
import com.sao.aincrad.entities.ConfigEnemy
import com.sao.aincrad.entities.SlimeEnemy
import com.sao.aincrad.maps.EnemySpawn

class EnemySpawnSystem(
    private val spawns: List<EnemySpawn>,
) {
    private val activeBySpawnId = mutableMapOf<String, EnemyEntity>()
    private val respawnTimers = mutableMapOf<String, Float>()

    fun initialEnemies(): MutableList<EnemyEntity> {
        activeBySpawnId.clear()
        respawnTimers.clear()
        val enemies = mutableListOf<EnemyEntity>()
        for (spawn in spawns) {
            val enemy = createEnemy(spawn)
            activeBySpawnId[spawn.id] = enemy
            enemies += enemy
        }
        return enemies
    }

    fun update(delta: Float, enemies: MutableList<EnemyEntity>) {
        for (spawn in spawns) {
            val active = activeBySpawnId[spawn.id]
            if (active != null && active.isDead) {
                activeBySpawnId.remove(spawn.id)
                respawnTimers[spawn.id] = spawn.respawnSeconds
            }
        }
        enemies.removeAll { it.isDead }

        val iterator = respawnTimers.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val remaining = entry.value - delta
            if (remaining <= 0f) {
                val spawn = spawns.firstOrNull { it.id == entry.key } ?: continue
                val enemy = createEnemy(spawn)
                activeBySpawnId[spawn.id] = enemy
                enemies += enemy
                iterator.remove()
            } else {
                entry.setValue(remaining)
            }
        }
    }

    private fun createEnemy(spawn: EnemySpawn): EnemyEntity {
        return when (spawn.type) {
            "skeleton" -> ConfigEnemy(spawn.x, spawn.y, "skeleton", 44, 240f, 34f, 48f, 10, 5)
            "knight" -> ConfigEnemy(spawn.x, spawn.y, "knight", 68, 260f, 38f, 54f, 14, 8)
            "heathcliff" -> BossEnemy(spawn.x, spawn.y, "Heathcliff", 260, 20)
            "floor_boss" -> BossEnemy(spawn.x, spawn.y, "Guardian", 160, 16)
            "dark_elf" -> DarkElfEnemy(spawn.x, spawn.y)
            "slime" -> SlimeEnemy(spawn.x, spawn.y)
            else -> SlimeEnemy(spawn.x, spawn.y)
        }
    }
}
