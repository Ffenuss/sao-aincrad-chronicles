package com.sao.aincrad.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.controllers.WorldController
import com.sao.aincrad.entities.BossEnemy
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.render.PixelSpriteAtlas
import com.sao.aincrad.ui.ControlsRenderer
import com.sao.aincrad.ui.DialogRenderer
import com.sao.aincrad.ui.HudRenderer
import com.sao.aincrad.ui.InventoryRenderer
import com.sao.aincrad.ui.VirtualControls
import com.sao.aincrad.utils.AudioManager
import com.sao.aincrad.utils.SaveManager

class GameScreen(
    @Suppress("UNUSED_PARAMETER") game: Game,
) : ScreenAdapter() {
    private val worldCamera = OrthographicCamera()
    private val uiCamera = OrthographicCamera()
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val font = BitmapFont()
    private val layout = GlyphLayout()
    private val controls = VirtualControls()
    private val controlsRenderer = ControlsRenderer(font, layout)
    private val hudRenderer = HudRenderer(font, layout)
    private val inventoryRenderer = InventoryRenderer(font)
    private val dialogRenderer = DialogRenderer(font)
    private val sprites = PixelSpriteAtlas()
    private val worldController = WorldController(AudioManager(), SaveManager())
    private val tempVec = Vector2()

    override fun show() {
        Gdx.input.inputProcessor = controls
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun resize(width: Int, height: Int) {
        worldCamera.setToOrtho(false, width.toFloat(), height.toFloat())
        uiCamera.setToOrtho(false, width.toFloat(), height.toFloat())
        controls.resize(width, height)
        clampWorldCamera()
    }

    override fun render(delta: Float) {
        handleInput()
        worldController.update(delta)
        clampWorldCamera()
        renderWorld()
        renderUi()
    }

    override fun resume() {
        Gdx.input.inputProcessor = controls
    }

    override fun dispose() {
        worldController.dispose()
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
        sprites.dispose()
    }

    private fun handleInput() {
        val state = worldController.state
        val joystick = controls.movementVector(tempVec)
        val keyX = when {
            Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT) -> 1f
            Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT) -> -1f
            else -> 0f
        }
        val keyY = when {
            Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP) -> 1f
            Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN) -> -1f
            else -> 0f
        }

        val moveX = joystick.x + keyX
        val moveY = joystick.y + keyY
        val moveLength = kotlin.math.sqrt((moveX * moveX + moveY * moveY).toDouble()).toFloat()
        if (moveLength > 1f) {
            worldController.setMovement(moveX / moveLength, moveY / moveLength)
        } else {
            worldController.setMovement(moveX, moveY)
        }

        if (Gdx.input.isKeyJustPressed(Keys.SPACE) || controls.consumeAttack()) worldController.basicAttack(1f)
        if (controls.consumeSkill1()) worldController.basicAttack(1.25f)
        if (controls.consumeSkill2()) worldController.basicAttack(1.55f)
        if (controls.consumeSkill3()) worldController.basicAttack(2.0f)
        if (Gdx.input.isKeyJustPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Keys.SHIFT_RIGHT) || controls.consumeDodge()) worldController.dodge()

        if (Gdx.input.isKeyJustPressed(Keys.I) || controls.consumeInventory()) worldController.toggleInventory()
        if (state.ui.inventoryOpen && Gdx.input.isKeyJustPressed(Keys.E)) worldController.equipFirstAvailable()
        else if (Gdx.input.isKeyJustPressed(Keys.E)) worldController.interactWithNearestNpc()

        if (Gdx.input.isKeyJustPressed(Keys.F5)) worldController.saveGame()
        if (Gdx.input.isKeyJustPressed(Keys.F9)) worldController.loadGame()
        if (Gdx.input.isKeyJustPressed(Keys.F6)) worldController.saveGame(2)
        if (Gdx.input.isKeyJustPressed(Keys.F7)) worldController.saveGame(3)
        if (Gdx.input.isKeyJustPressed(Keys.F10)) worldController.loadGame(2)
        if (Gdx.input.isKeyJustPressed(Keys.F11)) worldController.loadGame(3)
        if (Gdx.input.isKeyJustPressed(Keys.Q)) worldController.useFirstConsumable()
        if (Gdx.input.isKeyJustPressed(Keys.Z)) worldController.allocateStat("str")
        if (Gdx.input.isKeyJustPressed(Keys.X)) worldController.allocateStat("agi")
        if (Gdx.input.isKeyJustPressed(Keys.C)) worldController.allocateStat("int")
        if (Gdx.input.isKeyJustPressed(Keys.V)) worldController.allocateStat("def")

        if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) worldController.loadFloor(1)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) worldController.loadFloor(25)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) worldController.loadFloor(50)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) worldController.loadFloor(75)
        if (Gdx.input.isKeyJustPressed(Keys.NUM_5)) worldController.loadFloor(100)
    }

    private fun renderWorld() {
        val state = worldController.state
        Gdx.gl.glClearColor(0.08f, 0.10f, 0.14f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        worldCamera.update()
        shapeRenderer.projectionMatrix = worldCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.13f, 0.17f, 0.13f, 1f)
        shapeRenderer.rect(state.floor.worldBounds.x, state.floor.worldBounds.y, state.floor.worldBounds.width, state.floor.worldBounds.height)
        shapeRenderer.end()

        state.floor.renderer.setView(worldCamera)
        state.floor.renderer.render()

        batch.projectionMatrix = worldCamera.combined
        batch.begin()
        for (drop in state.lootDrops) {
            val bob = kotlin.math.sin((state.elapsedTime * 5f + drop.position.x * 0.03f).toDouble()).toFloat() * 2f
            batch.draw(sprites.lootFrame(drop.item.type), drop.position.x, drop.position.y + bob, 16f, 16f)
        }
        for (npc in state.npcs) {
            batch.draw(sprites.npcFrame(npc.role, state.elapsedTime), npc.position.x, npc.position.y, 32f, 32f)
        }
        for (enemy in state.enemies) {
            if (enemy.isDead) continue
            batch.draw(sprites.enemyFrame(enemy, state.elapsedTime), enemy.position.x - 2f, enemy.position.y - 4f, 32f, 32f)
        }
        val playerFrame = sprites.playerFrame(state.player.actionState, state.elapsedTime)
        val flipX = state.player.facing == PlayerEntity.Facing.LEFT
        val drawX = if (flipX) state.player.position.x + PlayerEntity.WIDTH else state.player.position.x
        val drawWidth = if (flipX) -PlayerEntity.WIDTH else PlayerEntity.WIDTH
        batch.draw(playerFrame, drawX, state.player.position.y, drawWidth, PlayerEntity.HEIGHT)
        for (popup in state.damagePopups) {
            font.color = popup.color.cpy().apply { a = popup.life.coerceIn(0f, 1f) }
            font.draw(batch, popup.text, popup.position.x, popup.position.y)
        }
        batch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        hudRenderer.drawWorldBars(
            shapeRenderer,
            worldCamera.position.x,
            worldCamera.position.y + worldCamera.viewportHeight * 0.5f,
            state.player,
            state.enemies,
        )
        shapeRenderer.end()
    }

    private fun renderUi() {
        val state = worldController.state
        uiCamera.update()

        shapeRenderer.projectionMatrix = uiCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        hudRenderer.drawPanels(shapeRenderer, uiCamera.viewportWidth, uiCamera.viewportHeight)
        controlsRenderer.drawPanels(shapeRenderer, controls)
        if (state.ui.inventoryOpen) {
            inventoryRenderer.drawPanel(shapeRenderer, state.inventory, uiCamera.viewportWidth, uiCamera.viewportHeight)
        }
        hudRenderer.drawMiniMap(shapeRenderer, uiCamera.viewportWidth, uiCamera.viewportHeight, state.floor, state.player.bounds, state.enemies)
        shapeRenderer.end()

        batch.projectionMatrix = uiCamera.combined
        batch.begin()
        val activeBoss = state.enemies.filterIsInstance<BossEnemy>().firstOrNull { !it.isDead }
        hudRenderer.drawText(
            batch,
            uiCamera.viewportWidth,
            uiCamera.viewportHeight,
            state.player,
            state.floor,
            state.ui.storyObjective,
            state.ui.pickupToast,
            state.ui.pickupToastTimer,
            activeBoss,
        )
        controlsRenderer.drawLabels(batch, controls)
        if (state.ui.inventoryOpen) {
            inventoryRenderer.drawText(batch, sprites, state.inventory, state.player.stats.gold, uiCamera.viewportWidth, uiCamera.viewportHeight)
        }
        if (state.ui.dialogTimer > 0f) {
            dialogRenderer.draw(batch, uiCamera.viewportWidth, state.ui.dialogSpeaker, state.ui.dialogText)
        }
        batch.end()
    }

    private fun clampWorldCamera() {
        val state = worldController.state
        val halfW = worldCamera.viewportWidth * 0.5f
        val halfH = worldCamera.viewportHeight * 0.5f
        val minX = state.floor.worldBounds.x + halfW
        val maxX = state.floor.worldBounds.x + state.floor.worldBounds.width - halfW
        val minY = state.floor.worldBounds.y + halfH
        val maxY = state.floor.worldBounds.y + state.floor.worldBounds.height - halfH

        worldCamera.position.x = state.player.position.x + PlayerEntity.WIDTH * 0.5f
        worldCamera.position.y = state.player.position.y + PlayerEntity.HEIGHT * 0.5f
        if (state.floor.worldBounds.width > worldCamera.viewportWidth) {
            worldCamera.position.x = worldCamera.position.x.coerceIn(minX, maxX)
        }
        if (state.floor.worldBounds.height > worldCamera.viewportHeight) {
            worldCamera.position.y = worldCamera.position.y.coerceIn(minY, maxY)
        }
    }
}
