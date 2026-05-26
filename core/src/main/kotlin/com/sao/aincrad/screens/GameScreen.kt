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
import com.sao.aincrad.entities.EnemyEntity
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.net.DebugCoopLoopback
import com.sao.aincrad.net.CoopCoordinator
import com.sao.aincrad.net.LanEndpointConfig
import com.sao.aincrad.net.ReplicatedPlayerState
import com.sao.aincrad.render.FloorBackdropRenderer
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
    companion object {
        private const val SHOW_COOP_DEBUG = false
        private const val DEFAULT_COOP_ROOM = "AINCRAD"
        private const val PLAYER_DRAW_SIZE = 36f
        private const val ENTITY_DRAW_SIZE = 32f
        private val TINT_WHITE = Color(1f, 1f, 1f, 1f)
    }

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
    private val floorBackdrop = FloorBackdropRenderer()
    private val worldController = WorldController(AudioManager(), SaveManager())
    private val lanConfig = LanEndpointConfig()
    private val tempVec = Vector2()
    private val coopLoopback = if (SHOW_COOP_DEBUG) DebugCoopLoopback() else null
    private var coopDebugLines: List<String> = emptyList()
    private var showNetDebugOverlay = false
    private var showCoopChecklist = false

    override fun show() {
        Gdx.input.inputProcessor = controls
        worldController.refreshFloorMusic()
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun resize(width: Int, height: Int) {
        val aspect = if (height <= 0) 16f / 9f else width.toFloat() / height.toFloat()
        val targetWorldHeight = 440f
        val targetWorldWidth = targetWorldHeight * aspect
        worldCamera.setToOrtho(false, targetWorldWidth, targetWorldHeight)
        uiCamera.setToOrtho(false, width.toFloat(), height.toFloat())
        controls.resize(width, height)
        clampWorldCamera()
    }

    override fun render(delta: Float) {
        handleInput()
        worldController.update(delta)
        if (SHOW_COOP_DEBUG) {
            coopLoopback?.update(delta, worldController.state)
            coopDebugLines = coopLoopback?.debugLines().orEmpty()
        }
        clampWorldCamera()
        renderWorld()
        renderUi()
    }

    override fun resume() {
        Gdx.input.inputProcessor = controls
        worldController.refreshFloorMusic()
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

        val attackPressed = Gdx.input.isKeyJustPressed(Keys.SPACE) || controls.consumeAttack()
        val skill1Pressed = controls.consumeSkill1()
        val skill2Pressed = controls.consumeSkill2()
        val skill3Pressed = controls.consumeSkill3()
        val dodgePressed = Gdx.input.isKeyJustPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Keys.SHIFT_RIGHT) || controls.consumeDodge()

        if (attackPressed) worldController.basicAttack(1f)
        if (skill1Pressed) worldController.basicAttack(1.25f)
        if (skill2Pressed) worldController.basicAttack(1.55f)
        if (skill3Pressed) worldController.basicAttack(2.0f)
        if (dodgePressed) worldController.dodge()

        val skillSlot = when {
            skill1Pressed -> 1
            skill2Pressed -> 2
            skill3Pressed -> 3
            else -> null
        }
        worldController.submitCoopInput(
            moveX = moveX,
            moveY = moveY,
            attackPressed = attackPressed || skillSlot != null,
            dodgePressed = dodgePressed,
            skillSlot = skillSlot,
        )

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

        if (Gdx.input.isKeyJustPressed(Keys.F1)) {
            val result = worldController.hostCoop(DEFAULT_COOP_ROOM)
            if (result.isSuccess) {
                worldController.showToast("Co-op host started: $DEFAULT_COOP_ROOM")
            } else {
                worldController.showToast("Co-op host failed")
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.F2)) {
            val result = worldController.joinCoop(DEFAULT_COOP_ROOM)
            if (result.isSuccess) {
                worldController.showToast("Co-op join requested: $DEFAULT_COOP_ROOM")
            } else {
                worldController.showToast("Co-op join failed")
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.K)) {
            val result = worldController.hostCoop(lanConfig.hostRoomCode())
            if (result.isSuccess) {
                worldController.showToast("LAN host opened: ${lanConfig.hostRoomCode()}")
            } else {
                worldController.showToast("LAN host failed")
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.L)) {
            val result = worldController.joinCoop(lanConfig.joinRoomCode())
            if (result.isSuccess) {
                worldController.showToast("LAN join: ${lanConfig.joinRoomCode()}")
            } else {
                worldController.showToast("LAN join failed")
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.J)) {
            val host = lanConfig.cycleHostForward()
            worldController.showToast("LAN host target: $host")
        }
        if (Gdx.input.isKeyJustPressed(Keys.H)) {
            val host = lanConfig.cycleHostBackward()
            worldController.showToast("LAN host target: $host")
        }
        if (Gdx.input.isKeyJustPressed(Keys.U)) {
            val port = lanConfig.decrementPort(1)
            worldController.showToast("LAN port: $port")
        }
        if (Gdx.input.isKeyJustPressed(Keys.O)) {
            val port = lanConfig.incrementPort(1)
            worldController.showToast("LAN port: $port")
        }
        if (Gdx.input.isKeyJustPressed(Keys.F3)) {
            val coop = worldController.coopState()
            if (coop.phase == CoopCoordinator.Phase.LOBBY) {
                val nextReady = !coop.localReady
                worldController.setCoopReady(nextReady)
                worldController.showToast(if (nextReady) "Co-op ready" else "Co-op not ready")
            } else {
                worldController.showToast("Co-op ready is available only in lobby")
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.F4)) {
            worldController.requestCoopMatchStart()
            worldController.showToast("Co-op match start requested")
        }
        if (Gdx.input.isKeyJustPressed(Keys.F8)) {
            worldController.disconnectCoop()
            worldController.showToast("Co-op disconnected")
        }
        if (Gdx.input.isKeyJustPressed(Keys.F12)) {
            val result = worldController.reconnectCoop()
            if (result.isSuccess) {
                worldController.showToast("Co-op reconnecting...")
            } else {
                worldController.showToast("Co-op reconnect unavailable")
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.GRAVE)) {
            showNetDebugOverlay = !showNetDebugOverlay
            worldController.showToast(if (showNetDebugOverlay) "Net debug ON" else "Net debug OFF")
        }
        if (Gdx.input.isKeyJustPressed(Keys.B)) {
            showCoopChecklist = !showCoopChecklist
            worldController.showToast(if (showCoopChecklist) "Co-op checklist ON" else "Co-op checklist OFF")
        }
    }

    private fun renderWorld() {
        val state = worldController.state
        Gdx.gl.glClearColor(0.08f, 0.10f, 0.14f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        worldCamera.update()
        shapeRenderer.projectionMatrix = worldCamera.combined
        if (!state.floor.hasImageBackdrop) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            floorBackdrop.draw(shapeRenderer, state.floor, state.elapsedTime)
            shapeRenderer.end()
        }

        state.floor.renderer.setView(worldCamera)
        state.floor.renderer.render()

        batch.projectionMatrix = worldCamera.combined
        batch.begin()
        for (drop in state.lootDrops) {
            val bob = kotlin.math.sin((state.elapsedTime * 5f + drop.position.x * 0.03f).toDouble()).toFloat() * 2f
            batch.draw(sprites.lootFrame(drop.item.type), drop.position.x, drop.position.y + bob, 16f, 16f)
        }
        for (npc in state.npcs) {
            batch.setColor(TINT_WHITE)
            batch.draw(sprites.npcFrame(npc.id, npc.role, state.elapsedTime), pixel(npc.position.x - 2f), pixel(npc.position.y - 2f), ENTITY_DRAW_SIZE, ENTITY_DRAW_SIZE)
            batch.setColor(TINT_WHITE)
        }
        for (enemy in state.enemies) {
            if (enemy.isDead) continue
            batch.draw(sprites.enemyFrame(enemy, state.elapsedTime), pixel(enemy.position.x - 1f), pixel(enemy.position.y - 2f), ENTITY_DRAW_SIZE, ENTITY_DRAW_SIZE)
        }
        batch.setColor(0f, 0f, 0f, 0.24f)
        batch.draw(sprites.playerFrame(PlayerEntity.ActionState.IDLE, state.elapsedTime, state.player.facing), pixel(state.player.position.x + 10f), pixel(state.player.position.y - 3f), 24f, 8f)
        batch.setColor(TINT_WHITE)
        val playerFrame = sprites.playerFrame(state.player.actionState, state.elapsedTime, state.player.facing)
        val drawX = pixel(state.player.position.x - (PLAYER_DRAW_SIZE - PlayerEntity.WIDTH) * 0.5f)
        val drawY = pixel(state.player.position.y - 4f)
        batch.draw(
            playerFrame,
            drawX,
            drawY,
            PLAYER_DRAW_SIZE,
            PLAYER_DRAW_SIZE,
        )
        val replicatedRemotePlayers = worldController.coopRemotePlayers()
        for (remote in replicatedRemotePlayers) {
            drawRemotePlayer(remote, state.elapsedTime)
        }

        if (SHOW_COOP_DEBUG) {
            coopLoopback?.remoteGhost()?.let { remote ->
                val remoteFrame = sprites.playerFrame(PlayerEntity.ActionState.RUNNING, state.elapsedTime, parseFacing(remote.facing))
                val remoteDrawX = pixel(remote.x - (PLAYER_DRAW_SIZE - PlayerEntity.WIDTH) * 0.5f)
                val remoteDrawY = pixel(remote.y - 4f)
                batch.setColor(0.75f, 1f, 0.8f, 0.80f)
                batch.draw(
                    remoteFrame,
                    remoteDrawX,
                    remoteDrawY,
                    PLAYER_DRAW_SIZE,
                    PLAYER_DRAW_SIZE,
                )
                batch.setColor(TINT_WHITE)
            }
        }
        for (popup in state.damagePopups) {
            font.color = popup.color.cpy().apply { a = popup.life.coerceIn(0f, 1f) }
            font.draw(batch, popup.text, popup.position.x, popup.position.y)
        }
        batch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        drawEnemyTelegraphs(shapeRenderer, state)
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
        val coopLines = buildCoopStatusLines(worldController.coopState())
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
            if (SHOW_COOP_DEBUG) coopDebugLines + coopLines else coopLines,
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

    private fun buildCoopStatusLines(coop: CoopCoordinator.State): List<String> {
        if (!coop.isConnected && !showNetDebugOverlay && !showCoopChecklist) {
            return emptyList()
        }
        val role = when {
            !coop.isConnected -> "offline"
            coop.isHost -> "host"
            else -> "client"
        }
        return listOf(
            "CO-OP [$role] room=${if (coop.roomCode.isBlank()) "-" else coop.roomCode} phase=${coop.phase.name}",
            "F1/F2 Loopback  K/L LAN  H/J Host  U/O Port  F12 Reconnect",
            "ready=${coop.localReady} remote=${coop.remoteReadyCount} allReady=${coop.allReady} tick=${coop.localTick}",
            "net in=${coop.messagesIn} out=${coop.messagesOut} rtt=${coop.averageRttMs?.toInt()?.toString() ?: "-"}ms",
            if (coop.reconnectAvailable) "reconnect ${coop.reconnectRemainingMs / 1000}s" else "reconnect -",
            if (coop.timedOut) "status TIMEOUT" else "status OK",
            "lan ${lanConfig.currentHost()}:${lanConfig.currentPort()}",
            "~ net-debug  B checklist",
        ) + if (showNetDebugOverlay) {
            listOf(
                "loss=${"%.1f".format(coop.packetLossPercent)}% drift=${coop.tickDrift} desync=${if (coop.desyncWarning) "WARN" else "ok"}",
            )
        } else {
            emptyList()
        } + if (showCoopChecklist) {
            buildCoopChecklistLines(coop)
        } else {
            emptyList()
        }
    }

    private fun buildCoopChecklistLines(coop: CoopCoordinator.State): List<String> {
        val connected = if (coop.isConnected) "PASS" else "WAIT"
        val lobbyReady = if (coop.phase == CoopCoordinator.Phase.LOBBY && coop.localReady) "PASS" else "WAIT"
        val peerSeen = if (coop.remoteReadyCount > 0 || coop.remoteTrafficSeen) "PASS" else "WAIT"
        val matchStarted = if (coop.phase == CoopCoordinator.Phase.MATCH) "PASS" else "WAIT"
        val netStable = if ((coop.averageRttMs ?: 0f) > 0f && !coop.desyncWarning) "PASS" else "WAIT"
        return listOf(
            "CHECKLIST:",
            "1 connected: $connected",
            "2 local ready: $lobbyReady",
            "3 peer seen: $peerSeen",
            "4 match started: $matchStarted",
            "5 net stable: $netStable",
        )
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
        worldCamera.position.x = pixel(worldCamera.position.x)
        worldCamera.position.y = pixel(worldCamera.position.y)
    }

    private fun drawRemotePlayer(remote: ReplicatedPlayerState, elapsedTime: Float) {
        val remoteFrame = sprites.playerFrame(parseRemoteActionState(remote.actionState), elapsedTime, parseFacing(remote.facing))
        val drawX = pixel(remote.x - (PLAYER_DRAW_SIZE - PlayerEntity.WIDTH) * 0.5f)
        val drawY = pixel(remote.y - 4f)
        batch.setColor(0.75f, 0.95f, 1f, 0.90f)
        batch.draw(
            remoteFrame,
            drawX,
            drawY,
            PLAYER_DRAW_SIZE,
            PLAYER_DRAW_SIZE,
        )
        batch.setColor(TINT_WHITE)
    }

    private fun parseRemoteActionState(raw: String): PlayerEntity.ActionState {
        return when (raw.uppercase()) {
            "RUNNING" -> PlayerEntity.ActionState.RUNNING
            "ATTACKING" -> PlayerEntity.ActionState.ATTACKING
            "DODGING" -> PlayerEntity.ActionState.DODGING
            "HURT" -> PlayerEntity.ActionState.HURT
            "DEAD" -> PlayerEntity.ActionState.DEAD
            else -> PlayerEntity.ActionState.IDLE
        }
    }

    private fun parseFacing(raw: String): PlayerEntity.Facing {
        return when (raw.uppercase()) {
            "UP" -> PlayerEntity.Facing.UP
            "UP_RIGHT" -> PlayerEntity.Facing.UP_RIGHT
            "RIGHT" -> PlayerEntity.Facing.RIGHT
            "DOWN_RIGHT" -> PlayerEntity.Facing.DOWN_RIGHT
            "DOWN" -> PlayerEntity.Facing.DOWN
            "DOWN_LEFT" -> PlayerEntity.Facing.DOWN_LEFT
            "LEFT" -> PlayerEntity.Facing.LEFT
            "UP_LEFT" -> PlayerEntity.Facing.UP_LEFT
            else -> PlayerEntity.Facing.DOWN
        }
    }

    private fun pixel(v: Float): Float {
        return kotlin.math.floor(v + 0.5f)
    }

    private fun drawEnemyTelegraphs(shape: ShapeRenderer, state: com.sao.aincrad.controllers.WorldState) {
        state.enemies.forEach { enemy ->
            if (enemy.isDead || enemy.state != EnemyEntity.AIState.ATTACK) return@forEach
            val radius = when (enemy.spriteKey) {
                "boss" -> 92f
                "knight" -> 54f
                "dark_elf" -> 80f
                else -> 44f
            }
            shape.color = Color(1f, 0.20f, 0.20f, 0.18f)
            shape.circle(
                enemy.position.x + enemy.bounds.width * 0.5f,
                enemy.position.y + enemy.bounds.height * 0.5f,
                radius,
                24,
            )
            shape.color = Color(1f, 0.80f, 0.80f, 0.22f)
            shape.circle(
                enemy.position.x + enemy.bounds.width * 0.5f,
                enemy.position.y + enemy.bounds.height * 0.5f,
                radius * 0.62f,
                20,
            )
        }
    }

}
