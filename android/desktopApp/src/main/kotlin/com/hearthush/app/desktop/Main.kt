package com.hearthush.app.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.hearthush.app.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HeartHush • Countdowns",
        state = rememberWindowState(width = 480.dp, height = 860.dp)
    ) {
        App()
    }
}
