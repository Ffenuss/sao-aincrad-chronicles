package com.sao.aincrad.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

class AudioManager {
    private var music: Music? = null
    private var ambientLoopSound: Sound? = null
    private var ambientLoopId: Long = -1L
    private val sounds = mutableMapOf<String, Sound>()
    var musicVolume = 0.9f
    var sfxVolume = 1.0f

    fun playFloorMusic(floorNumber: Int) {
        val basePath = when (floorNumber) {
            25 -> "audio/floor25.ogg"
            50 -> "audio/floor50.ogg"
            75 -> "audio/floor75.ogg"
            100 -> "audio/floor100.ogg"
            else -> "audio/floor1.ogg"
        }
        val file = existingAudioFile(basePath) ?: return
        Gdx.app.log("AudioManager", "playFloorMusic floor=$floorNumber file=${file.path()}")
        music?.stop()
        music?.dispose()
        stopAmbientLoop()
        try {
            music = Gdx.audio.newMusic(file).apply {
                isLooping = true
                volume = musicVolume
                play()
            }
            Gdx.app.log("AudioManager", "Music backend started")
        } catch (_: Throwable) {
            music = null
            try {
                ambientLoopSound = Gdx.audio.newSound(file)
                ambientLoopId = ambientLoopSound?.loop(musicVolume) ?: -1L
                Gdx.app.log("AudioManager", "Sound loop fallback started id=$ambientLoopId")
            } catch (_: Throwable) {
                ambientLoopSound = null
                ambientLoopId = -1L
                Gdx.app.error("AudioManager", "Failed to start both music and sound fallback")
            }
        }
    }

    fun playSfx(id: String) {
        val file = existingAudioFile("audio/$id.ogg") ?: return
        val sound = sounds.getOrPut(id) { Gdx.audio.newSound(file) }
        sound.play(sfxVolume)
    }

    private fun existingAudioFile(oggPath: String) = listOf(
        Gdx.files.internal(oggPath),
        Gdx.files.internal(oggPath.removeSuffix(".ogg") + ".wav"),
    ).firstOrNull { it.exists() }

    fun dispose() {
        music?.dispose()
        stopAmbientLoop()
        ambientLoopSound?.dispose()
        ambientLoopSound = null
        sounds.values.forEach { it.dispose() }
        sounds.clear()
    }

    private fun stopAmbientLoop() {
        if (ambientLoopId >= 0L) {
            ambientLoopSound?.stop(ambientLoopId)
            ambientLoopId = -1L
        }
    }
}
