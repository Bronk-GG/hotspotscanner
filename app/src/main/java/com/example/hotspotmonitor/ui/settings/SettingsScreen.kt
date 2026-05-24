package com.example.hotspotmonitor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hotspotmonitor.theme.Background
import com.example.hotspotmonitor.theme.BorderColor
import com.example.hotspotmonitor.theme.CardBackground
import com.example.hotspotmonitor.theme.Cyan
import com.example.hotspotmonitor.theme.TextMuted
import com.example.hotspotmonitor.theme.TextPrimary
import com.example.hotspotmonitor.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "ABOUT",
                    style = MaterialTheme.typography.labelSmall.copy(color = Cyan, letterSpacing = 3.sp),
                )
                Text(
                    "NetWatch passively scans any Wi-Fi or hotspot network you are connected to. No root access is required.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                )
                Text(
                    "Discoverable information includes device IP, MAC address, hardware vendor, open ports, and advertised network services (mDNS).",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "LIMITATIONS",
                    style = MaterialTheme.typography.labelSmall.copy(color = Cyan, letterSpacing = 3.sp),
                )
                Text(
                    "• HTTPS/DNS traffic cannot be intercepted on non-rooted devices.\n" +
                    "• MAC addresses may not be readable on Android 10+ for remote devices.\n" +
                    "• Some devices block ICMP ping and may not appear in scans.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                )
            }
        }
    }
}
