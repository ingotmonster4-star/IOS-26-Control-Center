package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LumaSettings
import com.example.data.model.SystemLiveState
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LumaAmber
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.LumaEmerald
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface

@Composable
fun ConnectivityExpandedSheet(
    settings: LumaSettings,
    liveState: SystemLiveState,
    onToggleAirplane: () -> Unit,
    onToggleCellular: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleHotspot: () -> Unit,
    onToggleLocation: () -> Unit,
    onToggleNfc: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val sheetCorner = settings.cornerRoundnessDp.dp
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(sheetCorner),
                    cornerRadius = sheetCorner,
                    opacityOverride = (settings.glassOpacity + 0.14f).coerceAtMost(0.75f)
                )
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connectivity",
                    style = MaterialTheme.typography.titleLarge,
                    color = FrostedLightText
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .testTag("close_connectivity_sheet")
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close connectivity drawer",
                        tint = FrostedLightText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val items = listOf(
                ConnectivityItemData("Airplane Mode", if (liveState.isAirplaneModeOn) "On" else "Off", liveState.isAirplaneModeOn, LumaAmber, Icons.Rounded.AirplanemodeActive, onToggleAirplane),
                ConnectivityItemData("Mobile Data", liveState.cellularLabel, liveState.isMobileDataEnabled, LumaEmerald, Icons.Rounded.SignalCellularAlt, onToggleCellular),
                ConnectivityItemData("Wi-Fi", liveState.wifiLabel, liveState.isWifiEnabled, LumaAzure, Icons.Rounded.Wifi, onToggleWifi),
                ConnectivityItemData("Bluetooth", liveState.bluetoothLabel, liveState.isBluetoothEnabled, LumaAzure, Icons.Rounded.Bluetooth, onToggleBluetooth),
                ConnectivityItemData("Hotspot", "Tethering", false, LumaEmerald, Icons.Rounded.WifiTethering, onToggleHotspot),
                ConnectivityItemData("Location", if (liveState.isLocationEnabled) "Active" else "Off", liveState.isLocationEnabled, LumaCyan, Icons.Rounded.LocationOn, onToggleLocation),
                ConnectivityItemData("NFC", if (liveState.isNfcEnabled) "Ready" else "Off", liveState.isNfcEnabled, LumaEmerald, Icons.Rounded.Nfc, onToggleNfc)
            )

            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        ConnectivityExpandedButton(
                            item = item,
                            settings = settings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class ConnectivityItemData(
    val title: String,
    val status: String,
    val isActive: Boolean,
    val activeColor: Color,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun ConnectivityExpandedButton(
    item: ConnectivityItemData,
    settings: LumaSettings,
    modifier: Modifier = Modifier
) {
    val btnCorner = (settings.cornerRoundnessDp * 0.8f).dp
    Row(
        modifier = modifier
            .height(68.dp)
            .liquidGlassSurface(
                settings = settings,
                shape = RoundedCornerShape(btnCorner),
                cornerRadius = btnCorner,
                isActive = false
            )
            .clickable { item.onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (item.isActive) item.activeColor else Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                color = FrostedLightText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.status,
                style = MaterialTheme.typography.labelMedium,
                color = if (item.isActive) item.activeColor else SecondaryGlassText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
