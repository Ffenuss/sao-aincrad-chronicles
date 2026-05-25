package com.sao.aincrad.controllers

import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.items.Inventory
import com.sao.aincrad.items.ItemType
import com.sao.aincrad.items.Items
import com.sao.aincrad.maps.FloorMap
import com.sao.aincrad.story.StoryController
import com.sao.aincrad.utils.AudioManager
import com.sao.aincrad.utils.SaveManager

class WorldController(
    private val audioManager: AudioManager,
    private val saveManager: SaveManager,
) {
    val state: WorldState

    private val floorController = FloorController(audioManager, saveManager)
    private val storyController = StoryController()
    private val combatController = CombatController(audioManager, saveManager, storyController)

    init {
        val floor = FloorMap.load(1)
        val player = PlayerEntity(floor.playerSpawn.x, floor.playerSpawn.y)
        val inventory = Inventory()
        seedStarterInventory(inventory)
        state = WorldState(
            floor = floor,
            player = player,
            inventory = inventory,
            enemies = mutableListOf(),
            npcs = mutableListOf(),
            lootDrops = mutableListOf(),
            damagePopups = mutableListOf(),
        )
        floorController.bootstrap(state)
        storyController.onFloorLoaded(state)
    }

    fun dispose() {
        state.floor.dispose()
    }

    fun toggleInventory() {
        state.ui.inventoryOpen = !state.ui.inventoryOpen
    }

    fun setMovement(x: Float, y: Float) {
        state.player.setMovement(x, y)
    }

    fun basicAttack(multiplier: Float = 1f) {
        state.player.basicAttack(multiplier)
        audioManager.playSfx("sword")
    }

    fun dodge() {
        state.player.dodge()
    }

    fun allocateStat(stat: String) {
        state.player.allocateStat(stat)
    }

    fun equipFirstAvailable() {
        val equipped = state.inventory.allSlots().indices.firstOrNull { state.inventory.equipFromSlot(it) } != null
        state.ui.pickupToast = if (equipped) "Equipment updated" else "No equippable item"
        state.ui.pickupToastTimer = 1.4f
    }

    fun interactWithNearestNpc() {
        val npc = state.npcs.minByOrNull { it.position.dst2(state.player.position) } ?: return
        if (npc.position.dst2(state.player.position) > 80f * 80f) return
        state.ui.dialogSpeaker = npc.displayName
        val storyDialog = storyController.onNpcInteracted(state, npc)
        state.ui.dialogText = if (storyDialog != null) {
            storyDialog
        } else if (npc.role == "trader") {
            state.inventory.add(Items.hpPotion, 1)
            "${npc.dialog} Trader bonus: +1 HP Potion."
        } else {
            npc.dialog
        }
        state.ui.dialogTimer = 5f
    }

    fun saveGame(slot: Int = 1, showToast: Boolean = true) {
        saveManager.save(slot, state.floor.floorNumber, state.player, state.inventory, storyController.state)
        if (showToast) {
            state.ui.pickupToast = "Saved to slot $slot"
            state.ui.pickupToastTimer = 1.2f
        }
    }

    fun loadGame(slot: Int = 1) {
        val loaded = saveManager.load(slot, state.player, state.inventory) ?: run {
            state.ui.pickupToast = "No save slot"
            state.ui.pickupToastTimer = 1.2f
            return
        }
        loadFloor(loaded.floorNumber, autoSave = false, enforceAccess = false)
        state.player.position.set(loaded.position)
        state.player.bounds.setPosition(state.player.position)
        storyController.state.load(loaded.storyStateRaw)
        state.ui.storyObjective = storyController.objectiveText()
        state.ui.pickupToast = "Loaded"
        state.ui.pickupToastTimer = 1.2f
    }

    fun loadFloor(floorNumber: Int, autoSave: Boolean = true, enforceAccess: Boolean = true) {
        if (enforceAccess && floorNumber != state.floor.floorNumber) {
            val access = storyController.canEnterFloor(floorNumber)
            if (!access.first) {
                state.ui.pickupToast = access.second
                state.ui.pickupToastTimer = 1.4f
                return
            }
        }
        floorController.loadFloor(state, floorNumber, autoSave)
        storyController.onFloorLoaded(state)
        combatController.resetAttackSequence()
        if (autoSave) {
            saveManager.save(1, state.floor.floorNumber, state.player, state.inventory, storyController.state)
        }
    }

    fun useFirstConsumable() {
        val index = state.inventory.allSlots().indexOfFirst { it?.item?.type == ItemType.CONSUMABLE && (it.quantity > 0) }
        if (index < 0) {
            state.ui.pickupToast = "No consumables"
            state.ui.pickupToastTimer = 1.2f
            return
        }
        val stack = state.inventory.allSlots()[index] ?: return
        when (stack.item.id) {
            Items.hpPotion.id -> {
                state.player.heal(45)
                state.ui.pickupToast = "Used HP Potion"
            }
            else -> {
                state.ui.pickupToast = "Cannot use ${stack.item.name}"
                state.ui.pickupToastTimer = 1.2f
                return
            }
        }
        stack.quantity -= 1
        if (stack.quantity <= 0) {
            state.inventory.removeAt(index)
        }
        state.ui.pickupToastTimer = 1.2f
    }

    fun update(delta: Float) {
        state.elapsedTime += delta
        state.player.update(delta, state.floor.collisionRects, state.floor.worldBounds)
        combatController.update(state, delta, floorController.currentSpawnSystem())
        maybeTransitionFloor()

        if (state.ui.pickupToastTimer > 0f) state.ui.pickupToastTimer = maxOf(0f, state.ui.pickupToastTimer - delta)
        if (state.ui.dialogTimer > 0f) state.ui.dialogTimer = maxOf(0f, state.ui.dialogTimer - delta)
    }

    private fun maybeTransitionFloor() {
        val exit = state.floor.exitZone ?: return
        if (!state.player.bounds.overlaps(exit)) return
        val nextFloor = floorController.nextFloorFor(state.floor.floorNumber)
        val access = storyController.canEnterFloor(nextFloor)
        if (!access.first) {
            state.ui.pickupToast = access.second
            state.ui.pickupToastTimer = 1.4f
            return
        }
        loadFloor(nextFloor)
    }

    private fun seedStarterInventory(inventory: Inventory) {
        inventory.add(Items.hpPotion, 3)
        inventory.add(Items.annealBlade, 1)
        inventory.add(Items.blackwyrmCoat, 1)
        inventory.add(Items.teleportCrystal, 1)
        inventory.equip("anneal_blade")
    }
}
