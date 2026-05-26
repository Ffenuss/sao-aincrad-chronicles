package com.sao.aincrad.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.items.ItemType

class PixelSpriteAtlas {
    private val textures = mutableListOf<Texture>()

    private val playerIdle = directionalFromSheet("sprites/player_idle.png")
    private val playerRun = directionalFromSheet("sprites/player_run.png")
    private val playerAttack = directionalFromSheet("sprites/player_attack.png")
    private val playerDodge = directionalFromSheet("sprites/player_dodge.png")
    private val playerDeath = directionalFromSheet("sprites/player_death.png")

    private val slimeIdle = directionalFromSheet("sprites/slime.png")
    private val slimeRun = directionalFromSheet("sprites/slime.png")
    private val slimeAttack = directionalFromSheet("sprites/slime.png")
    private val slimeDeath = directionalFromSheet("sprites/slime.png")

    private val darkElfIdle = directionalFromSheet("sprites/dark_elf.png")
    private val darkElfRun = directionalFromSheet("sprites/dark_elf.png")
    private val darkElfAttack = directionalFromSheet("sprites/dark_elf.png")
    private val darkElfDeath = directionalFromSheet("sprites/dark_elf.png")

    private val skeleton = directionalFromSheet("sprites/skeleton.png")
    private val knight = directionalFromSheet("sprites/knight.png")
    private val boss = directionalFromSheet("sprites/boss.png")

    private val npcDefault = stripFromSheet("sprites/npc_default.png")
    private val npcTrader = stripFromSheet("sprites/npc_trader.png")
    private val npcByKey = mapOf(
        "klein" to stripFromSheet("sprites/npc_klein.png"),
        "asuna" to stripFromSheet("sprites/npc_asuna.png"),
        "agil" to stripFromSheet("sprites/npc_agil.png"),
        "lisbeth" to stripFromSheet("sprites/npc_lisbeth.png"),
        "argo" to stripFromSheet("sprites/npc_argo.png"),
        "kayaba" to stripFromSheet("sprites/npc_kayaba.png"),
        "kizmel" to stripFromSheet("sprites/npc_kizmel.png"),
        "silica" to stripFromSheet("sprites/npc_silica.png"),
        "heathcliff" to stripFromSheet("sprites/npc_heathcliff.png"),
    )

    private val coin = singleFromFile("sprites/coin.png")
    private val potion = singleFromFile("sprites/potion.png")
    private val keyItem = singleFromFile("sprites/key_item.png")

    fun playerFrame(state: PlayerEntity.ActionState, time: Float, facing: PlayerEntity.Facing): TextureRegion {
        return when (state) {
            PlayerEntity.ActionState.IDLE -> playerIdle.frame(time, 0.30f, facing)
            PlayerEntity.ActionState.RUNNING -> playerRun.frame(time, 0.10f, facing)
            PlayerEntity.ActionState.ATTACKING -> playerAttack.frame(time, 0.07f, facing)
            PlayerEntity.ActionState.DODGING -> playerDodge.frame(time, 0.08f, facing)
            PlayerEntity.ActionState.HURT -> playerAttack.frame(time, 0.10f, facing)
            PlayerEntity.ActionState.DEAD -> playerDeath.last(facing)
        }
    }

    fun enemyFrame(enemy: EnemyEntity, time: Float): TextureRegion {
        val idle = when (enemy.spriteKey) {
            "dark_elf" -> darkElfIdle
            "skeleton" -> skeleton
            "knight" -> knight
            "boss" -> boss
            else -> slimeIdle
        }
        val run = when (enemy.spriteKey) {
            "dark_elf" -> darkElfRun
            "skeleton" -> skeleton
            "knight" -> knight
            "boss" -> boss
            else -> slimeRun
        }
        val attack = when (enemy.spriteKey) {
            "dark_elf" -> darkElfAttack
            "skeleton" -> skeleton
            "knight" -> knight
            "boss" -> boss
            else -> slimeAttack
        }
        val death = when (enemy.spriteKey) {
            "dark_elf" -> darkElfDeath
            "skeleton" -> skeleton
            "knight" -> knight
            "boss" -> boss
            else -> slimeDeath
        }

        return when (enemy.state) {
            EnemyEntity.AIState.IDLE -> idle.frame(time, 0.28f, enemy.facing)
            EnemyEntity.AIState.PATROL, EnemyEntity.AIState.CHASE, EnemyEntity.AIState.RETREAT -> run.frame(time, 0.13f, enemy.facing)
            EnemyEntity.AIState.ATTACK -> attack.frame(time, 0.09f, enemy.facing)
            EnemyEntity.AIState.DEATH -> death.last(enemy.facing)
        }
    }

    fun lootFrame(type: ItemType): TextureRegion {
        return when (type) {
            ItemType.CURRENCY -> coin
            ItemType.CONSUMABLE -> potion
            ItemType.KEY_ITEM -> keyItem
            else -> keyItem
        }
    }

    fun npcFrame(npcId: String, role: String, time: Float): TextureRegion {
        val strip = when {
            role == "trader" -> npcTrader
            npcId.contains("asuna", ignoreCase = true) -> npcByKey["asuna"]
            npcId.contains("klein", ignoreCase = true) -> npcByKey["klein"]
            npcId.contains("agil", ignoreCase = true) -> npcByKey["agil"]
            npcId.contains("lisbeth", ignoreCase = true) -> npcByKey["lisbeth"]
            npcId.contains("argo", ignoreCase = true) -> npcByKey["argo"]
            npcId.contains("kayaba", ignoreCase = true) -> npcByKey["kayaba"]
            npcId.contains("kizmel", ignoreCase = true) -> npcByKey["kizmel"]
            npcId.contains("silica", ignoreCase = true) -> npcByKey["silica"]
            npcId.contains("heathcliff", ignoreCase = true) -> npcByKey["heathcliff"]
            else -> npcDefault
        } ?: npcDefault
        val speed = if (role == "trader") 0.40f else 0.55f
        return strip[((time / speed).toInt()).coerceAtLeast(0) % strip.size]
    }

    fun dispose() {
        textures.forEach { it.dispose() }
        textures.clear()
    }

    private fun directionalFromSheet(path: String): DirectionalAnimation {
        val file = Gdx.files.internal(path)
        if (!file.exists()) return fallbackDirectional()

        val texture = Texture(file)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        textures += texture

        val frameWidth = 32
        val frameHeight = 32
        val cols = (texture.width / frameWidth).coerceAtLeast(1)
        val rows = (texture.height / frameHeight).coerceAtLeast(1)

        val rowData = MutableList(8) { mutableListOf<TextureRegion>() }
        for (row in 0 until 8) {
            val srcRow = row.coerceAtMost(rows - 1)
            for (col in 0 until cols) {
                rowData[row] += TextureRegion(texture, col * frameWidth, srcRow * frameHeight, frameWidth, frameHeight)
            }
        }
        return DirectionalAnimation(rowData.map { it.toList() })
    }

    private fun stripFromSheet(path: String): List<TextureRegion> {
        val file = Gdx.files.internal(path)
        if (!file.exists()) return fallbackDirectional().rows.first()

        val texture = Texture(file)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        textures += texture

        val frameWidth = 32
        val frameCount = (texture.width / frameWidth).coerceAtLeast(1)
        return List(frameCount) { index -> TextureRegion(texture, index * frameWidth, 0, frameWidth, 32) }
    }

    private fun singleFromFile(path: String): TextureRegion {
        val file = Gdx.files.internal(path)
        if (!file.exists()) {
            val pm = Pixmap(16, 16, Pixmap.Format.RGBA8888)
            pm.setColor(1f, 1f, 1f, 1f)
            pm.fill()
            val texture = Texture(pm)
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            pm.dispose()
            textures += texture
            return TextureRegion(texture)
        }

        val texture = Texture(file)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        textures += texture
        return TextureRegion(texture)
    }

    private fun fallbackDirectional(): DirectionalAnimation {
        val rows = MutableList(8) { mutableListOf<TextureRegion>() }
        repeat(4) { frame ->
            val pm = Pixmap(32, 32, Pixmap.Format.RGBA8888)
            pm.setColor(0f, 0f, 0f, 0f)
            pm.fill()
            pm.setColor(0.1f, 0.12f, 0.17f, 1f)
            pm.fillRectangle(10, 12, 12, 12)
            pm.setColor(0.8f, 0.68f, 0.52f, 1f)
            pm.fillRectangle(12, 8 + (frame % 2), 8, 5)
            val texture = Texture(pm)
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            pm.dispose()
            textures += texture
            val region = TextureRegion(texture)
            repeat(8) { row -> rows[row] += region }
        }
        return DirectionalAnimation(rows.map { it.toList() })
    }

    private class DirectionalAnimation(val rows: List<List<TextureRegion>>) {
        fun frame(time: Float, frameDuration: Float, facing: PlayerEntity.Facing): TextureRegion {
            val row = rows[facing.ordinal]
            return row[((time / frameDuration).toInt()).coerceAtLeast(0) % row.size]
        }

        fun last(facing: PlayerEntity.Facing): TextureRegion = rows[facing.ordinal].last()
    }
}
