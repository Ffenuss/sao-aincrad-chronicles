package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

abstract class EnemyEntity(
    startX: Float,
    startY: Float,
    width: Float = 28f,
    height: Float = 28f,
) {
    enum class AIState {
        IDLE, PATROL, CHASE, ATTACK, RETREAT, DEATH
    }

    val position = Vector2(startX, startY)
    val velocity = Vector2()
    val bounds = Rectangle(startX, startY, width, height)

    var hp = 20
    var maxHp = 20
    var defense = 0
    var attackDamage = 4
    var attackCooldown = 0f
    var attackTimer = 0f
    var state: AIState = AIState.IDLE
        protected set

    val isDead: Boolean
        get() = state == AIState.DEATH || hp <= 0

    open val spriteKey: String = "slime"

    open fun takeDamage(amount: Int) {
        if (isDead) return
        hp = (hp - amount).coerceAtLeast(0)
        if (hp <= 0) {
            state = AIState.DEATH
        }
    }

    abstract fun update(
        delta: Float,
        player: PlayerEntity,
        collisionRects: List<Rectangle>,
        worldBounds: Rectangle,
        onPlayerHit: (Int, Float, Float) -> Unit = { _, _, _ -> },
    )

    fun updateCommon(delta: Float) {
        if (attackCooldown > 0f) attackCooldown = maxOf(0f, attackCooldown - delta)
        if (attackTimer > 0f) attackTimer = maxOf(0f, attackTimer - delta)
    }

    protected fun syncBounds() {
        bounds.setPosition(position.x, position.y)
    }
}
