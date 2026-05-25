package com.sao.aincrad.story

data class StoryState(
    var activeQuestId: String = "q_tutorial",
    var chapter: Int = 1,
    var objective: String = "Talk to Klein in the Town of Beginnings",
    val unlockedFloors: MutableSet<Int> = mutableSetOf(1),
    val completedQuestIds: MutableSet<String> = mutableSetOf(),
    val flags: MutableSet<String> = mutableSetOf(),
) {
    fun serialize(): String {
        val floorsRaw = unlockedFloors.sorted().joinToString(",")
        val questsRaw = completedQuestIds.sorted().joinToString(",")
        val flagsRaw = flags.sorted().joinToString(",")
        return listOf(activeQuestId, chapter.toString(), objective, floorsRaw, questsRaw, flagsRaw).joinToString("#")
    }

    fun load(raw: String) {
        if (raw.isBlank()) return
        val parts = raw.split("#")
        activeQuestId = parts.getOrNull(0)?.ifBlank { activeQuestId } ?: activeQuestId
        chapter = parts.getOrNull(1)?.toIntOrNull() ?: chapter
        objective = parts.getOrNull(2)?.ifBlank { objective } ?: objective
        unlockedFloors.clear()
        (parts.getOrNull(3) ?: "").split(",")
            .mapNotNull { it.toIntOrNull() }
            .forEach { unlockedFloors += it }
        if (unlockedFloors.isEmpty()) unlockedFloors += 1
        completedQuestIds.clear()
        (parts.getOrNull(4) ?: "").split(",")
            .filter { it.isNotBlank() }
            .forEach { completedQuestIds += it }
        flags.clear()
        (parts.getOrNull(5) ?: "").split(",")
            .filter { it.isNotBlank() }
            .forEach { flags += it }
    }
}

