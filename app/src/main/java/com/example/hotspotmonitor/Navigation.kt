package com.example.hotspotmonitor

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hotspotmonitor.data.AppRepository
import com.example.hotspotmonitor.data.HotspotConfig
import com.example.hotspotmonitor.ui.detail.DeviceDetailScreen
import com.example.hotspotmonitor.ui.main.DashboardScreen
import com.example.hotspotmonitor.ui.main.DashboardViewModel
import com.example.hotspotmonitor.ui.settings.SettingsScreen

sealed class Screen {
    object Main : Screen()
    data class DeviceDetail(val ip: String) : Screen()
    object Settings : Screen()
}

@Composable
fun MainNavigation(repository: AppRepository) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Main) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Main
    
    val dashboardViewModel = remember { DashboardViewModel(repository) }

    AnimatedContent(targetState = currentScreen, label = "navigation") { screen ->
        when (screen) {
            is Screen.Main -> {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onDeviceClick = { ip -> backStack.add(Screen.DeviceDetail(ip)) },
                    onSettingsClick = { backStack.add(Screen.Settings) },
                )
            }
            is Screen.DeviceDetail -> {
                val ip = screen.ip
                val devices by dashboardViewModel.devices.collectAsStateWithLifecycle()
                val device = devices.find { d -> d.ip == ip }
                if (device != null) {
                    DeviceDetailScreen(
                        device = device,
                        onBack = { backStack.removeLastOrNull() },
                    )
                } else {
                    backStack.removeLastOrNull()
                }
            }
            is Screen.Settings -> {
                val config by dashboardViewModel.hotspotConfig.collectAsStateWithLifecycle()
                SettingsScreen(
                    currentConfig = config,
                    onSave = { newConfig -> dashboardViewModel.updateConfig(newConfig) },
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        }
    }
}
