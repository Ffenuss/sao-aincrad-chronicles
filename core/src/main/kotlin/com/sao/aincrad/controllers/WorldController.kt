package com.sao.aincrad.controllers

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.sao.aincrad.combat.CombatGeometry
import com.sao.aincrad.entities.PlayerEntity
import com.sao.aincrad.items.Inventory
import com.sao.aincrad.items.ItemType
import com.sao.aincrad.items.Items
import com.sao.aincrad.maps.FloorMap
import com.sao.aincrad.net.CoopCoordinator
import com.sao.aincrad.net.PlayerInputCommand
import com.sao.aincrad.net.ReplicatedEnemyState
import com.sao.aincrad.net.ReplicatedPlayerState
import com.sao.aincrad.net.WorldSnapshot
import com.sao.aincrad.story.StoryController
import com.sao.aincrad.ui.DamagePopup
import com.sao.aincrad.utils.AudioManager
import com.sao.aincrad.utils.SaveManager

class WorldController(
    private val audioManager: AudioManager,
    private val saveManager: SaveManager,
) {
    companion object {
        private const val EVENT_COMBAT_HIT = "combat_hit"
        private const val EVENT_COMBAT_KILL = "combat_kill"
    }

    private data class RemotePlayerTrack(
        val playerId: String,
        var renderX: Float,
        var renderY: Float,
        var targetX: Float,
        var targetY: Float,
        var hp: Int,
        var facing: String,
        var actionState: String,
        var lastSeenTick: Long,
    )

    val state: WorldState

    private val localCoopPlayerId = "p_${(System.currentTimeMillis() % 100000)}"
    private val floorController = FloorController(audioManager, saveManager)
    private val storyController = StoryController()
    private val combatController = CombatController(audioManager, saveManager, storyController)
    private val coopCoordinator = CoopCoordinator(
        localPlayerId = localCoopPlayerId,
        sessionId = "sao_main_session",
    )
    private val remotePlayerTracks = linkedMapOf<String, RemotePlayerTrack>()
    private val remoteAttackCooldownByPlayerId = mutableMapOf<String, Float>()
    private var timeoutToastShown = false
    private var deathRespawnTimer = -1f

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

    fun refreshFloorMusic() {
        audioManager.playFloorMusic(state.floor.floorNumber)
    }

    fun hostCoop(roomCode: String): Result<Unit> = coopCoordinator.host(roomCode)

    fun joinCoop(roomCode: String): Result<Unit> = coopCoordinator.join(roomCode)

    fun setCoopReady(ready: Boolean) = coopCoordinator.setReady(ready)

    fun requestCoopMatchStart() = coopCoordinator.requestMatchStart()

    fun coopState(): CoopCoordinator.State = coopCoordinator.state()

    fun reconnectCoop(): Result<Unit> = coopCoordinator.reconnect()

    fun disconnectCoop() {
        coopCoordinator.disconnect()
        remotePlayerTracks.clear()
        remoteAttackCooldownByPlayerId.clear()
        timeoutToastShown = false
    }

    fun showToast(message: String, durationSeconds: Float = 1.6f) {
        state.ui.pickupToast = message
        state.ui.pickupToastTimer = durationSeconds
    }

    fun coopRemotePlayers(): List<ReplicatedPlayerState> {
        return remotePlayerTracks.values.map {
            ReplicatedPlayerState(
                playerId = it.playerId,
                x = it.renderX,
                y = it.renderY,
                hp = it.hp,
                facing = it.facing,
                actionState = it.actionState,
            )
        }
    }

    fun submitCoopInput(moveX: Float, moveY: Float, attackPressed: Boolean, dodgePressed: Boolean, skillSlot: Int? = null) {
        val coop = coopCoordinator.state()
        if (coop.phase != CoopCoordinator.Phase.MATCH || !coop.isConnected) return
        coopCoordinator.enqueueInput(
            PlayerInputCommand(
                playerId = localCoopPlayerId,
                moveX = moveX,
                moveY = moveY,
                attackPressed = attackPressed,
                dodgePressed = dodgePressed,
                skillSlot = skillSlot,
            ),
        )
    }

    fun toggleInventory() {
        state.ui.inventoryOpen = !state.ui.inventoryOpen
    }

    fun setMovement(x: Float, y: Float) {
        state.player.setMovement(x, y)
    }

    fun basicAttack(multiplier: Float = 1f) {
        val style = when {
            multiplier >= 1.85f -> PlayerEntity.AttackStyle.SPIN
            multiplier >= 1.30f -> PlayerEntity.AttackStyle.HEAVY
            else -> PlayerEntity.AttackStyle.LIGHT
        }
        state.player.basicAttack(multiplier, style)
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
        coopCoordinator.update(delta)
        updateRemotePlayerSmoothing(delta)
        val coop = coopCoordinator.state()
        if (coop.timedOut && !timeoutToastShown) {
            showToast("Co-op timeout. Press F12 to reconnect.", durationSeconds = 2.2f)
            timeoutToastShown = true
        } else if (!coop.timedOut) {
            timeoutToastShown = false
        }
        val authoritativeMatch = coop.isConnected && coop.phase == CoopCoordinator.Phase.MATCH && coop.isHost
        val replicatedMatch = coop.isConnected && coop.phase == CoopCoordinator.Phase.MATCH && !coop.isHost

        state.elapsedTime += delta
        state.player.update(delta, state.floor.collisionRects, state.floor.worldBounds)
        if (authoritativeMatch) {
            updateRemoteAttackCooldowns(delta)
            applyRemoteCombatInputs()
        }
        if (authoritativeMatch || !replicatedMatch) {
            combatController.update(
                state = state,
                delta = delta,
                spawnSystem = floorController.currentSpawnSystem(),
                onEnemyDamaged = { enemy, damage, _ ->
                    if (authoritativeMatch) {
                        coopCoordinator.sendGameplayEvent(
                            eventType = EVENT_COMBAT_HIT,
                            targetId = enemy.networkId.ifBlank { null },
                            value = damage,
                        )
                    }
                },
                onEnemyKilled = { enemy ->
                    if (authoritativeMatch) {
                        coopCoordinator.sendGameplayEvent(
                            eventType = EVENT_COMBAT_KILL,
                            targetId = enemy.networkId.ifBlank { null },
                        )
                    }
                },
            )
        } else {
            combatController.updateReplica(state, delta)
        }
        handlePlayerDefeat(delta)
        syncCoopReplication()
        maybeTransitionFloor()

        if (state.ui.pickupToastTimer > 0f) state.ui.pickupToastTimer = maxOf(0f, state.ui.pickupToastTimer - delta)
        if (state.ui.dialogTimer > 0f) state.ui.dialogTimer = maxOf(0f, state.ui.dialogTimer - delta)
    }

    private fun handlePlayerDefeat(delta: Float) {
        if (state.player.stats.hp > 0) {
            deathRespawnTimer = -1f
            return
        }

        if (deathRespawnTimer < 0f) {
            deathRespawnTimer = 2.2f
            state.ui.pickupToast = "You are down. Respawn in 2..."
            state.ui.pickupToastTimer = 1.3f
            return
        }

        deathRespawnTimer -= delta
        if (deathRespawnTimer > 0f) return

        floorController.respawnPlayer(state)
        state.player.reviveAt(state.player.position.x, state.player.position.y)
        state.ui.pickupToast = "Respawned at Town Gate"
        state.ui.pickupToastTimer = 1.6f
        deathRespawnTimer = -1f
    }

    private fun syncCoopReplication() {
        val coop = coopCoordinator.state()
        if (!coop.isConnected || coop.phase != CoopCoordinator.Phase.MATCH) {
            remotePlayerTracks.clear()
            return
        }

        val authoritativeHost = coop.isHost
        val enemyStates = if (authoritativeHost) {
            state.enemies.map { enemy ->
                ReplicatedEnemyState(
                    enemyId = enemy.networkId.ifBlank { "enemy_${enemy.hashCode()}" },
                    x = enemy.position.x,
                    y = enemy.position.y,
                    hp = enemy.hp,
                    isDead = enemy.isDead,
                )
            }
        } else {
            emptyList()
        }

        coopCoordinator.publishSnapshot(
            WorldSnapshot(
                tick = coop.localTick,
                floorNumber = state.floor.floorNumber,
                players = listOf(
                    ReplicatedPlayerState(
                        playerId = localCoopPlayerId,
                        x = state.player.position.x,
                        y = state.player.position.y,
                        hp = state.player.stats.hp,
                        facing = state.player.facing.name,
                        actionState = state.player.actionState.name,
                    ),
                ),
                enemies = enemyStates,
            ),
        )

        coopCoordinator.consumeLatestSnapshot()?.let { snapshot ->
            if (snapshot.floorNumber != state.floor.floorNumber && !authoritativeHost) {
                loadFloor(snapshot.floorNumber, autoSave = false, enforceAccess = false)
                showToast("Co-op synced floor ${snapshot.floorNumber}", durationSeconds = 1.6f)
            }
            val seenIds = mutableSetOf<String>()
            snapshot.players
                .filter { it.playerId != localCoopPlayerId }
                .forEach { remote ->
                    seenIds += remote.playerId
                    upsertRemotePlayer(remote, snapshot.tick)
                }
            remotePlayerTracks.keys.toList()
                .filter { it !in seenIds }
                .forEach { remotePlayerTracks.remove(it) }

            if (!authoritativeHost) {
                applyReplicatedEnemies(snapshot.enemies)
            }
        }
        applyIncomingCoopEvents()
    }

    private fun applyReplicatedEnemies(enemies: List<ReplicatedEnemyState>) {
        val byId = state.enemies.associateBy { it.networkId }
        val visibleIds = enemies.mapTo(mutableSetOf()) { it.enemyId }
        enemies.forEach { remote ->
            byId[remote.enemyId]?.applyReplicatedState(remote.x, remote.y, remote.hp, remote.isDead)
        }
        state.enemies.forEach { local ->
            val id = local.networkId
            if (id.isNotBlank() && id !in visibleIds) {
                local.applyReplicatedState(local.position.x, local.position.y, 0, dead = true)
            }
        }
    }

    private fun applyIncomingCoopEvents() {
        val coop = coopCoordinator.state()
        if (!coop.isConnected || coop.phase != CoopCoordinator.Phase.MATCH) return
        if (coop.isHost) return

        coopCoordinator.drainGameplayEvents().forEach { event ->
            when (event.eventType) {
                EVENT_COMBAT_HIT -> {
                    val enemy = state.enemies.firstOrNull { it.networkId == event.targetId }
                    val amount = event.value ?: return@forEach
                    val popupPos = if (enemy != null) {
                        Vector2(enemy.position.x + enemy.bounds.width * 0.5f, enemy.position.y + enemy.bounds.height + 8f)
                    } else {
                        Vector2(state.player.position.x + 8f, state.player.position.y + 32f)
                    }
                    state.damagePopups += DamagePopup(amount.toString(), popupPos, Color(0.78f, 0.92f, 1f, 1f))
                    audioManager.playSfx("hit")
                }
                EVENT_COMBAT_KILL -> {
                    val enemy = state.enemies.firstOrNull { it.networkId == event.targetId } ?: return@forEach
                    state.damagePopups += DamagePopup(
                        "KO",
                        Vector2(enemy.position.x + 8f, enemy.position.y + enemy.bounds.height + 12f),
                        Color.ORANGE,
                    )
                }
            }
        }
    }

    private fun updateRemoteAttackCooldowns(delta: Float) {
        val keys = remoteAttackCooldownByPlayerId.keys.toList()
        keys.forEach { key ->
            val next = (remoteAttackCooldownByPlayerId[key] ?: 0f) - delta
            if (next <= 0f) {
                remoteAttackCooldownByPlayerId.remove(key)
            } else {
                remoteAttackCooldownByPlayerId[key] = next
            }
        }
    }

    private fun applyRemoteCombatInputs() {
        val inputs = coopCoordinator.drainIncomingInputs()
        if (inputs.isEmpty()) return
        for (input in inputs) {
            val remote = remotePlayerTracks[input.playerId] ?: continue
            if (!input.attackPressed) continue

            val cd = remoteAttackCooldownByPlayerId[input.playerId] ?: 0f
            if (cd > 0f) continue
            remoteAttackCooldownByPlayerId[input.playerId] = 0.18f

            val multiplier = when (input.skillSlot) {
                1 -> 1.25f
                2 -> 1.55f
                3 -> 2.0f
                else -> 1f
            }
            val attackRange = when (input.skillSlot) {
                2 -> 26f
                3 -> 22f
                else -> 18f
            }
            val attackHitbox = buildReplicatedAttackHitbox(remote.targetX, remote.targetY, remote.facing, attackRange)
            val baseDamage = (state.player.stats.str * multiplier).toInt().coerceAtLeast(1)

            for (enemy in state.enemies) {
                if (enemy.isDead || !enemy.bounds.overlaps(attackHitbox)) continue
                val finalDamage = (baseDamage - enemy.defense).coerceAtLeast(1)
                enemy.takeDamage(finalDamage)
                state.damagePopups += DamagePopup(
                    finalDamage.toString(),
                    Vector2(enemy.position.x + enemy.bounds.width * 0.5f, enemy.position.y + enemy.bounds.height + 8f),
                    Color(0.74f, 0.90f, 1f, 1f),
                )
                coopCoordinator.sendGameplayEvent(
                    eventType = EVENT_COMBAT_HIT,
                    targetId = enemy.networkId.ifBlank { null },
                    value = finalDamage,
                )
                if (enemy.isDead) {
                    combatController.finalizeEnemyDeath(state, enemy)
                    coopCoordinator.sendGameplayEvent(
                        eventType = EVENT_COMBAT_KILL,
                        targetId = enemy.networkId.ifBlank { null },
                    )
                }
            }
        }
    }

    private fun updateRemotePlayerSmoothing(delta: Float) {
        if (remotePlayerTracks.isEmpty()) return
        val factor = (delta * 12f).coerceIn(0f, 1f)
        remotePlayerTracks.values.forEach { track ->
            track.renderX += (track.targetX - track.renderX) * factor
            track.renderY += (track.targetY - track.renderY) * factor
        }
    }

    private fun upsertRemotePlayer(remote: ReplicatedPlayerState, tick: Long) {
        val existing = remotePlayerTracks[remote.playerId]
        if (existing == null) {
            remotePlayerTracks[remote.playerId] = RemotePlayerTrack(
                playerId = remote.playerId,
                renderX = remote.x,
                renderY = remote.y,
                targetX = remote.x,
                targetY = remote.y,
                hp = remote.hp,
                facing = remote.facing,
                actionState = remote.actionState,
                lastSeenTick = tick,
            )
            return
        }
        existing.targetX = remote.x
        existing.targetY = remote.y
        existing.hp = remote.hp
        existing.facing = remote.facing
        existing.actionState = remote.actionState
        existing.lastSeenTick = tick
    }

    private fun buildReplicatedAttackHitbox(x: Float, y: Float, facing: String, range: Float): Rectangle {
        return CombatGeometry.attackHitbox(
            x = x,
            y = y,
            width = PlayerEntity.WIDTH,
            height = PlayerEntity.HEIGHT,
            facing = facing,
            range = range,
        )
    }

    private fun maybeTransitionFloor() {
        val exit = state.floor.exitZone ?: return
        if (!state.player.bounds.overlaps(exit)) return
        val nextFloor = state.floor.exitTargetFloor ?: floorController.nextFloorFor(state.floor.floorNumber)
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
