package com.example.hotspotmonitor.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads information about the currently active network (WiFi or system hotspot).
 * No hotspot creation — purely passive discovery.
 */
class NetworkInfoManager(private val context: Context) {

    private val _localIp = MutableStateFlow("Unknown")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _networkName = MutableStateFlow("Unknown")
    val networkName: StateFlow<String> = _networkName.asStateFlow()

    /** Refresh local IP and network name from system. Call this before starting a scan. */
    fun refresh() {
        _localIp.value = getLocalIp()
        _networkName.value = getNetworkName()
    }

    /** Returns the subnet gateway (first .1 address on the subnet) */
    fun gatewayIp(): String {
        val ip = _localIp.value
        if (ip == "Unknown") return "192.168.1.1"
        val prefix = ip.substringBeforeLast(".")
        return "$prefix.1"
    }

    private fun getLocalIp(): String {
        return try {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wifiManager.connectionInfo
            val ipInt = info.ipAddress
            if (ipInt != 0) {
                // Convert little-endian int to dot notation
                "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt shr 24 and 0xff}"
            } else {
                // Fallback: iterate network interfaces
                java.net.NetworkInterface.getNetworkInterfaces()
                    .asSequence()
                    .flatMap { it.inetAddresses.asSequence() }
                    .filterIsInstance<java.net.Inet4Address>()
                    .firstOrNull { !it.isLoopbackAddress }
                    ?.hostAddress ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getNetworkName(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNet = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNet)
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                    @Suppress("DEPRECATION")
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val ssid = wm.connectionInfo.ssid?.trim('"') ?: "WiFi"
                    if (ssid == "<unknown ssid>") "WiFi Network" else ssid
                }
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Active Network"
            }
        } catch (e: Exception) {
            "Active Network"
        }
    }
}
