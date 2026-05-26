package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import kotlin.math.sqrt

class ConfigEnemy(
    startX: Float,
    startY: Float,
    override val spriteKey: String,
    hpValue: Int,
    private val chaseRange: Float,
    private val attackRange: Float,
    private val speed: Float,
    damage: Int,
    defenseValue: Int,
) : EnemyEntity(startX, startY, 30f, 30f) {
    private var patrolDirection = 1f
    private var patrolTimer = 1.4f
    private var retreatTimer = 0f

    init {
        hp = hpValue
        maxHp = hpValue
        attackDamage = damage
        defense = defenseValue
    }

    override fun update(
        delta: Float,
        player: PlayerEntity,
        collisionRects: List<Rectangle>,
        worldBounds: Rectangle,
        onPlayerHit: (Int, Float, Float) -> Unit,
    ) {
        updateCommon(delta)
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
        val chaseRange2 = chaseRange * chaseRange
        val attackRange2 = attackRange * attackRange

        when (state) {
            AIState.IDLE -> {
                velocity.setZero()
                patrolTimer -= delta
                if (distance2 <= chaseRange2) state = AIState.CHASE
                if (patrolTimer <= 0f) {
                    state = AIState.PATROL
                    patrolTimer = 1.7f
                    patrolDirection *= -1f
                }
            }
            AIState.PATROL -> {
                velocity.set(patrolDirection * speed * 0.45f, 0f)
                patrolTimer -= delta
                if (distance2 <= chaseRange2) state = AIState.CHASE
                if (patrolTimer <= 0f) state = AIState.IDLE
            }
            AIState.CHASE -> {
                if (distance2 <= attackRange2 && attackCooldown <= 0f) {
                    state = AIState.ATTACK
                    attackTimer = 0.32f
                    attackCooldown = 0.95f
                    velocity.setZero()
                } else if (distance2 > chaseRange2 * 1.35f) {
                    state = AIState.RETREAT
                    retreatTimer = 0.35f
                } else {
                    moveToward(dx, dy, speed)
                }
            }
            AIState.ATTACK -> {
                velocity.setZero()
                if (attackTimer <= 0f) {
                    if (distance2 <= attackRange2 * 1.25f && player.takeDamage(attackDamage)) {
                        onPlayerHit(attackDamage, player.position.x + PlayerEntity.WIDTH * 0.5f, player.position.y + PlayerEntity.HEIGHT)
                    }
                    state = AIState.RETREAT
                    retreatTimer = 0.35f
                }
            }
            AIState.RETREAT -> {
                retreatTimer -= delta
                moveToward(-dx, -dy, speed * 0.6f)
                if (retreatTimer <= 0f) state = if (distance2 <= chaseRange2) AIState.CHASE else AIState.IDLE
            }
            AIState.DEATH -> velocity.setZero()
        }

        updateFacingFromVelocity()
        moveWithCollision(delta, collisionRects, worldBounds)
        syncBounds()
    }

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
