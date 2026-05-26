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
    var facing: PlayerEntity.Facing = PlayerEntity.Facing.DOWN
        protected set
    var state: AIState = AIState.IDLE
        protected set
    var networkId: String = ""

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

    fun applyReplicatedState(x: Float, y: Float, hpValue: Int, dead: Boolean) {
        position.set(x, y)
        hp = hpValue.coerceAtLeast(0)
        state = if (dead || hp <= 0) {
            AIState.DEATH
        } else if (state == AIState.DEATH) {
            AIState.IDLE
        } else {
            state
        }
        syncBounds()
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

    protected fun updateFacingFromVelocity() {
        val vx = velocity.x
        val vy = velocity.y
        if (kotlin.math.abs(vx) < 0.01f && kotlin.math.abs(vy) < 0.01f) return
        val angle = kotlin.math.atan2(vy.toDouble(), vx.toDouble()) * (180.0 / Math.PI)
        facing = when {
            angle >= 157.5 || angle < -157.5 -> PlayerEntity.Facing.LEFT
            angle >= 112.5 -> PlayerEntity.Facing.UP_LEFT
            angle >= 67.5 -> PlayerEntity.Facing.UP
            angle >= 22.5 -> PlayerEntity.Facing.UP_RIGHT
            angle >= -22.5 -> PlayerEntity.Facing.RIGHT
            angle >= -67.5 -> PlayerEntity.Facing.DOWN_RIGHT
            angle >= -112.5 -> PlayerEntity.Facing.DOWN
            else -> PlayerEntity.Facing.DOWN_LEFT
        }
    }

    protected fun syncBounds() {
        bounds.setPosition(position.x, position.y)
    }
}
