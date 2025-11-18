package me.sukhov.weather.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import me.sukhov.weather.ui.component.App

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Weather",
            state = WindowState(
                width = 500.dp,
                height = 800.dp
            )
        ) { App() }
    }
}