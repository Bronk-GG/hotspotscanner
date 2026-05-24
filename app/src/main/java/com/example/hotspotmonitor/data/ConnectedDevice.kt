package com.example.hotspotmonitor.data

/**
 * Represents a device discovered on the network.
 */
data class ConnectedDevice(
    /** IP address on the subnet, e.g. 192.168.1.5 */
    val ip: String,
    /** MAC address in lowercase colon-separated form, e.g. a4:c3:f0:11:22:33 */
    val mac: String,
    /** Human-readable hostname resolved via reverse-DNS or mDNS */
    val hostname: String,
    /** Hardware vendor resolved from the MAC OUI prefix, e.g. "Apple Inc." */
    val vendor: String,
    /** Heuristic OS guess based on open ports, mDNS services, and MAC OUI */
    val osGuess: OsType,
    /** Whether this is a phone, PC, printer, etc. */
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    /** mDNS / NSD service types advertised by this device */
    val services: List<String> = emptyList(),
    /** Open TCP ports found during the port scan */
    val openPorts: List<Int> = emptyList(),
    /** Bytes sent from this device (upstream) since it connected */
    val bytesSent: Long = 0L,
    /** Bytes received by this device (downstream) since it connected */
    val bytesReceived: Long = 0L,
    /** Epoch millis when the device first appeared on the network */
    val firstSeen: Long = System.currentTimeMillis(),
    /** Epoch millis of the most recent ARP/ping confirmation */
    val lastSeen: Long = System.currentTimeMillis(),
    /** Whether the device is currently reachable */
    val isOnline: Boolean = true,
)

enum class OsType(val label: String, val emoji: String) {
    IOS("iPhone / iPad", "📱"),
    MACOS("macOS", "💻"),
    ANDROID_PHONE("Android Phone", "📱"),
    ANDROID_TV("Android TV", "📺"),
    WINDOWS_PC("Windows PC", "🖥️"),
    LINUX("Linux", "🐧"),
    ROUTER("Router / Gateway", "📡"),
    PRINTER("Printer", "🖨️"),
    SMART_TV("Smart TV", "📺"),
    UNKNOWN("Unknown Device", "❓"),
}

enum class DeviceType(val label: String, val icon: String) {
    PHONE("Phone", "📱"),
    PC("PC / Laptop", "💻"),
    TV("TV / Media", "📺"),
    PRINTER("Printer", "🖨️"),
    ROUTER("Router", "📡"),
    TABLET("Tablet", "📟"),
    UNKNOWN("Unknown", "❓"),
}
