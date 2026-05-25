package com.sao.aincrad.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.items.Inventory
import com.sao.aincrad.story.StoryState

data class LoadedGame(
    val floorNumber: Int,
    val position: Vector2,
    val storyStateRaw: String,
)

class SaveManager {
    private val prefs by lazy { Gdx.app.getPreferences("sao_aincrad_save_slots") }

    fun save(slot: Int, floorNumber: Int, player: PlayerEntity, inventory: Inventory, storyState: StoryState) {
        val prefix = "slot_${slot}_"
        prefs.putInteger(prefix + "floor", floorNumber)
        prefs.putFloat(prefix + "x", player.position.x)
        prefs.putFloat(prefix + "y", player.position.y)
        prefs.putInteger(prefix + "level", player.stats.level)
        prefs.putInteger(prefix + "exp", player.stats.exp)
        prefs.putInteger(prefix + "nextExp", player.stats.nextLevelExp)
        prefs.putInteger(prefix + "hp", player.stats.hp)
        prefs.putInteger(prefix + "maxHp", player.stats.maxHp)
        prefs.putInteger(prefix + "mp", player.stats.mp)
        prefs.putInteger(prefix + "maxMp", player.stats.maxMp)
        prefs.putInteger(prefix + "str", player.stats.str)
        prefs.putInteger(prefix + "agi", player.stats.agi)
        prefs.putInteger(prefix + "int", player.stats.intel)
        prefs.putInteger(prefix + "def", player.stats.defense)
        prefs.putInteger(prefix + "gold", player.stats.gold)
        prefs.putInteger(prefix + "statPoints", player.stats.statPoints)
        prefs.putString(prefix + "inventory", inventory.serialize())
        prefs.putString(prefix + "story", storyState.serialize())
        prefs.flush()
    }

    fun load(slot: Int, player: PlayerEntity, inventory: Inventory): LoadedGame? {
        val prefix = "slot_${slot}_"
        if (!prefs.contains(prefix + "floor")) return null

        val position = Vector2(prefs.getFloat(prefix + "x", player.position.x), prefs.getFloat(prefix + "y", player.position.y))
        player.stats.level = prefs.getInteger(prefix + "level", player.stats.level)
        player.stats.exp = prefs.getInteger(prefix + "exp", player.stats.exp)
        player.stats.nextLevelExp = prefs.getInteger(prefix + "nextExp", player.stats.nextLevelExp)
        player.stats.hp = prefs.getInteger(prefix + "hp", player.stats.hp)
        player.stats.maxHp = prefs.getInteger(prefix + "maxHp", player.stats.maxHp)
        player.stats.mp = prefs.getInteger(prefix + "mp", player.stats.mp)
        player.stats.maxMp = prefs.getInteger(prefix + "maxMp", player.stats.maxMp)
        player.stats.str = prefs.getInteger(prefix + "str", player.stats.str)
        player.stats.agi = prefs.getInteger(prefix + "agi", player.stats.agi)
        player.stats.intel = prefs.getInteger(prefix + "int", player.stats.intel)
        player.stats.defense = prefs.getInteger(prefix + "def", player.stats.defense)
        player.stats.gold = prefs.getInteger(prefix + "gold", player.stats.gold)
        player.stats.statPoints = prefs.getInteger(prefix + "statPoints", player.stats.statPoints)
        inventory.load(prefs.getString(prefix + "inventory", ""))
        return LoadedGame(
            floorNumber = prefs.getInteger(prefix + "floor", 1),
            position = position,
            storyStateRaw = prefs.getString(prefix + "story", ""),
        )
    }
}
