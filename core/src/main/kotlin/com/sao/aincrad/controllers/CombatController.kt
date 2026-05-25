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
        spawnSystem.update(delta, state.enemies)
        updateEnemies(state, delta)
        resolvePlayerAttack(state)
        updateLoot(state, delta)
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

    private fun resolvePlayerAttack(state: WorldState) {
        if (state.player.attackTimer <= 0f) {
            lastProcessedAttackId = -1
            return
        }
        if (state.player.attackSequenceId == lastProcessedAttackId) return

        val hitbox = state.player.attackHitbox()
        val crit = MathUtils.random() < 0.1f
        for (enemy in state.enemies) {
            if (enemy.isDead || !enemy.bounds.overlaps(hitbox)) continue

            val baseDamage = ((state.player.stats.str + state.inventory.statBonuses().str) * state.player.attackMultiplier).toInt().coerceAtLeast(1)
            val reduced = (baseDamage - enemy.defense).coerceAtLeast(1)
            val finalDamage = if (crit) (reduced * 1.5f).toInt().coerceAtLeast(1) else reduced
            enemy.takeDamage(finalDamage)
            audioManager.playSfx("hit")
            state.damagePopups += DamagePopup(
                finalDamage.toString(),
                Vector2(enemy.position.x + enemy.bounds.width * 0.5f, enemy.position.y + enemy.bounds.height + 8f),
                if (crit) Color.YELLOW else Color.WHITE,
            )

            if (enemy.isDead) onEnemyDeath(state, enemy)
        }
        lastProcessedAttackId = state.player.attackSequenceId
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
