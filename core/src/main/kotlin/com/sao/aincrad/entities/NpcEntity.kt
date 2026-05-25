package com.sao.aincrad.entities

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.maps.NpcSpawn

class NpcEntity(spawn: NpcSpawn) {
    val id = spawn.id
    val displayName = spawn.name
    val role = spawn.role
    val dialog = spawn.dialog
    val position = Vector2(spawn.x, spawn.y)
    val bounds = Rectangle(spawn.x, spawn.y, 28f, 32f)
}
