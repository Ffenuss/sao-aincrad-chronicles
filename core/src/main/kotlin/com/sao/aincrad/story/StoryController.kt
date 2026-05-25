package com.sao.aincrad.story

import com.sao.aincrad.controllers.WorldState
import com.sao.aincrad.entities.BossEnemy
import com.sao.aincrad.entities.NpcEntity
import com.sao.aincrad.items.Items

class StoryController {
    val state = StoryState()

    fun objectiveText(): String = state.objective

    fun canEnterFloor(floorNumber: Int): Pair<Boolean, String> {
        if (state.unlockedFloors.contains(floorNumber)) return true to ""
        return false to "Floor $floorNumber is locked. Complete the main objective first."
    }

    fun onFloorLoaded(world: WorldState) {
        if (world.floor.floorNumber >= 25) {
            state.flags += "reached_floor_25"
        }
        if (world.floor.floorNumber >= 50) {
            state.flags += "reached_floor_50"
        }
        if (world.floor.floorNumber >= 75) {
            state.flags += "reached_floor_75"
        }
        if (world.floor.floorNumber >= 100) {
            state.flags += "reached_floor_100"
        }
        refreshObjective(world)
    }

    fun onNpcInteracted(world: WorldState, npc: NpcEntity): String? {
        when (npc.id) {
            "klein_tutorial" -> {
                state.flags += "talked_klein"
                state.unlockedFloors += 25
                completeQuest("q_tutorial")
                state.activeQuestId = "q_dark_elf"
                state.chapter = maxOf(state.chapter, 2)
                refreshObjective(world)
                return "Klein: The route to Floor 25 is now unlocked."
            }
            "dark_elf_scout" -> {
                state.flags += "met_dark_elf_scout"
                if ("boss_floor50_down" !in state.flags) {
                    return "Scout: Defeat the Labyrinth guardian on Floor 50."
                }
            }
            "asuna_75" -> {
                state.flags += "met_asuna"
                if ("boss_floor75_down" !in state.flags) {
                    return "Asuna: Break Granzam's commander to open the Ruby Palace gate."
                }
            }
            "kayaba_echo" -> {
                state.flags += "met_kayaba"
                if ("boss_floor100_down" !in state.flags) {
                    return "Kayaba: This is your final duel."
                }
            }
        }
        refreshObjective(world)
        return null
    }

    fun onBossDefeated(world: WorldState, boss: BossEnemy) {
        when (world.floor.floorNumber) {
            50 -> {
                state.flags += "boss_floor50_down"
                state.unlockedFloors += 75
                completeQuest("q_dark_elf")
                state.activeQuestId = "q_granzam"
                state.chapter = maxOf(state.chapter, 3)
                world.inventory.add(Items.darkRepulser, 1)
            }
            75 -> {
                state.flags += "boss_floor75_down"
                state.unlockedFloors += 100
                completeQuest("q_granzam")
                state.activeQuestId = "q_ruby_palace"
                state.chapter = maxOf(state.chapter, 4)
            }
            100 -> {
                state.flags += "boss_floor100_down"
                completeQuest("q_ruby_palace")
                state.activeQuestId = "q_epilogue"
                state.chapter = maxOf(state.chapter, 5)
            }
        }
        refreshObjective(world)
    }

    private fun refreshObjective(world: WorldState) {
        state.objective = when {
            "boss_floor100_down" in state.flags -> "Aincrad clear condition reached. Return to Town of Beginnings."
            "boss_floor75_down" in state.flags -> "Final Objective: Defeat Heathcliff on Floor 100."
            "boss_floor50_down" in state.flags -> "Objective: Defeat Granzam commander on Floor 75."
            "talked_klein" in state.flags -> "Objective: Push through Floors 25 and 50."
            else -> "Talk to Klein in the Town of Beginnings."
        }
        world.ui.storyObjective = state.objective
    }

    private fun completeQuest(questId: String) {
        state.completedQuestIds += questId
    }
}

