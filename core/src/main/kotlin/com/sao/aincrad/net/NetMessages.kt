package com.sao.aincrad.net

data class PlayerInputCommand(
    val playerId: String,
    val moveX: Float,
    val moveY: Float,
    val attackPressed: Boolean,
    val dodgePressed: Boolean,
    val skillSlot: Int? = null,
)

data class ReplicatedPlayerState(
    val playerId: String,
    val x: Float,
    val y: Float,
    val hp: Int,
    val facing: String,
    val actionState: String = "IDLE",
)

data class ReplicatedEnemyState(
    val enemyId: String,
    val x: Float,
    val y: Float,
    val hp: Int,
    val isDead: Boolean,
)

data class WorldSnapshot(
    val tick: Long,
    val floorNumber: Int,
    val players: List<ReplicatedPlayerState>,
    val enemies: List<ReplicatedEnemyState>,
)

data class GameEventMessage(
    val eventId: String,
    val eventType: String,
    val actorId: String? = null,
    val targetId: String? = null,
    val value: Int? = null,
)

data class AckMessage(
    val ackSequence: Long,
)
