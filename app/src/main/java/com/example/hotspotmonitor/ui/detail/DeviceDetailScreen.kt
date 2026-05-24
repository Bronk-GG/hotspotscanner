package com.example.hotspotmonitor.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hotspotmonitor.data.ConnectedDevice
import com.example.hotspotmonitor.theme.Amber
import com.example.hotspotmonitor.theme.Background
import com.example.hotspotmonitor.theme.BorderColor
import com.example.hotspotmonitor.theme.CardBackground
import com.example.hotspotmonitor.theme.Cyan
import com.example.hotspotmonitor.theme.CyanGlow
import com.example.hotspotmonitor.theme.GreenGlow
import com.example.hotspotmonitor.theme.GreenOnline
import com.example.hotspotmonitor.theme.RedAlert
import com.example.hotspotmonitor.theme.SurfaceVariant
import com.example.hotspotmonitor.theme.TextMuted
import com.example.hotspotmonitor.theme.TextPrimary
import com.example.hotspotmonitor.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(device: ConnectedDevice, onBack: () -> Unit) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = device.hostname.ifBlank { device.ip },
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Cyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero card
            DeviceHeroCard(device)
            // Network info
            SectionCard(title = "NETWORK") {
                InfoRow("IP Address", device.ip, Cyan)
                InfoRow("MAC Address", device.mac.uppercase(), TextSecondary)
                InfoRow("Status", if (device.isOnline) "Online" else "Offline",
                    if (device.isOnline) GreenOnline else RedAlert)
                InfoRow("First Seen", formatTime(device.firstSeen), TextSecondary)
                InfoRow("Last Seen", formatTime(device.lastSeen), TextSecondary)
                InfoRow("Session", formatDuration(device.lastSeen - device.firstSeen), Amber)
            }
            // Data usage
            SectionCard(title = "DATA USAGE") {
                DataUsageBar(
                    label = "↑ Upload",
                    bytes = device.bytesSent,
                    color = Cyan,
                )
                Spacer(Modifier.height(8.dp))
                DataUsageBar(
                    label = "↓ Download",
                    bytes = device.bytesReceived,
                    color = GreenOnline,
                )
            }
            // Open ports
            if (device.openPorts.isNotEmpty()) {
                SectionCard(title = "OPEN PORTS") {
                    device.openPorts.forEach { port ->
                        PortRow(port)
                    }
                }
            }
            // mDNS services
            if (device.services.isNotEmpty()) {
                SectionCard(title = "ADVERTISED SERVICES") {
                    device.services.forEach { service ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(6.dp).clip(CircleShape).background(Amber),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(service, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DeviceHeroCard(device: ConnectedDevice) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (device.isOnline) GreenGlow else BorderColor,
                        CardBackground,
                    ),
                    radius = 400f,
                ),
            )
            .border(1.dp, if (device.isOnline) GreenGlow else BorderColor, RoundedCornerShape(20.dp))
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .border(2.dp, if (device.isOnline) GreenOnline else BorderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(device.osGuess.emoji, fontSize = 36.sp)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    text = device.hostname.ifBlank { device.ip },
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = device.vendor,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = device.osGuess.label,
                    style = MaterialTheme.typography.labelLarge.copy(color = Cyan),
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(color = Cyan, letterSpacing = 3.sp),
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(color = valueColor))
    }
}

@Composable
private fun DataUsageBar(label: String, bytes: Long, color: androidx.compose.ui.graphics.Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
            Text(formatBytes(bytes), style = MaterialTheme.typography.labelLarge.copy(color = color))
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(minOf(1f, bytes / 10_000_000f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun PortRow(port: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(GreenOnline),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = portNameFull(port),
            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ":$port",
            style = MaterialTheme.typography.labelLarge.copy(color = Amber),
        )
    }
}

private fun portNameFull(port: Int) = when (port) {
    22 -> "SSH / Secure Shell"
    80 -> "HTTP / Web Server"
    443 -> "HTTPS / Secure Web"
    445 -> "SMB / File Sharing"
    548 -> "AFP / Apple File Protocol"
    554 -> "RTSP / Media Stream"
    5555 -> "ADB / Android Debug Bridge"
    5556 -> "ADB / Android Debug Bridge"
    7000 -> "AirPlay / Apple Streaming"
    8080 -> "HTTP Alternate"
    8443 -> "HTTPS Alternate"
    8888 -> "HTTP Dev Server"
    9100 -> "JetDirect / Printing"
    else -> "Unknown Service"
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatTime(epochMs: Long): String {
    return SimpleDateFormat("HH:mm:ss · dd MMM", Locale.getDefault()).format(Date(epochMs))
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}
