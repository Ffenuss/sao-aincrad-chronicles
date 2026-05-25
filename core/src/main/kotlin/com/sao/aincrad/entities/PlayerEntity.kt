package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

class PlayerEntity(
    startX: Float = 0f,
    startY: Float = 0f,
) {
    enum class Facing {
        UP, DOWN, LEFT, RIGHT
    }

    enum class ActionState {
        IDLE, RUNNING, ATTACKING, DODGING, HURT, DEAD
    }

    data class Stats(
        var level: Int = 1,
        var exp: Int = 0,
        var nextLevelExp: Int = 100,
        var hp: Int = 100,
        var maxHp: Int = 100,
        var mp: Int = 40,
        var maxMp: Int = 40,
        var str: Int = 12,
        var agi: Int = 10,
        var intel: Int = 6,
        var defense: Int = 4,
        var gold: Int = 0,
        var statPoints: Int = 0,
    )

    val position = Vector2(startX, startY)
    val velocity = Vector2()
    val bounds = Rectangle(startX, startY, WIDTH, HEIGHT)
    val stats = Stats()

    var facing: Facing = Facing.DOWN
        private set
    var actionState: ActionState = ActionState.IDLE
        private set

    var attackCooldown = 0f
        private set
    var attackTimer = 0f
        private set
    var attackSequenceId = 0
        private set
    var attackMultiplier = 1f
        private set

    var invulnerableTimer = 0f
        private set

    var dodgeCooldown = 0f
        private set
    var dodgeTimer = 0f
        private set
    private val dodgeDirection = Vector2(0f, 1f)

    var moveSpeed = 120f
    var dodgeSpeed = 240f

    fun update(delta: Float, collisionRects: List<Rectangle> = emptyList(), worldBounds: Rectangle? = null) {
        if (attackCooldown > 0f) attackCooldown = maxOf(0f, attackCooldown - delta)
        if (attackTimer > 0f) attackTimer = maxOf(0f, attackTimer - delta)
        if (invulnerableTimer > 0f) invulnerableTimer = maxOf(0f, invulnerableTimer - delta)
        if (dodgeCooldown > 0f) dodgeCooldown = maxOf(0f, dodgeCooldown - delta)
        if (dodgeTimer > 0f) dodgeTimer = maxOf(0f, dodgeTimer - delta)

        if (dodgeTimer > 0f) {
            velocity.set(dodgeDirection).scl(dodgeSpeed)
        }

        moveWithCollision(delta, collisionRects, worldBounds)
        syncBounds()

        if (stats.hp <= 0) {
            actionState = ActionState.DEAD
        } else if (dodgeTimer > 0f) {
            actionState = ActionState.DODGING
        } else if (attackTimer > 0f) {
            actionState = ActionState.ATTACKING
        } else if (velocity.isZero(0.001f)) {
            actionState = ActionState.IDLE
        } else {
            actionState = ActionState.RUNNING
        }
    }

    fun setMovement(inputX: Float, inputY: Float) {
        velocity.set(inputX, inputY)
        if (velocity.len2() > 1f) {
            velocity.nor()
        }
        velocity.scl(moveSpeed)
        updateFacingFromMovement(inputX, inputY)
    }

    fun stopMovement() {
        velocity.setZero()
    }

    fun canAttack(): Boolean = attackCooldown <= 0f && stats.hp > 0

    fun basicAttack(multiplier: Float = 1f) {
        if (!canAttack()) return
        actionState = ActionState.ATTACKING
        attackCooldown = 0.35f
        attackTimer = 0.18f
        attackMultiplier = multiplier
        attackSequenceId += 1
    }

    fun dodge() {
        if (dodgeCooldown > 0f || stats.hp <= 0) return
        dodgeCooldown = 0.65f
        dodgeTimer = 0.16f
        invulnerableTimer = dodgeTimer
        when (facing) {
            Facing.UP -> dodgeDirection.set(0f, 1f)
            Facing.DOWN -> dodgeDirection.set(0f, -1f)
            Facing.LEFT -> dodgeDirection.set(-1f, 0f)
            Facing.RIGHT -> dodgeDirection.set(1f, 0f)
        }
    }

    fun takeDamage(amount: Int): Boolean {
        if (stats.hp <= 0 || invulnerableTimer > 0f) return false
        stats.hp = (stats.hp - amount).coerceAtLeast(0)
        invulnerableTimer = 0.15f
        if (stats.hp <= 0) {
            actionState = ActionState.DEAD
            stopMovement()
        } else {
            actionState = ActionState.HURT
        }
        return true
    }

    fun heal(amount: Int) {
        stats.hp = (stats.hp + amount).coerceAtMost(stats.maxHp)
    }

    fun gainExp(amount: Int) {
        stats.exp += amount
        while (stats.exp >= stats.nextLevelExp) {
            stats.exp -= stats.nextLevelExp
            levelUp()
        }
    }

    fun attackDamage(multiplier: Float = 1f): Int {
        val raw = stats.str * multiplier
        return maxOf(1, raw.toInt())
    }

    fun allocateStat(stat: String): Boolean {
        if (stats.statPoints <= 0) return false
        when (stat.lowercase()) {
            "str" -> stats.str += 1
            "agi" -> {
                stats.agi += 1
                moveSpeed += 2f
            }
            "int" -> {
                stats.intel += 1
                stats.maxMp += 2
                stats.mp = stats.maxMp
            }
            "def" -> stats.defense += 1
            "hp" -> {
                stats.maxHp += 5
                stats.hp = stats.maxHp
            }
            else -> return false
        }
        stats.statPoints -= 1
        return true
    }

    fun attackHitbox(): Rectangle {
        val range = 14f
        return when (facing) {
            Facing.UP -> Rectangle(bounds.x, bounds.y + bounds.height, bounds.width, range)
            Facing.DOWN -> Rectangle(bounds.x, bounds.y - range, bounds.width, range)
            Facing.LEFT -> Rectangle(bounds.x - range, bounds.y, range, bounds.height)
            Facing.RIGHT -> Rectangle(bounds.x + bounds.width, bounds.y, range, bounds.height)
        }
    }

    private fun levelUp() {
        stats.level += 1
        stats.nextLevelExp = (stats.nextLevelExp * 1.35f).toInt().coerceAtLeast(stats.nextLevelExp + 20)
        stats.maxHp += 12
        stats.maxMp += 6
        stats.hp = stats.maxHp
        stats.mp = stats.maxMp
        stats.str += 2
        stats.agi += 1
        stats.intel += 1
        stats.defense += 1
        stats.statPoints += 3
    }

    private fun updateFacingFromMovement(inputX: Float, inputY: Float) {
        if (kotlin.math.abs(inputX) > kotlin.math.abs(inputY)) {
            facing = if (inputX >= 0f) Facing.RIGHT else Facing.LEFT
        } else if (kotlin.math.abs(inputY) > 0.01f) {
            facing = if (inputY >= 0f) Facing.UP else Facing.DOWN
        }
    }

    private fun clampPositionToWorld(worldBounds: Rectangle?) {
        if (worldBounds == null) {
            position.x = position.x.coerceAtLeast(0f)
            position.y = position.y.coerceAtLeast(0f)
        } else {
            position.x = position.x.coerceIn(worldBounds.x, worldBounds.x + worldBounds.width - bounds.width)
            position.y = position.y.coerceIn(worldBounds.y, worldBounds.y + worldBounds.height - bounds.height)
        }
    }

    private fun moveWithCollision(delta: Float, collisionRects: List<Rectangle>, worldBounds: Rectangle?) {
        if (stats.hp <= 0) return

        val moveX = velocity.x * delta
        val moveY = velocity.y * delta

        if (moveX != 0f) {
            position.x += moveX
            syncBounds()
            if (worldBounds != null && !within(worldBounds)) {
                position.x -= moveX
                syncBounds()
            } else if (collisionRects.any { bounds.overlaps(it) }) {
                position.x -= moveX
                syncBounds()
            }
        }

        if (moveY != 0f) {
            position.y += moveY
            syncBounds()
            if (worldBounds != null && !within(worldBounds)) {
                position.y -= moveY
                syncBounds()
            } else if (collisionRects.any { bounds.overlaps(it) }) {
                position.y -= moveY
                syncBounds()
            }
        }

        clampPositionToWorld(worldBounds)
    }

    private fun syncBounds() {
        bounds.setPosition(position.x, position.y)
    }

    private fun within(worldBounds: Rectangle): Boolean {
        return position.x >= worldBounds.x &&
            position.y >= worldBounds.y &&
            position.x + bounds.width <= worldBounds.x + worldBounds.width &&
            position.y + bounds.height <= worldBounds.y + worldBounds.height
    }

    companion object {
        const val WIDTH = 32f
        const val HEIGHT = 32f
    }
}
