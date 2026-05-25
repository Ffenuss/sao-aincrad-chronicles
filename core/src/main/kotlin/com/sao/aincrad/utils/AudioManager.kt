package com.sao.aincrad.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

class AudioManager {
    private var music: Music? = null
    private val sounds = mutableMapOf<String, Sound>()
    var musicVolume = 0.45f
    var sfxVolume = 0.7f

    fun playFloorMusic(floorNumber: Int) {
        val basePath = when (floorNumber) {
            25 -> "audio/floor25.ogg"
            50 -> "audio/floor50.ogg"
            75 -> "audio/floor75.ogg"
            100 -> "audio/floor100.ogg"
            else -> "audio/floor1.ogg"
        }
        val file = existingAudioFile(basePath) ?: return
        music?.stop()
        music?.dispose()
        music = Gdx.audio.newMusic(file).apply {
            isLooping = true
            volume = musicVolume
            play()
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
        sounds.values.forEach { it.dispose() }
        sounds.clear()
    }
}
