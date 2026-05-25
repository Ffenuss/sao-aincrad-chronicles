package com.sao.aincrad.controllers

import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.LootDrop
import com.sao.aincrad.entities.NpcEntity
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.items.Inventory
import com.sao.aincrad.maps.FloorMap
import com.sao.aincrad.ui.DamagePopup

data class UiTransientState(
    var pickupToast: String = "",
    var pickupToastTimer: Float = 0f,
    var dialogSpeaker: String = "",
    var dialogText: String = "",
    var dialogTimer: Float = 0f,
    var inventoryOpen: Boolean = false,
    var storyObjective: String = "",
)

data class WorldState(
    var floor: FloorMap,
    val player: PlayerEntity,
    val inventory: Inventory,
    val enemies: MutableList<EnemyEntity>,
    val npcs: MutableList<NpcEntity>,
    val lootDrops: MutableList<LootDrop>,
    val damagePopups: MutableList<DamagePopup>,
    val ui: UiTransientState = UiTransientState(),
    var elapsedTime: Float = 0f,
)
