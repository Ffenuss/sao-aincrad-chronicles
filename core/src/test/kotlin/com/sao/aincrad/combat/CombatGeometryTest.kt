package com.sao.aincrad.combat

import kotlin.test.Test
import kotlin.test.assertEquals

class CombatGeometryTest {
    @Test
    fun `attack hitbox uses stable geometry for all directions`() {
        val x = 100f
        val y = 200f
        val width = 32f
        val height = 32f
        val range = 18f

        val up = CombatGeometry.attackHitbox(x, y, width, height, "UP", range)
        assertEquals(100f, up.x)
        assertEquals(232f, up.y)
        assertEquals(32f, up.width)
        assertEquals(18f, up.height)

        val downLeft = CombatGeometry.attackHitbox(x, y, width, height, "DOWN_LEFT", range)
        assertEquals(96f, downLeft.x)
        assertEquals(196f, downLeft.y)
        assertEquals(18f, downLeft.width)
        assertEquals(18f, downLeft.height)

        val right = CombatGeometry.attackHitbox(x, y, width, height, "RIGHT", range)
        assertEquals(132f, right.x)
        assertEquals(200f, right.y)
        assertEquals(18f, right.width)
        assertEquals(32f, right.height)
    }
}
