package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.items.Item

data class LootDrop(
    val item: Item,
    var quantity: Int,
    val position: Vector2,
) {
    val bounds = Rectangle(position.x, position.y, 18f, 18f)
    var life = 30f

    fun update(delta: Float) {
        life -= delta
        bounds.setPosition(position.x, position.y)
    }
}
