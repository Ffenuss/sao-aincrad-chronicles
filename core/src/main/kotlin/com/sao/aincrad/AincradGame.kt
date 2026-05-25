package com.sao.aincrad

import com.badlogic.gdx.Game
import com.sao.aincrad.screens.GameScreen

class AincradGame : Game() {
    override fun create() {
        setScreen(GameScreen(this))
    }
}
