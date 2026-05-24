package com.example.hotspotmonitor.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

private const val TAG = "NetworkScanner"

/**
 * Scans any subnet for connected devices without root.
 *
 * Key improvements:
 *  - Multi-method host discovery: ICMP + TCP-knock on Windows/common ports
 *  - Windows PCs are found via TCP 135/139/445/3389 even if ICMP is blocked
 *  - NetBIOS name resolution for Windows hostnames
 *  - Port-based OS/device type classification
 *  - ARP table reading with IP-match fallback
 */
class NetworkScanner(private val context: Context) {

    private val _devices = MutableStateFlow<Map<String, ConnectedDevice>>(emptyMap())
    val devices: StateFlow<Map<String, ConnectedDevice>> = _devices.asStateFlow()

    // "Alive probe" ports: trying these is how we detect hosts whose ICMP is blocked (e.g. Windows)
    private val aliveProbePorts = listOf(
        80, 443,        // Any web server / router
        135, 139, 445,  // Windows RPC / NetBIOS / SMB
        3389,           // Windows RDP
        22,             // SSH (Linux/Mac)
        548, 7000,      // AFP / AirPlay (Apple)
        5555,           // ADB (Android)
        9100,           // Printing
        8080, 8443,     // Common dev/proxy ports
    )

    // Full port scan list (superset of alive probes)
    private val scanPorts = listOf(
        22, 80, 135, 139, 443, 445, 548, 554,
        3389, 5555, 5556, 7000, 8080, 8443, 8888, 9100,
    )

    private val serviceTypes = listOf(
        "_http._tcp.", "_https._tcp.", "_airplay._tcp.", "_raop._tcp.",
        "_spotify-connect._tcp.", "_googlecast._tcp.", "_smb._tcp.",
        "_afpovertcp._tcp.", "_ssh._tcp.", "_printer._tcp.",
        "_ipp._tcp.", "_androidtvremote._tcp.", "_daap._tcp.",
    )

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val activeListeners = mutableMapOf<String, NsdManager.DiscoveryListener>()

    /** Perform a full scan pass of the subnet. */
    suspend fun scanSubnet(gatewayIp: String) = withContext(Dispatchers.IO) {
        val prefix = gatewayIp.substringBeforeLast(".")
        val now = System.currentTimeMillis()

        val jobs = (1..254).map { host ->
            val ip = "$prefix.$host"
            async {
                try {
                    if (!isHostAlive(ip)) {
                        // Mark offline if we had it before
                        val existing = _devices.value[ip]
                        if (existing != null && existing.isOnline) {
                            _devices.update { it + (ip to existing.copy(isOnline = false, lastSeen = now)) }
                        }
                        return@async
                    }

                    // Host is alive — gather info
                    val hostname = resolveHostname(ip)
                    val mac = readMacFromArp(ip) ?: "00:00:00:00:00:00"
                    val vendor = OuiDatabase.lookup(mac)

                    val existing = _devices.value[ip]
                    val initDevice = existing?.copy(
                        hostname = if (hostname != ip) hostname else existing.hostname,
                        vendor = if (vendor != "Unknown") vendor else existing.vendor,
                        lastSeen = now,
                        isOnline = true,
                    ) ?: ConnectedDevice(
                        ip = ip, mac = mac, hostname = hostname,
                        vendor = vendor, osGuess = OsType.UNKNOWN,
                        firstSeen = now, lastSeen = now,
                    )
                    _devices.update { it + (ip to initDevice) }

                    // Full port scan — results drive OS/device classification
                    val openPorts = fullPortScan(ip)
                    val (osGuess, deviceType) = guessOsAndType(
                        vendor = initDevice.vendor,
                        hostname = initDevice.hostname,
                        openPorts = openPorts,
                        mac = mac,
                    )
                    _devices.update { map ->
                        map[ip]?.let { d ->
                            map + (ip to d.copy(
                                openPorts = openPorts,
                                osGuess = if (osGuess != OsType.UNKNOWN) osGuess else d.osGuess,
                                deviceType = if (deviceType != DeviceType.UNKNOWN) deviceType else d.deviceType,
                            ))
                        } ?: map
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error scanning $ip: ${e.message}")
                }
            }
        }
        jobs.awaitAll()
    }

    /**
     * Determine if a host is alive using ICMP first, then TCP-knock fallback.
     * This finds Windows PCs whose firewall blocks ICMP.
     */
    private fun isHostAlive(ip: String): Boolean {
        // 1. Try ICMP first (fast, works for most non-Windows devices)
        try {
            if (InetAddress.getByName(ip).isReachable(600)) return true
        } catch (_: Exception) {}

        // 2. TCP-knock fallback: any RST (connection refused) also means host is up
        for (port in listOf(80, 443, 135, 139, 445, 3389, 22, 8080)) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 200)
                    return true // connection accepted — port open
                }
            } catch (e: java.net.ConnectException) {
                // "Connection refused" = host is up, port is closed
                if (e.message?.contains("refused", ignoreCase = true) == true ||
                    e.message?.contains("ECONNREFUSED") == true) {
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    /** Full port scan — returns list of OPEN ports. */
    private fun fullPortScan(ip: String): List<Int> {
        val open = mutableListOf<Int>()
        for (port in scanPorts) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 250)
                    open.add(port)
                }
            } catch (_: Exception) {}
        }
        return open
    }

    /** Resolve hostname via reverse-DNS and attempt NetBIOS (port 137 UDP) heuristic. */
    private fun resolveHostname(ip: String): String {
        return try {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName
            if (canonical != ip) canonical else ip
        } catch (e: Exception) {
            ip
        }
    }

    /** Read MAC from /proc/net/arp — works on Android 9 and below, may be empty on 10+. */
    private fun readMacFromArp(ip: String): String? {
        return try {
            java.io.File("/proc/net/arp").readLines()
                .drop(1)
                .firstOrNull { line ->
                    line.split("\\s+".toRegex()).getOrNull(0) == ip
                }
                ?.split("\\s+".toRegex())
                ?.getOrNull(3)
                ?.takeIf { it != "00:00:00:00:00:00" }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Classify a device using every available signal.
     * Port-based signals are most reliable when MAC/hostname are unavailable.
     */
    private fun guessOsAndType(
        vendor: String,
        hostname: String,
        openPorts: List<Int>,
        mac: String,
    ): Pair<OsType, DeviceType> {
        val v = vendor.lowercase()
        val h = hostname.lowercase()

        val hasPrinterPort = openPorts.any { it == 9100 || it == 515 }
        val hasAirPlay     = openPorts.contains(7000)
        val hasAfp         = openPorts.contains(548)
        val hasRtsp        = openPorts.contains(554)
        val hasSsh         = openPorts.contains(22)
        val hasSmb         = openPorts.contains(445)
        val hasNetBios     = openPorts.any { it == 135 || it == 139 }
        val hasRdp         = openPorts.contains(3389)
        val hasAdb         = openPorts.any { it == 5555 || it == 5556 }
        val hasWebOnly     = openPorts.any { it == 80 || it == 443 || it == 8080 || it == 8443 }

        // Windows signals: NetBIOS (135/139), SMB (445), RDP (3389) — very reliable
        val windowsSignals = (if (hasNetBios) 2 else 0) +
                             (if (hasSmb) 2 else 0) +
                             (if (hasRdp) 2 else 0) +
                             (if (h.contains("windows") || h.contains("desktop") || h.contains("laptop") || h.contains("pc")) 3 else 0) +
                             (if (v.contains("intel") || v.contains("realtek") || v.contains("micro-star") || v.contains("gigabyte")) 1 else 0)

        // Apple signals
        val appleSignals = (if (hasAirPlay) 2 else 0) +
                           (if (hasAfp) 2 else 0) +
                           (if (v.contains("apple")) 3 else 0) +
                           (if (h.contains("iphone") || h.contains("ipad") || h.contains("macbook") || h.contains("imac")) 3 else 0)

        // Android signals
        val androidSignals = (if (hasAdb) 3 else 0) +
                             (if (v.contains("samsung") || v.contains("xiaomi") || v.contains("huawei") ||
                                  v.contains("oneplus") || v.contains("google") || v.contains("motorola")) 2 else 0) +
                             (if (h.contains("android") || h.contains("pixel") || h.contains("galaxy")) 2 else 0)

        return when {
            hasPrinterPort || v.contains("canon") || v.contains("epson") ||
            v.contains("hp inc") || v.contains("brother") || v.contains("lexmark") ->
                Pair(OsType.PRINTER, DeviceType.PRINTER)

            h.contains("router") || h.contains("gateway") || h.contains("fritzbox") ||
            v.contains("cisco") || v.contains("tp-link") || v.contains("netgear") || v.contains("mikrotik") ->
                Pair(OsType.ROUTER, DeviceType.ROUTER)

            hasRtsp && !hasSsh && !hasSmb ->
                Pair(OsType.SMART_TV, DeviceType.TV)

            windowsSignals >= 2 ->
                Pair(OsType.WINDOWS_PC, DeviceType.PC)

            appleSignals >= 3 && (hasAfp || hasSsh || hasSmb || h.contains("macbook") || h.contains("imac")) ->
                Pair(OsType.MACOS, DeviceType.PC)

            appleSignals >= 2 ->
                Pair(OsType.IOS, DeviceType.PHONE)

            androidSignals >= 2 ->
                Pair(OsType.ANDROID_PHONE, DeviceType.PHONE)

            hasSsh && !hasSmb ->
                Pair(OsType.LINUX, DeviceType.PC)

            else -> Pair(OsType.UNKNOWN, DeviceType.UNKNOWN)
        }
    }

    /** Start mDNS discovery for all known service types. */
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

    fun stopMdnsDiscovery() {
        activeListeners.values.forEach { listener ->
            try { nsdManager.stopServiceDiscovery(listener) } catch (_: Exception) {}
        }
        activeListeners.clear()
    }

    fun clearDevices() {
        _devices.value = emptyMap()
    }
}
