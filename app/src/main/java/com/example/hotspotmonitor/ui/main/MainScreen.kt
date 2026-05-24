package com.example.hotspotmonitor.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hotspotmonitor.data.ConnectedDevice
import com.example.hotspotmonitor.data.HotspotState
import com.example.hotspotmonitor.data.OsType
import com.example.hotspotmonitor.theme.Amber
import com.example.hotspotmonitor.theme.AmberGlow
import com.example.hotspotmonitor.theme.Background
import com.example.hotspotmonitor.theme.BorderColor
import com.example.hotspotmonitor.theme.CardBackground
import com.example.hotspotmonitor.theme.Cyan
import com.example.hotspotmonitor.theme.CyanGlow
import com.example.hotspotmonitor.theme.GreenGlow
import com.example.hotspotmonitor.theme.GreenOnline
import com.example.hotspotmonitor.theme.RedAlert
import com.example.hotspotmonitor.theme.RedGlow
import com.example.hotspotmonitor.theme.SurfaceVariant
import com.example.hotspotmonitor.theme.TextMuted
import com.example.hotspotmonitor.theme.TextPrimary
import com.example.hotspotmonitor.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onDeviceClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val hotspotState by viewModel.hotspotState.collectAsStateWithLifecycle()
    val config by viewModel.hotspotConfig.collectAsStateWithLifecycle()
    val gatewayIp by viewModel.gatewayIp.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = RedAlert,
                    contentColor = TextPrimary,
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "NET",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Cyan, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp,
                            ),
                        )
                        Text(
                            "WATCH",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TextPrimary, fontWeight = FontWeight.Light, letterSpacing = 4.sp,
                            ),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hotspot control card
            item {
                HotspotControlCard(
                    hotspotState = hotspotState,
                    ssid = config.ssid,
                    password = config.password,
                    gatewayIp = gatewayIp,
                    onToggle = { viewModel.toggleHotspot() },
                )
            }

            // Stats bar
            if (devices.isNotEmpty()) {
                item {
                    StatsBar(
                        total = devices.size,
                        online = devices.count { it.isOnline },
                    )
                }
            }

            // Device list
            if (devices.isEmpty() && hotspotState == HotspotState.ON) {
                item { EmptyDevicesCard() }
            }

            items(devices, key = { it.ip }) { device ->
                DeviceCard(
                    device = device,
                    onClick = { onDeviceClick(device.ip) },
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HotspotControlCard(
    hotspotState: HotspotState,
    ssid: String,
    password: String,
    gatewayIp: String,
    onToggle: () -> Unit,
) {
    val isOn = hotspotState == HotspotState.ON
    val isTransitioning = hotspotState == HotspotState.STARTING || hotspotState == HotspotState.STOPPING

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    val accentColor by animateColorAsState(
        targetValue = when (hotspotState) {
            HotspotState.ON -> GreenOnline
            HotspotState.ERROR -> RedAlert
            HotspotState.STARTING, HotspotState.STOPPING -> Amber
            else -> TextMuted
        },
        label = "accentColor",
    )
    val glowColor by animateColorAsState(
        targetValue = when (hotspotState) {
            HotspotState.ON -> GreenGlow
            HotspotState.ERROR -> RedGlow
            HotspotState.STARTING, HotspotState.STOPPING -> AmberGlow
            else -> Color.Transparent
        },
        label = "glowColor",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceVariant, CardBackground),
                    ),
                )
                .border(1.dp, glowColor, RoundedCornerShape(20.dp)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "HOTSPOT",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, letterSpacing = 3.sp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (hotspotState) {
                                HotspotState.ON -> "● BROADCASTING"
                                HotspotState.STARTING -> "◌ STARTING..."
                                HotspotState.STOPPING -> "◌ STOPPING..."
                                HotspotState.ERROR -> "✕ ERROR"
                                HotspotState.OFF -> "○ OFFLINE"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isTransitioning) Amber.copy(alpha = pulseAlpha) else accentColor,
                            ),
                        )
                    }
                    Switch(
                        checked = isOn || hotspotState == HotspotState.STARTING,
                        onCheckedChange = { onToggle() },
                        enabled = !isTransitioning,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Background,
                            checkedTrackColor = GreenOnline,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceVariant,
                        ),
                    )
                }

                if (isOn) {
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HotspotInfoItem(label = "SSID", value = ssid)
                        HotspotInfoItem(label = "PASSWORD", value = password)
                        HotspotInfoItem(label = "GATEWAY", value = gatewayIp)
                    }
                }

                if (!isOn && hotspotState == HotspotState.OFF) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Tap the switch to create a WiFi hotspot and start monitoring connected devices.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    )
                }
            }
        }
    }
}

@Composable
private fun HotspotInfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, letterSpacing = 2.sp))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(color = Cyan), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatsBar(total: Int, online: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatItem(label = "TOTAL", value = total.toString(), color = TextPrimary)
        StatItem(label = "ONLINE", value = online.toString(), color = GreenOnline)
        StatItem(label = "OFFLINE", value = (total - online).toString(), color = TextMuted)
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(color = color, fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, letterSpacing = 2.sp))
    }
}

@Composable
private fun EmptyDevicesCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Scanning for devices...", style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary))
            Spacer(Modifier.height(4.dp))
            Text("Devices that connect to your hotspot\nwill appear here automatically.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun DeviceCard(device: ConnectedDevice, onClick: () -> Unit) {
    val onlineColor = if (device.isOnline) GreenOnline else TextMuted
    val borderColor = if (device.isOnline) GreenGlow else BorderColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // OS / vendor icon circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant)
                        .border(1.dp, BorderColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(device.osGuess.emoji, fontSize = 22.sp)
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Device name
                    Text(
                        text = device.hostname.ifBlank { device.ip },
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    // Vendor + OS
                    Text(
                        text = "${device.vendor} · ${device.osGuess.label}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    // IP + MAC
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillLabel(text = device.ip, color = Cyan)
                        if (device.mac != "00:00:00:00:00:00") {
                            PillLabel(text = device.mac.uppercase(), color = TextSecondary)
                        }
                    }

                    // Open ports
                    if (device.openPorts.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            device.openPorts.take(5).forEach { port ->
                                PillLabel(text = portName(port), color = Amber)
                            }
                            if (device.openPorts.size > 5) {
                                PillLabel(text = "+${device.openPorts.size - 5}", color = TextMuted)
                            }
                        }
                    }

                    // Services
                    if (device.services.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = device.services.take(3).joinToString(" · ") { it.substringBefore("._").removePrefix("_") },
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    // Online dot
                    PulsingDot(color = onlineColor, size = 10.dp, active = device.isOnline)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (device.isOnline) "ONLINE" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall.copy(color = onlineColor, letterSpacing = 1.sp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatDuration(System.currentTimeMillis() - device.firstSeen),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(color: Color, size: Dp, active: Boolean) {
    val transition = rememberInfiniteTransition(label = "dotPulse")
    val alpha by transition.animateFloat(
        initialValue = if (active) 0.4f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dotAlpha",
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = if (active) alpha else 0.3f)),
    )
}

@Composable
private fun PillLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(color = color))
    }
}

private fun portName(port: Int) = when (port) {
    22 -> "SSH:22"
    80 -> "HTTP:80"
    443 -> "HTTPS:443"
    445 -> "SMB:445"
    548 -> "AFP:548"
    554 -> "RTSP:554"
    5555 -> "ADB:5555"
    5556 -> "ADB:5556"
    7000 -> "AirPlay:7000"
    8080 -> "HTTP:8080"
    8443 -> "HTTPS:8443"
    8888 -> "HTTP:8888"
    9100 -> "Print:9100"
    else -> ":$port"
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}
