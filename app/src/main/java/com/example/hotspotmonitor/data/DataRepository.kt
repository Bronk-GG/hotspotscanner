package com.example.hotspotmonitor.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Top-level repository that coordinates HotspotManager and NetworkScanner.
 * This is the single source of truth for all app state.
 */
class AppRepository(context: Context) {

    val hotspotManager = HotspotManager(context)
    val networkScanner = NetworkScanner(context)

    val hotspotState: StateFlow<HotspotState> = hotspotManager.hotspotState
    val hotspotConfig: StateFlow<HotspotConfig> = hotspotManager.config
    val gatewayIp: StateFlow<String> = hotspotManager.gatewayIp

    /** All known devices as a list, sorted: online first, then by last-seen descending */
    val devices: Flow<List<ConnectedDevice>> = networkScanner.devices.map { map ->
        map.values
            .sortedWith(compareByDescending<ConnectedDevice> { it.isOnline }.thenByDescending { it.lastSeen })
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null

    fun startHotspot(onStarted: () -> Unit, onError: (String) -> Unit) {
        hotspotManager.startHotspot(
            onStarted = {
                onStarted()
                startScanning()
            },
            onError = onError,
        )
    }

    fun stopHotspot() {
        stopScanning()
        hotspotManager.stopHotspot()
        networkScanner.stopMdnsDiscovery()
        networkScanner.clearDevices()
    }

    private fun startScanning() {
        networkScanner.startMdnsDiscovery()
        scanJob?.cancel()
        scanJob = scope.launch {
            while (true) {
                networkScanner.scanSubnet(hotspotManager.gatewayIp.value)
                delay(8_000) // rescan every 8 seconds
            }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
    }

    fun updateConfig(config: HotspotConfig) {
        hotspotManager.updateConfig(config)
    }
}
