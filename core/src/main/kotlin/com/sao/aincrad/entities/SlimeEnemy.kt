package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import kotlin.math.sqrt

class SlimeEnemy(
    startX: Float,
    startY: Float,
) : EnemyEntity(startX, startY, 28f, 24f) {
    private var patrolDirection = 1f
    private var patrolTimer = 1.5f
    private var retreatTimer = 0f

    init {
        hp = 18
        maxHp = 18
        defense = 1
        attackDamage = 4
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
        val chaseRange2 = 170f * 170f
        val attackRange2 = 24f * 24f

        when (state) {
            AIState.IDLE -> {
                velocity.setZero()
                patrolTimer -= delta
                if (distance2 <= chaseRange2) {
                    state = AIState.CHASE
                } else if (patrolTimer <= 0f) {
                    state = AIState.PATROL
                    patrolTimer = 2.0f
                    patrolDirection *= -1f
                }
            }

            AIState.PATROL -> {
                velocity.set(patrolDirection * 25f, 0f)
                patrolTimer -= delta
                if (distance2 <= chaseRange2) {
                    state = AIState.CHASE
                } else if (patrolTimer <= 0f) {
                    state = AIState.IDLE
                    patrolTimer = 1.5f
                }
            }

            AIState.CHASE -> {
                if (distance2 <= attackRange2 && attackCooldown <= 0f) {
                    state = AIState.ATTACK
                    attackTimer = 0.25f
                    attackCooldown = 0.85f
                    velocity.setZero()
                } else if (distance2 > chaseRange2 * 1.35f) {
                    state = AIState.RETREAT
                    retreatTimer = 0.45f
                } else {
                    val length = sqrt(distance2)
                    if (length > 0.01f) {
                        velocity.set(dx / length * 52f, dy / length * 52f)
                    } else {
                        velocity.setZero()
                    }
                }
            }

            AIState.ATTACK -> {
                velocity.setZero()
                if (attackTimer <= 0f) {
                    if (distance2 <= attackRange2 * 1.4f) {
                        if (player.takeDamage(attackDamage)) {
                            onPlayerHit(
                                attackDamage,
                                player.position.x + PlayerEntity.WIDTH * 0.5f,
                                player.position.y + PlayerEntity.HEIGHT,
                            )
                        }
                    }
                    state = AIState.RETREAT
                    retreatTimer = 0.3f
                }
            }

            AIState.RETREAT -> {
                retreatTimer -= delta
                val length = sqrt(distance2)
                if (length > 0.01f) {
                    velocity.set(-dx / length * 36f, -dy / length * 36f)
                } else {
                    velocity.setZero()
                }
                if (retreatTimer <= 0f) {
                    state = if (distance2 <= chaseRange2) AIState.CHASE else AIState.IDLE
                }
            }

            AIState.DEATH -> {
                velocity.setZero()
            }
        }

        updateFacingFromVelocity()
        moveWithCollision(delta, collisionRects, worldBounds)
        syncBounds()
    }

    private fun moveWithCollision(
        delta: Float,
        collisionRects: List<Rectangle>,
        worldBounds: Rectangle,
    ) {
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
