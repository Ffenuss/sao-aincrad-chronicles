package com.sao.aincrad.android

import android.os.Bundle
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.sao.aincrad.AincradGame

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashHandler()

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = true
            useWakelock = true
        }

        try {
            initialize(AincradGame(), config)
        } catch (t: Throwable) {
            showCrashScreen(t)
            finish()
        }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            showCrashScreen(throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun showCrashScreen(t: Throwable) {
        Log.e("SAO_CRASH", "Unhandled runtime crash", t)
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        val report = buildString {
            appendLine("SAO runtime crash")
            appendLine()
            appendLine("${t::class.java.name}: ${t.message ?: "no message"}")
            appendLine()
            append(sw.toString())
        }
        CrashActivity.open(this, report)
    }
}
