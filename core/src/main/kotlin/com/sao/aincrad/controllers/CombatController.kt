package com.sao.aincrad.controllers

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.entities.BossEnemy
import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.LootDrop
import com.sao.aincrad.items.ItemType
import com.sao.aincrad.items.Items
import com.sao.aincrad.systems.EnemySpawnSystem
import com.sao.aincrad.systems.LootSystem
import com.sao.aincrad.story.StoryController
import com.sao.aincrad.ui.DamagePopup
import com.sao.aincrad.utils.AudioManager
import com.sao.aincrad.utils.SaveManager

class CombatController(
    private val audioManager: AudioManager,
    private val saveManager: SaveManager,
    private val storyController: StoryController,
) {
    private var lastProcessedAttackId = -1

    fun resetAttackSequence() {
        lastProcessedAttackId = -1
    }

    fun update(state: WorldState, delta: Float, spawnSystem: EnemySpawnSystem) {
        update(state, delta, spawnSystem, onEnemyDamaged = null, onEnemyKilled = null)
    }

    fun update(
        state: WorldState,
        delta: Float,
        spawnSystem: EnemySpawnSystem,
        onEnemyDamaged: ((EnemyEntity, Int, Boolean) -> Unit)? = null,
        onEnemyKilled: ((EnemyEntity) -> Unit)? = null,
    ) {
        spawnSystem.update(delta, state.enemies)
        updateEnemies(state, delta)
        resolvePlayerAttack(state, onEnemyDamaged, onEnemyKilled)
        updateLoot(state, delta)
        updateDamagePopups(state, delta)
    }

    fun updateReplica(state: WorldState, delta: Float) {
        updateDamagePopups(state, delta)
    }

    private fun updateEnemies(state: WorldState, delta: Float) {
        for (enemy in state.enemies) {
            if (enemy.isDead) continue
            enemy.update(
                delta = delta,
                player = state.player,
                collisionRects = state.floor.collisionRects,
                worldBounds = state.floor.worldBounds,
                onPlayerHit = { damage, x, y ->
                    state.damagePopups += DamagePopup(damage.toString(), Vector2(x, y), Color.RED)
                },
            )
        }
    }

    private fun resolvePlayerAttack(
        state: WorldState,
        onEnemyDamaged: ((EnemyEntity, Int, Boolean) -> Unit)? = null,
        onEnemyKilled: ((EnemyEntity) -> Unit)? = null,
    ) {
        if (state.player.attackTimer <= 0f) {
            lastProcessedAttackId = -1
            return
        }
        if (state.player.attackSequenceId == lastProcessedAttackId) return

        val hitbox = state.player.attackHitbox()
        val crit = MathUtils.random() < 0.1f
        for (enemy in state.enemies) {
            if (enemy.isDead || !isPlayerAttackConnecting(state, enemy, hitbox)) continue

            val baseDamage = ((state.player.stats.str + state.inventory.statBonuses().str) * state.player.attackMultiplier).toInt().coerceAtLeast(1)
            val reduced = (baseDamage - enemy.defense).coerceAtLeast(1)
            val finalDamage = if (crit) (reduced * 1.5f).toInt().coerceAtLeast(1) else reduced
            enemy.takeDamage(finalDamage)
            onEnemyDamaged?.invoke(enemy, finalDamage, crit)
            audioManager.playSfx("hit")
            state.damagePopups += DamagePopup(
                finalDamage.toString(),
                Vector2(enemy.position.x + enemy.bounds.width * 0.5f, enemy.position.y + enemy.bounds.height + 8f),
                if (crit) Color.YELLOW else Color.WHITE,
            )

            if (enemy.isDead) {
                onEnemyDeath(state, enemy)
                onEnemyKilled?.invoke(enemy)
            }
        }
        lastProcessedAttackId = state.player.attackSequenceId
    }

    private fun isPlayerAttackConnecting(state: WorldState, enemy: EnemyEntity, hitbox: com.badlogic.gdx.math.Rectangle): Boolean {
        if (enemy.bounds.overlaps(hitbox)) return true

        val playerCenter = Vector2(
            state.player.bounds.x + state.player.bounds.width * 0.5f,
            state.player.bounds.y + state.player.bounds.height * 0.5f,
        )
        val enemyCenter = Vector2(
            enemy.bounds.x + enemy.bounds.width * 0.5f,
            enemy.bounds.y + enemy.bounds.height * 0.5f,
        )
        val distance2 = playerCenter.dst2(enemyCenter)
        if (distance2 > 48f * 48f) return false

        val dir = when (state.player.facing) {
            com.sao.aincrad.entities.PlayerEntity.Facing.UP -> Vector2(0f, 1f)
            com.sao.aincrad.entities.PlayerEntity.Facing.UP_RIGHT -> Vector2(1f, 1f).nor()
            com.sao.aincrad.entities.PlayerEntity.Facing.RIGHT -> Vector2(1f, 0f)
            com.sao.aincrad.entities.PlayerEntity.Facing.DOWN_RIGHT -> Vector2(1f, -1f).nor()
            com.sao.aincrad.entities.PlayerEntity.Facing.DOWN -> Vector2(0f, -1f)
            com.sao.aincrad.entities.PlayerEntity.Facing.DOWN_LEFT -> Vector2(-1f, -1f).nor()
            com.sao.aincrad.entities.PlayerEntity.Facing.LEFT -> Vector2(-1f, 0f)
            com.sao.aincrad.entities.PlayerEntity.Facing.UP_LEFT -> Vector2(-1f, 1f).nor()
        }
        val toEnemy = enemyCenter.sub(playerCenter).nor()
        return dir.dot(toEnemy) > 0.10f
    }

    private fun onEnemyDeath(state: WorldState, enemy: EnemyEntity) {
        state.player.gainExp(25)
        val drops = LootSystem.dropsFor(enemy)
        state.lootDrops += drops
        state.damagePopups += DamagePopup("KO", Vector2(enemy.position.x + 8f, enemy.position.y + 20f), Color.ORANGE)
        if (drops.isNotEmpty()) {
            state.damagePopups += DamagePopup("Loot", Vector2(enemy.position.x + 2f, enemy.position.y + 34f), Color(0.7f, 1f, 0.55f, 1f))
        }

        if (enemy is BossEnemy) {
            storyController.onBossDefeated(state, enemy)
            state.inventory.add(Items.floorKeyShard, 1)
            state.ui.pickupToast = "Boss defeated: ${enemy.title()}"
            state.ui.pickupToastTimer = 2.5f
            saveManager.save(1, state.floor.floorNumber, state.player, state.inventory, storyController.state)
        }
    }

    fun finalizeEnemyDeath(state: WorldState, enemy: EnemyEntity) {
        onEnemyDeath(state, enemy)
    }

    private fun updateLoot(state: WorldState, delta: Float) {
        val iterator = state.lootDrops.iterator()
        while (iterator.hasNext()) {
            val drop = iterator.next()
            drop.update(delta)
            if (drop.life <= 0f) {
                iterator.remove()
                continue
            }
            if (drop.bounds.overlaps(state.player.bounds)) {
                pickupDrop(state, drop)
                iterator.remove()
            }
        }
    }

    private fun pickupDrop(state: WorldState, drop: LootDrop) {
        if (drop.item.type == ItemType.CURRENCY) {
            state.player.stats.gold += drop.quantity
        } else {
            state.inventory.add(drop.item, drop.quantity)
        }
        state.ui.pickupToast = "+${drop.quantity} ${drop.item.name}"
        state.ui.pickupToastTimer = 1.8f
        audioManager.playSfx("pickup")
    }

    private fun updateDamagePopups(state: WorldState, delta: Float) {
        val iterator = state.damagePopups.iterator()
        while (iterator.hasNext()) {
            val popup = iterator.next()
            popup.life -= delta
            popup.position.y += popup.velocityY * delta
            if (popup.life <= 0f) iterator.remove()
        }
    }
}
