package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import kotlin.math.sqrt

class BossEnemy(
    startX: Float,
    startY: Float,
    private val bossName: String,
    hpValue: Int,
    damage: Int,
) : EnemyEntity(startX, startY, 48f, 48f) {
    override val spriteKey = "boss"
    var phase = 1
        private set
    var shieldTimer = 0f
        private set
    private var patternTimer = 1.2f

    init {
        hp = hpValue
        maxHp = hpValue
        attackDamage = damage
        defense = 8
    }

    override fun takeDamage(amount: Int) {
        if (shieldTimer > 0f) return
        super.takeDamage(amount)
        if (hp <= maxHp / 2 && phase == 1 && !isDead) {
            phase = 2
            shieldTimer = 1.6f
            defense += 4
            attackDamage += 5
        }
    }

    override fun update(
        delta: Float,
        player: PlayerEntity,
        collisionRects: List<Rectangle>,
        worldBounds: Rectangle,
        onPlayerHit: (Int, Float, Float) -> Unit,
    ) {
        updateCommon(delta)
        if (shieldTimer > 0f) shieldTimer = maxOf(0f, shieldTimer - delta)
        if (isDead) {
            velocity.setZero()
            syncBounds()
            return
        }

        val playerCenterX = player.position.x + PlayerEntity.WIDTH * 0.5f
        val playerCenterY = player.position.y + PlayerEntity.HEIGHT * 0.5f
        val enemyCenterX = position.x + bounds.width * 0.5f
        val enemyCenterY = position.y + bounds.height * 0.5f
        val dx = playerCenterX - enemyCenterX
        val dy = playerCenterY - enemyCenterY
        val distance2 = dx * dx + dy * dy
        val attackRange = if (phase == 1) 52f else 82f

        patternTimer -= delta
        state = if (distance2 <= attackRange * attackRange && attackCooldown <= 0f) {
            AIState.ATTACK
        } else {
            AIState.CHASE
        }

        if (state == AIState.ATTACK) {
            velocity.setZero()
            attackCooldown = if (phase == 1) 0.8f else 0.55f
            if (player.takeDamage(attackDamage)) {
                onPlayerHit(attackDamage, player.position.x + PlayerEntity.WIDTH * 0.5f, player.position.y + PlayerEntity.HEIGHT)
            }
            if (patternTimer <= 0f) {
                shieldTimer = if (phase == 2) 0.8f else 0.35f
                patternTimer = if (phase == 2) 2.2f else 3.0f
            }
        } else {
            val speed = if (phase == 1) 58f else 82f
            moveToward(dx, dy, speed)
        }

        updateFacingFromVelocity()
        moveWithCollision(delta, collisionRects, worldBounds)
        syncBounds()
    }

    fun title(): String = "$bossName P$phase"

    private fun moveToward(dx: Float, dy: Float, speed: Float) {
        val length = sqrt(dx * dx + dy * dy)
        if (length > 0.01f) velocity.set(dx / length * speed, dy / length * speed) else velocity.setZero()
    }

    private fun moveWithCollision(delta: Float, collisionRects: List<Rectangle>, worldBounds: Rectangle) {
        val moveX = velocity.x * delta
        val moveY = velocity.y * delta
        if (moveX != 0f) {
            position.x += moveX
            syncBounds()
            if (!within(worldBounds) || collisionRects.any { bounds.overlaps(it) }) {
                position.x -= moveX
                syncBounds()
            }
        }
        if (moveY != 0f) {
            position.y += moveY
            syncBounds()
            if (!within(worldBounds) || collisionRects.any { bounds.overlaps(it) }) {
                position.y -= moveY
                syncBounds()
            }
        }
    }

    private fun within(worldBounds: Rectangle): Boolean {
        return position.x >= worldBounds.x &&
            position.y >= worldBounds.y &&
            position.x + bounds.width <= worldBounds.x + worldBounds.width &&
            position.y + bounds.height <= worldBounds.y + worldBounds.height
    }
}
