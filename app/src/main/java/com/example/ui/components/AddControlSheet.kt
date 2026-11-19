package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.LumaSettings
import com.example.data.model.ThirdPartyAppControl
import com.example.data.model.TileKind
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaEmerald
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface

@Composable
fun AddControlSheet(
    settings: LumaSettings,
    thirdPartyControls: List<ThirdPartyAppControl>,
    onAddSystemTile: (TileKind) -> Unit,
    onAddThirdPartyTile: (ThirdPartyAppControl) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val systemKinds = remember {
        TileKind.entries.filter { it != TileKind.THIRD_PARTY_APP }
    }
    val filteredSystem = remember(searchQuery) {
        if (searchQuery.isBlank()) systemKinds
        else systemKinds.filter {
            it.defaultTitle.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredApps = remember(searchQuery, thirdPartyControls) {
        if (searchQuery.isBlank()) thirdPartyControls
        else thirdPartyControls.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClose() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        val sheetCorner = settings.cornerRoundnessDp.dp
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(580.dp)
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(sheetCorner),
                    cornerRadius = sheetCorner,
                    opacityOverride = (settings.glassOpacity + 0.18f).coerceAtMost(0.82f)
                )
                .clickable(enabled = false) {}
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add a Control",
                        style = MaterialTheme.typography.titleLarge,
                        color = FrostedLightText
                    )
                    Text(
                        text = "System modules or third-party app toggles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryGlassText
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .testTag("close_add_control")
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = FrostedLightText)
                }
            }

            // Tab Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("System Controls", "App Toggles (${thirdPartyControls.size})").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("add_tab_$idx")
                            .liquidGlassSurface(
                                settings = settings,
                                shape = RoundedCornerShape(18.dp),
                                cornerRadius = 18.dp,
                                isActive = selectedTab == idx,
                                activeColor = LumaAzure
                            )
                            .clickable { selectedTab = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = FrostedLightText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Type control, app, or package...", color = SecondaryGlassText) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = SecondaryGlassText) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = SecondaryGlassText,
                            modifier = Modifier.clickable { searchQuery = "" }
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
                modifier = Modifier.fillMaxWidth().testTag("search_controls_input")
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    items(filteredSystem, key = { it.name }) { kind ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassSurface(
                                    settings = settings,
                                    shape = RoundedCornerShape(20.dp),
                                    cornerRadius = 20.dp
                                )
                                .clickable {
                                    onAddSystemTile(kind)
                                    onClose()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(kind.defaultColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = resolveTileIcon(kind, isActive = true),
                                    contentDescription = kind.defaultTitle,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = kind.defaultTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FrostedLightText
                                )
                                Text(
                                    text = "Default size: ${kind.defaultSpanCols}×${kind.defaultSpanRows}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryGlassText
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add ${kind.defaultTitle}",
                                tint = LumaAzure
                            )
                        }
                    }
                } else {
                    items(filteredApps, key = { it.uniqueKey }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassSurface(
                                    settings = settings,
                                    shape = RoundedCornerShape(20.dp),
                                    cornerRadius = 20.dp
                                )
                                .clickable {
                                    onAddThirdPartyTile(app)
                                    onClose()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (app.icon != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = app.icon),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LumaEmerald)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FrostedLightText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (app.supportsQuickSettingsTile) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(LumaEmerald.copy(alpha = 0.25f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "QS Tile",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LumaEmerald
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryGlassText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Pin ${app.label}",
                                tint = LumaEmerald
                            )
                        }
                    }
                    if (filteredApps.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "No matching app toggles found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SecondaryGlassText
                            )
                        }
                    }
                }
            }
        }
    }
}
