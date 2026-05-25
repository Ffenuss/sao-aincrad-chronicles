package com.sao.aincrad.ui

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs

class VirtualControls : InputAdapter() {
    data class ActionButton(
        val id: String,
        val label: String,
        val rect: Rectangle = Rectangle(),
        var pressed: Boolean = false,
        var triggered: Boolean = false,
        var pointer: Int = -1,
    )

    private val joystickCenter = Vector2()
    private val joystickKnob = Vector2()
    private val joystickDelta = Vector2()
    private val joystickRadius = 72f

    private var joystickPointer = -1
    private var width = 0f
    private var height = 0f

    val attackButton = ActionButton("attack", "ATTACK")
    val skill1Button = ActionButton("skill1", "SKILL 1")
    val skill2Button = ActionButton("skill2", "SKILL 2")
    val skill3Button = ActionButton("skill3", "SKILL 3")
    val dodgeButton = ActionButton("dodge", "DODGE")
    val inventoryButton = ActionButton("inventory", "BAG")

    private val buttons = listOf(attackButton, skill1Button, skill2Button, skill3Button, dodgeButton, inventoryButton)

    fun resize(width: Int, height: Int) {
        this.width = width.toFloat()
        this.height = height.toFloat()

        joystickCenter.set(this.width * 0.16f, this.height * 0.19f)
        joystickKnob.set(joystickCenter)

        val buttonSize = this.width.coerceAtMost(this.height) * 0.11f
        val padding = buttonSize * 0.35f
        val right = this.width - padding - buttonSize
        val bottom = padding
        val gap = buttonSize + padding * 0.75f

        attackButton.rect.set(right, bottom + gap * 1.6f, buttonSize, buttonSize)
        skill1Button.rect.set(right - gap, bottom + gap, buttonSize, buttonSize)
        skill2Button.rect.set(right, bottom + gap, buttonSize, buttonSize)
        skill3Button.rect.set(right + gap, bottom + gap, buttonSize, buttonSize)
        dodgeButton.rect.set(right - gap * 0.5f, bottom, buttonSize, buttonSize)
        inventoryButton.rect.set(this.width - padding - buttonSize, this.height - padding - buttonSize, buttonSize, buttonSize)
    }

    fun movementVector(out: Vector2 = Vector2()): Vector2 {
        if (joystickPointer == -1) {
            return out.setZero()
        }
        joystickDelta.set(joystickKnob).sub(joystickCenter)
        val len = joystickDelta.len()
        return if (len < 1f) {
            out.setZero()
        } else {
            out.set(joystickDelta.x / joystickRadius, joystickDelta.y / joystickRadius)
                .clamp(-1f, 1f)
        }
    }

    fun consumeAttack(): Boolean = consumeTriggered(attackButton)
    fun consumeSkill1(): Boolean = consumeTriggered(skill1Button)
    fun consumeSkill2(): Boolean = consumeTriggered(skill2Button)
    fun consumeSkill3(): Boolean = consumeTriggered(skill3Button)
    fun consumeDodge(): Boolean = consumeTriggered(dodgeButton)
    fun consumeInventory(): Boolean = consumeTriggered(inventoryButton)

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val uiY = height - screenY.toFloat()
        val x = screenX.toFloat()
        if (isWithinJoystick(x, uiY)) {
            joystickPointer = pointer
            updateJoystick(x, uiY)
            return true
        }
        buttons.firstOrNull { it.rect.contains(x, uiY) }?.let { action ->
            action.pressed = true
            action.triggered = true
            action.pointer = pointer
            return true
        }
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer == joystickPointer) {
            updateJoystick(screenX.toFloat(), height - screenY.toFloat())
            return true
        }
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == joystickPointer) {
            joystickPointer = -1
            joystickKnob.set(joystickCenter)
            return true
        }
        buttons.forEach { action ->
            if (action.pointer == pointer) {
                action.pointer = -1
                action.pressed = false
            }
        }
        return false
    }

    fun update() {
        buttons.forEach { action ->
            if (action.pointer == -1) {
                action.pressed = false
            }
        }
    }

    private fun updateJoystick(x: Float, y: Float) {
        joystickKnob.set(x, y)
        joystickDelta.set(joystickKnob).sub(joystickCenter)
        if (joystickDelta.len() > joystickRadius) {
            joystickDelta.nor().scl(joystickRadius)
            joystickKnob.set(joystickCenter).add(joystickDelta)
        }
    }

    private fun isWithinJoystick(x: Float, y: Float): Boolean {
        val dx = x - joystickCenter.x
        val dy = y - joystickCenter.y
        return dx * dx + dy * dy <= (joystickRadius * 1.35f) * (joystickRadius * 1.35f)
    }

    private fun consumeTriggered(button: ActionButton): Boolean {
        if (!button.triggered) return false
        button.triggered = false
        return true
    }

    fun buttons(): List<ActionButton> = buttons

    fun center(): Vector2 = joystickCenter

    fun knob(): Vector2 = joystickKnob

    fun radius(): Float = joystickRadius
}
