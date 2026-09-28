package com.secount.app.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.secount.app.ui.App
import java.awt.event.WindowFocusListener
import java.awt.event.WindowEvent

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Secount ♥ Countdowns",
        state = rememberWindowState(width = 480.dp, height = 860.dp)
    ) {
        // User rule: lock only when the window loses focus (Home/minimize/
        // Alt-Tab), never on a timer. App.kt polls this flag once per second.
        remember(window) {
            try {
                window.addWindowFocusListener(object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) {}
                    override fun windowLostFocus(e: WindowEvent?) {
                        try {
                            java.util.prefs.Preferences.userRoot().node("secount")
                                .put("secount_need_lock", "1")
                        } catch (ignored: Exception) {
                        }
                    }
                })
            } catch (ignored: Exception) {
            }
            true
        }
        App()
    }
}
