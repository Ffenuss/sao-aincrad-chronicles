package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.combat.CombatGeometry

class PlayerEntity(
    startX: Float = 0f,
    startY: Float = 0f,
) {
    enum class Facing {
        UP,
        UP_RIGHT,
        RIGHT,
        DOWN_RIGHT,
        DOWN,
        DOWN_LEFT,
        LEFT,
        UP_LEFT,
    }

    enum class ActionState {
        IDLE, RUNNING, ATTACKING, DODGING, HURT, DEAD
    }

    enum class AttackStyle {
        LIGHT, HEAVY, SPIN
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
    var attackStyle: AttackStyle = AttackStyle.LIGHT
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

    fun basicAttack(multiplier: Float = 1f, style: AttackStyle = AttackStyle.LIGHT) {
        if (!canAttack()) return
        actionState = ActionState.ATTACKING
        attackStyle = style
        when (style) {
            AttackStyle.LIGHT -> {
                attackCooldown = 0.34f
                attackTimer = 0.18f
            }
            AttackStyle.HEAVY -> {
                attackCooldown = 0.52f
                attackTimer = 0.24f
            }
            AttackStyle.SPIN -> {
                attackCooldown = 0.65f
                attackTimer = 0.28f
            }
        }
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
            Facing.UP_RIGHT -> dodgeDirection.set(1f, 1f).nor()
            Facing.DOWN_RIGHT -> dodgeDirection.set(1f, -1f).nor()
            Facing.DOWN_LEFT -> dodgeDirection.set(-1f, -1f).nor()
            Facing.UP_LEFT -> dodgeDirection.set(-1f, 1f).nor()
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

    fun reviveAt(x: Float, y: Float) {
        position.set(x, y)
        syncBounds()
        stats.hp = stats.maxHp
        stats.mp = stats.maxMp
        invulnerableTimer = 1.2f
        attackCooldown = 0f
        attackTimer = 0f
        dodgeCooldown = 0f
        dodgeTimer = 0f
        stopMovement()
        actionState = ActionState.IDLE
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
        val range = when (attackStyle) {
            AttackStyle.LIGHT -> 26f
            AttackStyle.HEAVY -> 34f
            AttackStyle.SPIN -> 30f
        }
        return CombatGeometry.attackHitbox(
            x = bounds.x,
            y = bounds.y,
            width = bounds.width,
            height = bounds.height,
            facing = facing.name,
            range = range,
        )
    }

    fun facingAngleDegrees(): Float {
        return when (facing) {
            Facing.UP -> 90f
            Facing.UP_RIGHT -> 45f
            Facing.RIGHT -> 0f
            Facing.DOWN_RIGHT -> -45f
            Facing.DOWN -> -90f
            Facing.DOWN_LEFT -> -135f
            Facing.LEFT -> 180f
            Facing.UP_LEFT -> 135f
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
        if (kotlin.math.abs(inputX) < 0.20f && kotlin.math.abs(inputY) < 0.20f) return
        val angle = kotlin.math.atan2(inputY.toDouble(), inputX.toDouble()) * (180.0 / Math.PI)
        facing = when {
            angle >= 157.5 || angle < -157.5 -> Facing.LEFT
            angle >= 112.5 -> Facing.UP_LEFT
            angle >= 67.5 -> Facing.UP
            angle >= 22.5 -> Facing.UP_RIGHT
            angle >= -22.5 -> Facing.RIGHT
            angle >= -67.5 -> Facing.DOWN_RIGHT
            angle >= -112.5 -> Facing.DOWN
            else -> Facing.DOWN_LEFT
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
