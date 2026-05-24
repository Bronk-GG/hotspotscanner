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
 * Top-level repository that coordinates NetworkInfoManager and NetworkScanner.
 * No hotspot creation — the app passively monitors whatever network is active.
 */
class AppRepository(context: Context) {

    val networkInfoManager = NetworkInfoManager(context)
    val networkScanner = NetworkScanner(context)

    val localIp: StateFlow<String> = networkInfoManager.localIp
    val networkName: StateFlow<String> = networkInfoManager.networkName

    /** All known devices as a list, sorted: online first, then by last-seen descending */
    val devices: Flow<List<ConnectedDevice>> = networkScanner.devices.map { map ->
        map.values
            .sortedWith(compareByDescending<ConnectedDevice> { it.isOnline }.thenByDescending { it.lastSeen })
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null
    private var isScanning = false

    fun startScanning() {
        if (isScanning) return
        isScanning = true
        networkInfoManager.refresh()
        networkScanner.startMdnsDiscovery()
        scanJob?.cancel()
        scanJob = scope.launch {
            while (true) {
                networkScanner.scanSubnet(networkInfoManager.gatewayIp())
                delay(8_000)
            }
        }
    }

    fun stopScanning() {
        isScanning = false
        scanJob?.cancel()
        scanJob = null
        networkScanner.stopMdnsDiscovery()
        networkScanner.clearDevices()
    }

    fun isScanningActive() = isScanning
}
