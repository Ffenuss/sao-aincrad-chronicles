package com.sao.aincrad.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.items.ItemType

class PixelSpriteAtlas {
    private val textures = mutableListOf<Texture>()

    val playerIdle = framesFromSheet("sprites/player_idle.png") { frames(::drawKiritoIdle) }
    val playerRun = framesFromSheet("sprites/player_run.png") { frames(::drawKiritoRun) }
    val playerAttack = framesFromSheet("sprites/player_attack.png") { frames(::drawKiritoAttack) }
    val playerDodge = framesFromSheet("sprites/player_dodge.png") { frames(::drawKiritoDodge) }
    val playerDeath = framesFromSheet("sprites/player_death.png") { frames(::drawKiritoDeath) }

    val slimeIdle = framesFromSheet("sprites/slime.png") { frames(::drawSlimeIdle) }
    val slimeRun = framesFromSheet("sprites/slime.png") { frames(::drawSlimeRun) }
    val slimeAttack = framesFromSheet("sprites/slime.png") { frames(::drawSlimeAttack) }
    val slimeDeath = framesFromSheet("sprites/slime.png") { frames(::drawSlimeDeath) }

    val darkElfIdle = framesFromSheet("sprites/dark_elf.png") { frames(::drawDarkElfIdle) }
    val darkElfRun = framesFromSheet("sprites/dark_elf.png") { frames(::drawDarkElfRun) }
    val darkElfAttack = framesFromSheet("sprites/dark_elf.png") { frames(::drawDarkElfAttack) }
    val darkElfDeath = framesFromSheet("sprites/dark_elf.png") { frames(::drawDarkElfDeath) }
    val skeleton = framesFromSheet("sprites/skeleton.png") { frames(::drawDarkElfIdle) }
    val knight = framesFromSheet("sprites/knight.png") { frames(::drawDarkElfIdle) }
    val npc = framesFromSheet("sprites/npc.png") { frames(::drawNpc) }
    val boss = framesFromSheet("sprites/boss.png") { frames(::drawBoss) }

    val coin = singleFromFile("sprites/coin.png") { single(::drawCoin) }
    val potion = singleFromFile("sprites/potion.png") { single(::drawPotion) }
    val keyItem = singleFromFile("sprites/key_item.png") { single(::drawKeyShard) }

    fun playerFrame(state: PlayerEntity.ActionState, time: Float): TextureRegion {
        return when (state) {
            PlayerEntity.ActionState.IDLE -> playerIdle.frame(time, 0.45f)
            PlayerEntity.ActionState.RUNNING -> playerRun.frame(time, 0.12f)
            PlayerEntity.ActionState.ATTACKING -> playerAttack.frame(time, 0.08f)
            PlayerEntity.ActionState.DODGING -> playerDodge.frame(time, 0.08f)
            PlayerEntity.ActionState.HURT -> playerAttack.frame(time, 0.10f)
            PlayerEntity.ActionState.DEAD -> playerDeath.last()
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
            EnemyEntity.AIState.IDLE -> idle.frame(time, 0.35f)
            EnemyEntity.AIState.PATROL, EnemyEntity.AIState.CHASE, EnemyEntity.AIState.RETREAT -> run.frame(time, 0.16f)
            EnemyEntity.AIState.ATTACK -> attack.frame(time, 0.10f)
            EnemyEntity.AIState.DEATH -> death.last()
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

    fun npcFrame(role: String, time: Float): TextureRegion {
        return npc.frame(time, if (role == "trader") 0.4f else 0.55f)
    }

    fun dispose() {
        textures.forEach { it.dispose() }
        textures.clear()
    }

    private fun framesFromSheet(path: String, fallback: () -> List<TextureRegion>): List<TextureRegion> {
        val file = Gdx.files.internal(path)
        if (!file.exists()) return fallback()
        val texture = Texture(file)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        textures += texture
        val frameWidth = 32
        val frameCount = (texture.width / frameWidth).coerceAtLeast(1)
        return List(frameCount) { index -> TextureRegion(texture, index * frameWidth, 0, frameWidth, 32) }
    }

    private fun singleFromFile(path: String, fallback: () -> TextureRegion): TextureRegion {
        val file = Gdx.files.internal(path)
        if (!file.exists()) return fallback()
        val texture = Texture(file)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        textures += texture
        return TextureRegion(texture)
    }

    private fun frames(draw: (Pixmap, Int) -> Unit): List<TextureRegion> {
        return List(4) { index ->
            val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
            pixmap.setBlending(Pixmap.Blending.None)
            pixmap.setColor(0f, 0f, 0f, 0f)
            pixmap.fill()
            pixmap.setBlending(Pixmap.Blending.SourceOver)
            draw(pixmap, index)
            regionFrom(pixmap)
        }
    }

    private fun single(draw: (Pixmap) -> Unit): TextureRegion {
        val pixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
        pixmap.setBlending(Pixmap.Blending.None)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        pixmap.setBlending(Pixmap.Blending.SourceOver)
        draw(pixmap)
        return regionFrom(pixmap)
    }

    private fun regionFrom(pixmap: Pixmap): TextureRegion {
        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pixmap.dispose()
        textures += texture
        return TextureRegion(texture)
    }

    private fun drawKiritoIdle(p: Pixmap, frame: Int) {
        drawKiritoBase(p, if (frame % 2 == 0) 0 else 1)
    }

    private fun drawKiritoRun(p: Pixmap, frame: Int) {
        drawKiritoBase(p, if (frame % 2 == 0) -1 else 1)
        p.set(Color(0.04f, 0.04f, 0.05f, 1f))
        p.fillRectangle(10 + frame % 2, 23, 4, 6)
        p.fillRectangle(18 - frame % 2, 23, 4, 6)
    }

    private fun drawKiritoAttack(p: Pixmap, frame: Int) {
        drawKiritoBase(p, 0)
        p.set(Color(0.78f, 0.86f, 0.95f, 1f))
        p.fillRectangle(21, 10 + frame, 9, 2)
        p.fillRectangle(28, 8 + frame, 2, 6)
    }

    private fun drawKiritoDodge(p: Pixmap, frame: Int) {
        drawKiritoBase(p, 0)
        p.set(Color(0.35f, 0.42f, 0.52f, 0.55f))
        p.fillRectangle(4 + frame, 24, 18, 2)
    }

    private fun drawKiritoDeath(p: Pixmap, frame: Int) {
        p.set(Color(0.04f, 0.04f, 0.05f, 1f))
        p.fillRectangle(8, 22, 17 + frame, 5)
        p.set(Color(0.12f, 0.12f, 0.16f, 1f))
        p.fillRectangle(12, 18, 10, 4)
    }

    private fun drawKiritoBase(p: Pixmap, offset: Int) {
        p.set(Color(0.05f, 0.05f, 0.07f, 1f))
        p.fillRectangle(10, 12 + offset, 12, 13)
        p.set(Color(0.12f, 0.12f, 0.16f, 1f))
        p.fillRectangle(8, 11 + offset, 4, 14)
        p.fillRectangle(20, 11 + offset, 4, 14)
        p.set(Color(0.86f, 0.68f, 0.52f, 1f))
        p.fillRectangle(12, 6 + offset, 8, 7)
        p.set(Color(0.02f, 0.02f, 0.03f, 1f))
        p.fillRectangle(10, 4 + offset, 12, 5)
        p.set(Color(0.75f, 0.82f, 0.9f, 1f))
        p.fillRectangle(7, 8 + offset, 2, 13)
        p.fillRectangle(23, 8 + offset, 2, 13)
    }

    private fun drawSlimeIdle(p: Pixmap, frame: Int) {
        drawSlimeBase(p, if (frame % 2 == 0) 0 else 1)
    }

    private fun drawSlimeRun(p: Pixmap, frame: Int) {
        drawSlimeBase(p, frame % 2)
        p.set(Color(0.12f, 0.45f, 0.15f, 1f))
        p.fillRectangle(9, 25, 14, 2)
    }

    private fun drawSlimeAttack(p: Pixmap, frame: Int) {
        drawSlimeBase(p, -frame)
        p.set(Color(0.76f, 1f, 0.52f, 0.75f))
        p.drawRectangle(6 - frame, 11 - frame, 20 + frame * 2, 16 + frame)
    }

    private fun drawSlimeDeath(p: Pixmap, frame: Int) {
        p.set(Color(0.16f, 0.45f, 0.17f, 0.75f))
        p.fillRectangle(8, 23, 16 + frame, 4)
    }

    private fun drawSlimeBase(p: Pixmap, offset: Int) {
        p.set(Color(0.18f, 0.64f, 0.22f, 1f))
        p.fillRectangle(7, 13 + offset, 18, 12)
        p.fillRectangle(10, 9 + offset, 12, 5)
        p.set(Color(0.45f, 0.95f, 0.38f, 1f))
        p.fillRectangle(11, 11 + offset, 5, 3)
        p.set(Color(0.02f, 0.08f, 0.03f, 1f))
        p.fillRectangle(13, 16 + offset, 2, 2)
        p.fillRectangle(19, 16 + offset, 2, 2)
    }

    private fun drawDarkElfIdle(p: Pixmap, frame: Int) {
        drawDarkElfBase(p, if (frame % 2 == 0) 0 else 1)
    }

    private fun drawDarkElfRun(p: Pixmap, frame: Int) {
        drawDarkElfBase(p, if (frame % 2 == 0) -1 else 1)
        p.set(Color(0.08f, 0.22f, 0.14f, 1f))
        p.fillRectangle(9 + frame % 2, 24, 4, 5)
        p.fillRectangle(19 - frame % 2, 24, 4, 5)
    }

    private fun drawDarkElfAttack(p: Pixmap, frame: Int) {
        drawDarkElfBase(p, 0)
        p.set(Color(0.55f, 0.95f, 0.72f, 0.9f))
        p.drawLine(22, 11, 30, 7 + frame)
        p.drawLine(22, 12, 30, 12 + frame)
    }

    private fun drawDarkElfDeath(p: Pixmap, frame: Int) {
        p.set(Color(0.08f, 0.18f, 0.11f, 1f))
        p.fillRectangle(8, 22, 18 + frame, 4)
        p.set(Color(0.18f, 0.32f, 0.22f, 1f))
        p.fillRectangle(12, 18, 10, 4)
    }

    private fun drawDarkElfBase(p: Pixmap, offset: Int) {
        p.set(Color(0.08f, 0.22f, 0.14f, 1f))
        p.fillRectangle(10, 12 + offset, 12, 14)
        p.set(Color(0.16f, 0.34f, 0.20f, 1f))
        p.fillRectangle(8, 11 + offset, 4, 13)
        p.fillRectangle(20, 11 + offset, 4, 13)
        p.set(Color(0.62f, 0.48f, 0.42f, 1f))
        p.fillRectangle(12, 6 + offset, 8, 7)
        p.set(Color(0.10f, 0.10f, 0.08f, 1f))
        p.fillRectangle(10, 4 + offset, 12, 5)
        p.set(Color(0.55f, 0.95f, 0.72f, 1f))
        p.fillRectangle(22, 8 + offset, 2, 13)
    }

    private fun drawNpc(p: Pixmap, frame: Int) {
        val offset = if (frame % 2 == 0) 0 else 1
        p.set(Color(0.20f, 0.18f, 0.14f, 1f))
        p.fillRectangle(10, 13 + offset, 12, 13)
        p.set(Color(0.82f, 0.62f, 0.45f, 1f))
        p.fillRectangle(12, 7 + offset, 8, 7)
        p.set(Color(0.45f, 0.12f, 0.12f, 1f))
        p.fillRectangle(9, 4 + offset, 14, 5)
        p.set(Color(0.95f, 0.78f, 0.28f, 1f))
        p.fillRectangle(8, 14 + offset, 3, 8)
    }

    private fun drawBoss(p: Pixmap, frame: Int) {
        val offset = if (frame % 2 == 0) 0 else 1
        p.set(Color(0.46f, 0.47f, 0.52f, 1f))
        p.fillRectangle(7, 10 + offset, 18, 17)
        p.set(Color(0.78f, 0.12f, 0.16f, 1f))
        p.fillRectangle(10, 5 + offset, 12, 6)
        p.set(Color(0.85f, 0.88f, 0.95f, 1f))
        p.fillRectangle(4, 12 + offset, 6, 12)
        p.set(Color(0.92f, 0.92f, 1f, 1f))
        p.fillRectangle(22, 8 + offset, 3, 18)
        p.set(Color(1f, 0.95f, 0.35f, 1f))
        p.drawRectangle(5, 13 + offset, 4, 10)
    }

    private fun drawCoin(p: Pixmap) {
        p.set(Color(0.98f, 0.78f, 0.18f, 1f))
        p.fillCircle(8, 8, 6)
        p.set(Color(1f, 0.96f, 0.45f, 1f))
        p.drawCircle(8, 8, 5)
        p.drawLine(8, 4, 8, 12)
    }

    private fun drawPotion(p: Pixmap) {
        p.set(Color(0.76f, 0.12f, 0.18f, 1f))
        p.fillRectangle(5, 6, 7, 7)
        p.set(Color(0.92f, 0.86f, 0.78f, 1f))
        p.fillRectangle(6, 3, 5, 3)
    }

    private fun drawKeyShard(p: Pixmap) {
        p.set(Color(0.65f, 0.9f, 1f, 1f))
        p.fillTriangle(8, 2, 13, 12, 3, 12)
        p.set(Color(1f, 1f, 1f, 0.75f))
        p.drawLine(8, 4, 10, 10)
    }

    private fun Pixmap.set(color: Color) {
        setColor(color)
    }

    private fun List<TextureRegion>.frame(time: Float, frameDuration: Float): TextureRegion {
        return this[((time / frameDuration).toInt()).coerceAtLeast(0) % size]
    }
}
