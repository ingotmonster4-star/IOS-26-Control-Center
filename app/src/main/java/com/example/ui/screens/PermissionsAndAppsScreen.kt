package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.model.ControlTile
import com.example.data.model.GlassTintMode
import com.example.data.model.LumaSettings
import com.example.data.model.PermissionHealthState
import com.example.data.model.PermissionType
import com.example.data.model.ThirdPartyAppControl
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.GlassWarningAmber
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaEmerald
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface

@Composable
fun PermissionsAndAppsScreen(
    settings: LumaSettings,
    healthState: PermissionHealthState,
    discoveredApps: List<ThirdPartyAppControl>,
    currentTiles: List<ControlTile>,
    onRequestPermissionSettings: (PermissionType) -> Unit,
    onRefreshPermissions: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    onPinThirdPartyApp: (ThirdPartyAppControl) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    val pinnedKeys = remember(currentTiles) { currentTiles.map { it.tileKey }.toSet() }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshPermissions()
    }

    val filteredApps = remember(discoveredApps, search) {
        if (search.isBlank()) discoveredApps
        else discoveredApps.filter {
            it.label.contains(search, ignoreCase = true) ||
                it.appName.contains(search, ignoreCase = true) ||
                it.packageName.contains(search, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.LumaBackdropLayer(
            settings = settings,
            blurRadiusDp = 0f,
            refractionStrength = 0f,
            isControlCenterOpen = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .liquidGlassSurface(
                                shape = CircleShape,
                                cornerRadius = 24.dp,
                                glassOpacity = settings.glassOpacity,
                                specularStrength = settings.specularStrength,
                                tintMode = settings.tintMode
                            )
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = FrostedLightText)
                    }
                    Text(
                        text = "Apps & Health",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FrostedLightText
                    )
                }

                IconButton(
                    onClick = {
                        onRefreshPermissions()
                        onRefreshDiscovery()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlassSurface(
                            shape = CircleShape,
                            cornerRadius = 24.dp,
                            glassOpacity = settings.glassOpacity,
                            specularStrength = settings.specularStrength,
                            tintMode = settings.tintMode
                        )
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh status", tint = FrostedLightText)
                }
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    "App Toggles (${discoveredApps.size})",
                    "Permissions (${healthState.items.count { it.isGranted }}/${healthState.items.size})"
                ).forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .liquidGlassSurface(
                                shape = RoundedCornerShape(18.dp),
                                cornerRadius = 18.dp,
                                glassOpacity = settings.glassOpacity,
                                specularStrength = settings.specularStrength,
                                tintMode = settings.tintMode,
                                isActive = selectedTab == idx,
                                activeColor = LumaAzure
                            )
                            .clickable { selectedTab = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = FrostedLightText)
                    }
                }
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Type app name, QS tile, or package...", color = SecondaryGlassText) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = SecondaryGlassText) },
                    trailingIcon = if (search.isNotEmpty()) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = SecondaryGlassText,
                                modifier = Modifier.clickable { search = "" }
                            )
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LumaAzure,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedTextColor = FrostedLightText,
                        unfocusedTextColor = FrostedLightText
                    ),
                    modifier = Modifier
                        .widthIn(max = 540.dp)
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    items(filteredApps, key = { it.uniqueKey }) { app ->
                        val isPinned = app.uniqueKey in pinnedKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassSurface(
                                    settings = settings,
                                    shape = RoundedCornerShape(settings.cornerRoundnessDp.dp),
                                    cornerRadius = settings.cornerRoundnessDp.dp
                                )
                                .clickable(enabled = !isPinned) { onPinThirdPartyApp(app) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (app.icon != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = app.icon),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FrostedLightText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (app.supportsQuickSettingsTile) "QS Tile · ${app.appName}" else app.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (app.supportsQuickSettingsTile) LumaEmerald else SecondaryGlassText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isPinned) LumaEmerald.copy(alpha = 0.22f)
                                        else LumaAzure
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Rounded.CheckCircle else Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = if (isPinned) LumaEmerald else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isPinned) "Pinned" else "Add",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isPinned) LumaEmerald else Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(healthState.items, key = { it.type.name }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassSurface(
                                    settings = settings,
                                    shape = RoundedCornerShape(settings.cornerRoundnessDp.dp),
                                    cornerRadius = settings.cornerRoundnessDp.dp
                                )
                                .clickable {
                                    if (item.type == PermissionType.BLUETOOTH_CONNECT ||
                                        item.type == PermissionType.POST_NOTIFICATIONS
                                    ) {
                                        val req = buildList {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                add(Manifest.permission.BLUETOOTH_CONNECT)
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                add(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                        if (req.isNotEmpty()) runtimePermLauncher.launch(req.toTypedArray())
                                    } else {
                                        onRequestPermissionSettings(item.type)
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = if (item.isGranted) LumaEmerald else GlassWarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.type.title, style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                                Text(item.type.reason, style = MaterialTheme.typography.bodyMedium, color = SecondaryGlassText)
                            }
                            Text(
                                text = if (item.isGranted) "Active" else "Enable",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (item.isGranted) LumaEmerald else LumaAzure
                            )
                        }
                    }
                }
            }
        }
    }
}
