package com.example.hotspotmonitor.data

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.reflect.Method

private const val TAG = "HotspotManager"

enum class HotspotState { OFF, STARTING, ON, STOPPING, ERROR }

data class HotspotConfig(
    val ssid: String = "NetWatch",
    val password: String = "netwatch123",
)

/**
 * Manages the Android WiFi hotspot (SoftAP).
 *
 * Uses the public WifiManager API on Android 8+ (Oreo) via
 * startLocalOnlyHotspot, and reflection for older approaches on
 * devices that still support it.
 *
 * On Android 13+ the NEARBY_WIFI_DEVICES permission is required
 * in addition to ACCESS_FINE_LOCATION.
 */
class HotspotManager(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _hotspotState = MutableStateFlow(HotspotState.OFF)
    val hotspotState: StateFlow<HotspotState> = _hotspotState.asStateFlow()

    private val _config = MutableStateFlow(HotspotConfig())
    val config: StateFlow<HotspotConfig> = _config.asStateFlow()

    /** Gateway IP of the hotspot interface — typically 192.168.43.1 */
    private val _gatewayIp = MutableStateFlow("192.168.43.1")
    val gatewayIp: StateFlow<String> = _gatewayIp.asStateFlow()

    private var localOnlyCallback: WifiManager.LocalOnlyHotspotCallback? = null
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun updateConfig(newConfig: HotspotConfig) {
        _config.value = newConfig
    }

    /**
     * Start the hotspot. On Android 8+ this uses LocalOnlyHotspot.
     * The SSID/password in [config] may not be honoured on Android 10+
     * due to OS restrictions, but the hotspot will still be created.
     */
    fun startHotspot(onStarted: () -> Unit, onError: (String) -> Unit) {
        if (_hotspotState.value == HotspotState.ON ||
            _hotspotState.value == HotspotState.STARTING) return

        _hotspotState.value = HotspotState.STARTING

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startLocalOnlyHotspot(onStarted, onError)
        } else {
            startHotspotLegacy(onStarted, onError)
        }
    }

    private fun startLocalOnlyHotspot(onStarted: () -> Unit, onError: (String) -> Unit) {
        localOnlyCallback = object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(r: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(r)
                reservation = r
                _hotspotState.value = HotspotState.ON
                // Try to read the actual SSID/password from the reservation
                try {
                    val wifiConfig = r.wifiConfiguration
                    if (wifiConfig != null) {
                        _config.value = HotspotConfig(
                            ssid = wifiConfig.SSID?.removePrefix("\"")?.removeSuffix("\"")
                                ?: _config.value.ssid,
                            password = wifiConfig.preSharedKey?.removePrefix("\"")?.removeSuffix("\"")
                                ?: _config.value.password,
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not read hotspot config: ${e.message}")
                }
                detectGatewayIp()
                Log.d(TAG, "Hotspot started. SSID=${_config.value.ssid}")
                onStarted()
            }

            override fun onStopped() {
                super.onStopped()
                _hotspotState.value = HotspotState.OFF
                reservation = null
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                _hotspotState.value = HotspotState.ERROR
                reservation = null
                onError("Hotspot failed to start (reason=$reason). Check permissions.")
            }
        }

        try {
            wifiManager.startLocalOnlyHotspot(localOnlyCallback!!, null)
        } catch (e: Exception) {
            _hotspotState.value = HotspotState.ERROR
            onError("Exception starting hotspot: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun startHotspotLegacy(onStarted: () -> Unit, onError: (String) -> Unit) {
        try {
            val method: Method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                android.net.wifi.WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType,
            )
            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = _config.value.ssid
                preSharedKey = _config.value.password
                allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA2_PSK)
            }
            val result = method.invoke(wifiManager, wifiConfig, true) as Boolean
            if (result) {
                _hotspotState.value = HotspotState.ON
                detectGatewayIp()
                onStarted()
            } else {
                _hotspotState.value = HotspotState.ERROR
                onError("Legacy hotspot API returned false.")
            }
        } catch (e: Exception) {
            _hotspotState.value = HotspotState.ERROR
            onError("Legacy hotspot error: ${e.message}")
        }
    }

    fun stopHotspot() {
        _hotspotState.value = HotspotState.STOPPING
        reservation?.close()
        reservation = null
        localOnlyCallback = null
        _hotspotState.value = HotspotState.OFF
    }

    fun isHotspotOn() = _hotspotState.value == HotspotState.ON

    /**
     * Try to determine the hotspot gateway IP by checking common defaults
     * and the wifiManager's DHCP info.
     */
    private fun detectGatewayIp() {
        // Android hotspot gateway is almost always 192.168.43.1
        // but can vary by device. We try DHCP info first.
        try {
            val dhcp = wifiManager.dhcpInfo
            if (dhcp.gateway != 0) {
                val ip = intToIp(dhcp.gateway)
                // gateway from DhcpInfo is the device's upstream gateway, not the hotspot IP
                // The hotspot interface IP is typically 192.168.43.1
            }
        } catch (_: Exception) {}
        // Fallback: read from common interface IPs
        _gatewayIp.value = getHotspotInterfaceIp() ?: "192.168.43.1"
    }

    private fun getHotspotInterfaceIp(): String? {
        return try {
            java.net.NetworkInterface.getNetworkInterfaces()?.toList()
                ?.firstOrNull { iface ->
                    iface.name.startsWith("ap") || iface.name.startsWith("wlan1") ||
                    (iface.name.startsWith("wlan") && !iface.isLoopback)
                }
                ?.inetAddresses?.toList()
                ?.firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
        } catch (_: Exception) { null }
    }

    private fun intToIp(i: Int): String {
        return "${i and 0xff}.${i shr 8 and 0xff}.${i shr 16 and 0xff}.${i shr 24 and 0xff}"
    }
}
