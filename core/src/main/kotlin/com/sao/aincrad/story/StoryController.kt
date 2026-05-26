package com.sao.aincrad.story

import com.sao.aincrad.controllers.WorldState
import com.sao.aincrad.entities.BossEnemy
import com.sao.aincrad.entities.NpcEntity
import com.sao.aincrad.items.Items

class StoryController {
    private data class StoryNode(
        val floor: Int,
        val objective: String,
        val unlockOnNpc: String? = null,
        val unlockOnBoss: Boolean = false,
        val nextFloor: Int? = null,
    )

    private val route = listOf(1, 2, 10, 22, 25, 35, 47, 50, 55, 67, 74, 75, 90, 100)
    private val nodesByFloor = listOf(
        StoryNode(1, "Meet Klein and prepare for the first boss raid.", unlockOnNpc = "klein_tutorial", nextFloor = 2),
        StoryNode(2, "Defeat the first raid guardian in Tolbana Outskirts.", unlockOnBoss = true, nextFloor = 10),
        StoryNode(10, "Speak with Argo and investigate the Elf Campaign route.", unlockOnNpc = "argo_info", nextFloor = 22),
        StoryNode(22, "Coordinate with Kizmel and secure entry to the Dark Elf Castle.", unlockOnNpc = "kizmel_echo", nextFloor = 25),
        StoryNode(25, "Defeat the Dark Elf Castle guardian.", unlockOnBoss = true, nextFloor = 35),
        StoryNode(35, "Find Silica at the Forest of Wandering and clear the event route.", unlockOnNpc = "silica_35", nextFloor = 47),
        StoryNode(47, "Report to Heathcliff in the Cathedral District.", unlockOnNpc = "heathcliff_47", nextFloor = 50),
        StoryNode(50, "Defeat the Algade Labyrinth boss to open Grandzam.", unlockOnBoss = true, nextFloor = 55),
        StoryNode(55, "Talk to Lisbeth and reinforce your gear for higher floors.", unlockOnNpc = "lisbeth_55", nextFloor = 67),
        StoryNode(67, "Defeat the Ruined Battleground guardian.", unlockOnBoss = true, nextFloor = 74),
        StoryNode(74, "Defeat the Crimson Approach gatekeeper.", unlockOnBoss = true, nextFloor = 75),
        StoryNode(75, "Defeat the Granzam commander and assemble the final party.", unlockOnBoss = true, nextFloor = 90),
        StoryNode(90, "Defeat the Sky Bridge guardian.", unlockOnBoss = true, nextFloor = 100),
        StoryNode(100, "Final Duel: defeat Heathcliff at the Ruby Palace.", unlockOnBoss = true, nextFloor = null),
    ).associateBy { it.floor }

    val state = StoryState()

    init {
        state.unlockedFloors.clear()
        state.unlockedFloors += 1
        state.activeQuestId = "q_floor_1"
        state.objective = nodesByFloor[1]?.objective ?: state.objective
    }

    fun objectiveText(): String = state.objective

    fun canEnterFloor(floorNumber: Int): Pair<Boolean, String> {
        if (state.unlockedFloors.contains(floorNumber)) return true to ""
        return false to "Floor $floorNumber is locked. Progress the main objective first."
    }

    fun onFloorLoaded(world: WorldState) {
        state.flags += "reached_floor_${world.floor.floorNumber}"
        refreshObjective(world)
    }

    fun onNpcInteracted(world: WorldState, npc: NpcEntity): String? {
        state.flags += "npc_${npc.id}_met"

        val floor = world.floor.floorNumber
        val node = nodesByFloor[floor]
        if (node != null && node.unlockOnNpc == npc.id && node.nextFloor != null) {
            unlockNextFloor(world, node.nextFloor, "npc_${npc.id}_unlock")
            return "${npc.displayName}: Route to Floor ${node.nextFloor} unlocked."
        }

        if (npc.role == "trader") {
            world.inventory.add(Items.hpPotion, 1)
            return "${npc.dialog} Bonus: +1 HP Potion."
        }

        refreshObjective(world)
        return null
    }

    fun onBossDefeated(world: WorldState, boss: BossEnemy) {
        val floor = world.floor.floorNumber
        state.flags += "boss_floor_${floor}_down"

        val node = nodesByFloor[floor]
        if (node?.unlockOnBoss == true && node.nextFloor != null) {
            unlockNextFloor(world, node.nextFloor, "boss_floor_${floor}_unlock")
        }

        when (floor) {
            2 -> world.inventory.add(Items.annealBlade, 1)
            25 -> world.inventory.add(Items.darkRepulser, 1)
            50 -> world.inventory.add(Items.blackwyrmCoat, 1)
            75 -> world.inventory.add(Items.floorKeyShard, 1)
            100 -> {
                state.flags += "aincrad_cleared"
                state.chapter = 8
                state.activeQuestId = "q_epilogue"
            }
        }

        refreshObjective(world)
    }

    private fun unlockNextFloor(world: WorldState, nextFloor: Int, flag: String) {
        if (flag in state.flags) return
        state.flags += flag
        state.unlockedFloors += nextFloor
        state.chapter = maxOf(state.chapter, route.indexOf(nextFloor) + 1)
        completeQuest("q_floor_${world.floor.floorNumber}")
        state.activeQuestId = "q_floor_$nextFloor"
        world.ui.pickupToast = "New floor unlocked: $nextFloor"
        world.ui.pickupToastTimer = 1.8f
        refreshObjective(world)
    }

    private fun refreshObjective(world: WorldState) {
        if ("aincrad_cleared" in state.flags) {
            state.objective = "Aincrad clear condition reached. Return to Floor 1 for epilogue dialogue."
            world.ui.storyObjective = state.objective
            return
        }

        val floor = world.floor.floorNumber
        val node = nodesByFloor[floor]
        state.objective = if (node != null) {
            when {
                node.unlockOnBoss && "boss_floor_${floor}_down" !in state.flags -> node.objective
                node.unlockOnNpc != null && "npc_${node.unlockOnNpc}_met" !in state.flags -> node.objective
                node.nextFloor != null && node.nextFloor !in state.unlockedFloors -> "Unlock Floor ${node.nextFloor}."
                node.nextFloor != null -> "Proceed to Floor ${node.nextFloor}."
                else -> "Clear condition reached."
            }
        } else {
            "Explore Aincrad and follow your frontline allies."
        }
        world.ui.storyObjective = state.objective
    }

    private fun completeQuest(questId: String) {
        state.completedQuestIds += questId
    }
}
