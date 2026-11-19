package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.ControlTile
import com.example.data.model.LumaSettings
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaCoral
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface
import kotlin.math.roundToInt

@Composable
fun CustomizeTileDialog(
    tile: ControlTile,
    settings: LumaSettings,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onSave: (ControlTile) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(tile.id) { mutableStateOf(tile.title) }
    var subtitle by remember(tile.id) { mutableStateOf(tile.subtitle) }
    var spanCols by remember(tile.id) { mutableStateOf(tile.spanCols) }
    var spanRows by remember(tile.id) { mutableStateOf(tile.spanRows) }
    var accentHex by remember(tile.id) { mutableLongStateOf(tile.accentColorHex) }
    var customOpacity by remember(tile.id) { mutableStateOf(tile.customOpacity) }
    var opacityInput by remember(tile.id, customOpacity) {
        mutableStateOf(((customOpacity ?: settings.glassOpacity) * 100).roundToInt().toString())
    }

    val colorChoices = listOf(
        0xFF0A84FFL,
        0xFF38BDF8L,
        0xFFFF9F0AL,
        0xFF5E5CE6L,
        0xFF30D158L,
        0xFFFF453AL,
        0xFFFF375FL,
        0xFF8E8E93L
    )

    val sizeChoices = listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 4 to 2)
    val dialogCorner = settings.cornerRoundnessDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onDismiss() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(dialogCorner),
                    cornerRadius = dialogCorner,
                    opacityOverride = (settings.glassOpacity + 0.16f).coerceAtMost(0.84f)
                )
                .clickable(enabled = false) {}
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize Module",
                    style = MaterialTheme.typography.titleLarge,
                    color = FrostedLightText
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .liquidGlassSurface(settings = settings, shape = CircleShape, cornerRadius = 19.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = FrostedLightText)
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 64) title = it },
                label = { Text("Module Title (${title.length}/64)", color = SecondaryGlassText) },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LumaAzure,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                    focusedTextColor = FrostedLightText,
                    unfocusedTextColor = FrostedLightText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subtitle,
                onValueChange = { if (it.length <= 96) subtitle = it },
                label = { Text("Custom Subtitle / Note (${subtitle.length}/96)", color = SecondaryGlassText) },
                placeholder = { Text("Optional status label or shortcut note", color = SecondaryGlassText) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LumaAzure,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                    focusedTextColor = FrostedLightText,
                    unfocusedTextColor = FrostedLightText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Grid Footprint", style = MaterialTheme.typography.labelLarge, color = SecondaryGlassText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizeChoices.forEach { (c, r) ->
                    val selected = spanCols == c && spanRows == r
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .liquidGlassSurface(
                                settings = settings,
                                shape = RoundedCornerShape(14.dp),
                                cornerRadius = 14.dp,
                                isActive = selected,
                                activeColor = LumaAzure
                            )
                            .clickable {
                                spanCols = c
                                spanRows = r
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${c}×${r}", style = MaterialTheme.typography.labelLarge, color = FrostedLightText)
                    }
                }
            }

            // Per-tile individual glass transparency override with direct typing + slider (2%..95%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Module Glass Opacity",
                    style = MaterialTheme.typography.labelLarge,
                    color = SecondaryGlassText
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customOpacity?.let { "${(it * 100).roundToInt()}%" } ?: "Global",
                        style = MaterialTheme.typography.labelLarge,
                        color = LumaCyan
                    )
                    if (customOpacity != null) {
                        TextButton(onClick = { customOpacity = null }) {
                            Text("Reset", color = SecondaryGlassText)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Slider(
                    value = (customOpacity ?: settings.glassOpacity).coerceIn(0.02f, 0.95f),
                    onValueChange = { customOpacity = it },
                    valueRange = 0.02f..0.95f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LumaCyan),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = opacityInput,
                    onValueChange = { txt ->
                        val digits = txt.filter { it.isDigit() }.take(3)
                        opacityInput = digits
                        digits.toIntOrNull()?.let { pct ->
                            customOpacity = (pct / 100f).coerceIn(0.02f, 0.95f)
                        }
                    },
                    singleLine = true,
                    suffix = { Text("%", color = SecondaryGlassText) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LumaCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                        focusedTextColor = FrostedLightText,
                        unfocusedTextColor = FrostedLightText
                    ),
                    modifier = Modifier.widthIn(min = 78.dp, max = 88.dp)
                )
            }

            Text("Active Glow Accent", style = MaterialTheme.typography.labelLarge, color = SecondaryGlassText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colorChoices.forEach { hex ->
                    val selected = accentHex == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .then(
                                if (selected) Modifier.border(2.5.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { accentHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected color",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Text("Position in Canvas", style = MaterialTheme.typography.labelLarge, color = SecondaryGlassText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(16.dp),
                            cornerRadius = 16.dp
                        )
                        .clickable(enabled = canMoveEarlier) { onMoveEarlier() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = FrostedLightText, modifier = Modifier.size(16.dp))
                        Text(" Earlier", color = if (canMoveEarlier) FrostedLightText else SecondaryGlassText)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(16.dp),
                            cornerRadius = 16.dp
                        )
                        .clickable(enabled = canMoveLater) { onMoveLater() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Later ", color = if (canMoveLater) FrostedLightText else SecondaryGlassText)
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = FrostedLightText, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(16.dp),
                            cornerRadius = 16.dp,
                            isActive = true,
                            activeColor = LumaCoral
                        )
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove tile", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(16.dp),
                            cornerRadius = 16.dp,
                            isActive = true,
                            activeColor = LumaAzure
                        )
                        .clickable {
                            onSave(
                                tile.copy(
                                    title = title.ifBlank { tile.title },
                                    subtitle = subtitle.trim(),
                                    spanCols = spanCols,
                                    spanRows = spanRows,
                                    accentColorHex = accentHex,
                                    customOpacity = customOpacity
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Apply Changes", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}
