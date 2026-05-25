package com.sao.aincrad.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.LootDrop
import com.sao.aincrad.items.Items

object LootSystem {
    fun dropsFor(enemy: EnemyEntity): List<LootDrop> {
        val center = Vector2(enemy.position.x + enemy.bounds.width * 0.5f, enemy.position.y + enemy.bounds.height * 0.5f)
        val drops = mutableListOf<LootDrop>()

        drops += LootDrop(Items.col, MathUtils.random(2, 5), center.cpy())

        if (MathUtils.random() < 0.25f) {
            drops += LootDrop(Items.hpPotion, 1, center.cpy().add(16f, 0f))
        }
        if (MathUtils.random() < 0.12f) {
            drops += LootDrop(Items.floorKeyShard, 1, center.cpy().add(-16f, 0f))
        }

        return drops
    }
}
