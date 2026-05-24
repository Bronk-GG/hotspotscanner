package com.example.hotspotmonitor.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

private const val TAG = "NetworkScanner"

/**
 * Scans the hotspot subnet (typically 192.168.43.x) for connected devices.
 *
 * Capabilities (no root required):
 *   - Ping sweep to discover live IPs
 *   - Reverse DNS / hostname resolution
 *   - MAC address resolution via InetAddress + ARP table (Android <= 9) or ping-trigger
 *   - mDNS / NSD service discovery
 *   - TCP port scanning of common ports
 *   - MAC OUI vendor lookup
 */
class NetworkScanner(private val context: Context) {

    private val _devices = MutableStateFlow<Map<String, ConnectedDevice>>(emptyMap())
    val devices: StateFlow<Map<String, ConnectedDevice>> = _devices.asStateFlow()

    // Common ports to probe: AirPlay, HTTP, HTTPS, SSH, SMB, mDNS, ADB, Chromecast, etc.
    private val commonPorts = listOf(22, 80, 443, 445, 548, 554, 5555, 5556, 7000, 8080, 8443, 8888, 9100)

    private val serviceTypes = listOf(
        "_http._tcp.",
        "_https._tcp.",
        "_airplay._tcp.",
        "_raop._tcp.",
        "_spotify-connect._tcp.",
        "_googlecast._tcp.",
        "_smb._tcp.",
        "_afpovertcp._tcp.",
        "_ssh._tcp.",
        "_printer._tcp.",
        "_ipp._tcp.",
        "_androidtvremote._tcp.",
        "_sleep-proxy._udp.",
        "_daap._tcp.",
    )

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    // Active mDNS listeners keyed by service type
    private val activeListeners = mutableMapOf<String, NsdManager.DiscoveryListener>()

    /** Perform a single full scan pass of the subnet. Call this in a loop. */
    suspend fun scanSubnet(gatewayIp: String) = withContext(Dispatchers.IO) {
        val prefix = gatewayIp.substringBeforeLast(".")
        val now = System.currentTimeMillis()

        // Ping all 254 addresses in parallel (coroutines on IO dispatcher)
        val jobs = (1..254).map { host ->
            val ip = "$prefix.$host"
            async {
                try {
                    val addr = InetAddress.getByName(ip)
                    val reachable = addr.isReachable(800)
                    if (reachable) {
                        val hostname = resolveHostname(ip)
                        val mac = readMacFromArp(ip) ?: "00:00:00:00:00:00"
                        val vendor = OuiDatabase.lookup(mac)
                        val (osGuess, deviceType) = guessOsAndType(vendor, hostname, emptyList())

                        val existing = _devices.value[ip]
                        val device = existing?.copy(
                            hostname = if (hostname != ip) hostname else existing.hostname,
                            vendor = if (vendor != "Unknown") vendor else existing.vendor,
                            osGuess = if (osGuess != OsType.UNKNOWN) osGuess else existing.osGuess,
                            deviceType = if (deviceType != DeviceType.UNKNOWN) deviceType else existing.deviceType,
                            lastSeen = now,
                            isOnline = true,
                        ) ?: ConnectedDevice(
                            ip = ip,
                            mac = mac,
                            hostname = hostname,
                            vendor = vendor,
                            osGuess = osGuess,
                            deviceType = deviceType,
                            firstSeen = now,
                            lastSeen = now,
                        )
                        _devices.update { it + (ip to device) }

                        // Port scan then re-evaluate device type with port info
                        val openPorts = scanPorts(ip)
                        _devices.update { map ->
                            map[ip]?.let { d ->
                                val (newOs, newType) = guessOsAndType(d.vendor, d.hostname, openPorts)
                                map + (ip to d.copy(
                                    openPorts = openPorts,
                                    osGuess = if (newOs != OsType.UNKNOWN) newOs else d.osGuess,
                                    deviceType = if (newType != DeviceType.UNKNOWN) newType else d.deviceType,
                                ))
                            } ?: map
                        }
                    } else {
                        // Mark existing device offline if it was online
                        val existing = _devices.value[ip]
                        if (existing != null && existing.isOnline) {
                            _devices.update { it + (ip to existing.copy(isOnline = false, lastSeen = now)) }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error scanning $ip: ${e.message}")
                }
            }
        }
        jobs.awaitAll()
    }

    /** Resolve a human-readable hostname for an IP. */
    private fun resolveHostname(ip: String): String {
        return try {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName
            if (canonical == ip) ip else canonical
        } catch (e: Exception) {
            ip
        }
    }

    /** Read the MAC address from the system ARP table at /proc/net/arp. */
    private fun readMacFromArp(ip: String): String? {
        return try {
            java.io.File("/proc/net/arp").readLines()
                .drop(1) // skip header
                .firstOrNull { it.startsWith(ip) || it.contains(" $ip ") }
                ?.split("\\s+".toRegex())
                ?.getOrNull(3)
                ?.takeIf { it != "00:00:00:00:00:00" }
        } catch (e: Exception) {
            null
        }
    }

    /** Scan common TCP ports on a host. Returns list of open ports. */
    private fun scanPorts(ip: String): List<Int> {
        val open = mutableListOf<Int>()
        for (port in commonPorts) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 300)
                    open.add(port)
                }
            } catch (_: Exception) { /* closed or filtered */ }
        }
        return open
    }

    /** Heuristic OS + device type detection from vendor, hostname, and open ports. */
    private fun guessOsAndType(vendor: String, hostname: String, openPorts: List<Int>): Pair<OsType, DeviceType> {
        val v = vendor.lowercase()
        val h = hostname.lowercase()
        val hasPrinterPort = openPorts.any { it == 9100 || it == 515 }
        val hasMediaPort = openPorts.any { it == 554 || it == 7000 || it == 8008 }
        val hasSsh = openPorts.contains(22)
        val hasSmb = openPorts.contains(445)
        val hasAdb = openPorts.any { it == 5555 || it == 5556 }

        return when {
            // Printer
            hasPrinterPort || h.contains("print") || v.contains("canon") || v.contains("epson") ||
            v.contains("hp inc") || v.contains("brother") || v.contains("lexmark") ->
                Pair(OsType.PRINTER, DeviceType.PRINTER)

            // Router / gateway
            h.contains("router") || h.contains("gateway") || h.contains("fritzbox") ||
            v.contains("cisco") || v.contains("tp-link") || v.contains("netgear") ||
            v.contains("asus") && h.contains("router") || v.contains("mikrotik") ->
                Pair(OsType.ROUTER, DeviceType.ROUTER)

            // Smart TV / media
            hasMediaPort || h.contains("chromecast") || h.contains("appletv") || h.contains("roku") ||
            h.contains("firetv") || v.contains("roku") || v.contains("vizio") ||
            v.contains("samsung electronics") && h.contains("tv") ->
                Pair(OsType.SMART_TV, DeviceType.TV)

            // Android TV
            hasAdb && (h.contains("tv") || h.contains("shield") || v.contains("nvidia")) ->
                Pair(OsType.ANDROID_TV, DeviceType.TV)

            // macOS (Mac laptops/desktops)
            v.contains("apple") && (h.contains("macbook") || h.contains("imac") || h.contains("mac-mini") || hasSsh || hasSmb) ->
                Pair(OsType.MACOS, DeviceType.PC)

            // iPhone / iPad
            v.contains("apple") || h.contains("iphone") || h.contains("ipad") ->
                Pair(OsType.IOS, DeviceType.PHONE)

            // Android phone (ADB port = likely phone/tablet)
            hasAdb || v.contains("samsung") || v.contains("xiaomi") || v.contains("huawei") ||
            v.contains("oneplus") || h.contains("android") ->
                Pair(OsType.ANDROID_PHONE, DeviceType.PHONE)

            // Windows PC
            hasSmb || h.contains("windows") || h.contains("desktop") || h.contains("laptop") ||
            h.contains("pc") || v.contains("intel") || v.contains("realtek") ->
                Pair(OsType.WINDOWS_PC, DeviceType.PC)

            // Linux (SSH but no other strong signals)
            hasSsh ->
                Pair(OsType.LINUX, DeviceType.PC)

            else -> Pair(OsType.UNKNOWN, DeviceType.UNKNOWN)
        }
    }

    /** Start mDNS service discovery for all service types. */
    fun startMdnsDiscovery() {
        serviceTypes.forEach { serviceType ->
            if (activeListeners.containsKey(serviceType)) return@forEach
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(s: String, e: Int) {}
                override fun onStopDiscoveryFailed(s: String, e: Int) {}
                override fun onDiscoveryStarted(s: String) {}
                override fun onDiscoveryStopped(s: String) {}
                override fun onServiceLost(info: NsdServiceInfo) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(i: NsdServiceInfo, e: Int) {}
                        override fun onServiceResolved(i: NsdServiceInfo) {
                            val ip = i.host?.hostAddress ?: return
                            val serviceName = "${i.serviceType} (${i.serviceName})"
                            _devices.update { map ->
                                val dev = map[ip] ?: return@update map
                                val services = (dev.services + serviceName).distinct()
                                map + (ip to dev.copy(
                                    hostname = if (i.serviceName.isNotBlank() && dev.hostname == ip)
                                        i.serviceName else dev.hostname,
                                    services = services,
                                ))
                            }
                        }
                    })
                }
            }
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                activeListeners[serviceType] = listener
            } catch (e: Exception) {
                Log.w(TAG, "mDNS discovery failed for $serviceType: ${e.message}")
            }
        }
    }

    /** Stop all mDNS discovery listeners. */
    fun stopMdnsDiscovery() {
        activeListeners.values.forEach { listener ->
            try { nsdManager.stopServiceDiscovery(listener) } catch (_: Exception) {}
        }
        activeListeners.clear()
    }

    /** Clear the device list. */
    fun clearDevices() {
        _devices.value = emptyMap()
    }
}
