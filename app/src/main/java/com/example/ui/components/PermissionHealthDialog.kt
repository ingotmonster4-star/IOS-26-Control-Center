package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.LumaSettings
import com.example.data.model.PermissionType
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.GlassWarningAmber
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface

@Composable
fun PermissionHealthDialog(
    settings: LumaSettings,
    revokedPermissions: List<PermissionType>,
    onReEnablePermission: (PermissionType) -> Unit,
    onSnooze: () -> Unit
) {
    if (revokedPermissions.isEmpty()) return
    val dialogCorner = settings.cornerRoundnessDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .testTag("permission_reenable_dialog")
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(dialogCorner),
                    cornerRadius = dialogCorner,
                    opacityOverride = (settings.glassOpacity + 0.18f).coerceAtMost(0.82f)
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(GlassWarningAmber.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Permission disabled warning",
                        tint = GlassWarningAmber,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Permission Turned Off",
                        style = MaterialTheme.typography.titleLarge,
                        color = FrostedLightText
                    )
                    Text(
                        text = "Re-enable to keep top-right swipe triggers & hardware controls active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryGlassText
                    )
                }
            }

            revokedPermissions.forEach { perm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(20.dp),
                            cornerRadius = 20.dp
                        )
                        .clickable { onReEnablePermission(perm) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = perm.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostedLightText
                        )
                        Text(
                            text = perm.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryGlassText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .liquidGlassSurface(
                                settings = settings,
                                shape = RoundedCornerShape(14.dp),
                                cornerRadius = 14.dp,
                                isActive = true,
                                activeColor = LumaAzure
                            )
                            .clickable { onReEnablePermission(perm) }
                            .padding(horizontal = 14.dp)
                            .testTag("reenable_${perm.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Restore", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSnooze) {
                    Text("Remind Later", color = SecondaryGlassText)
                }
            }
        }
    }
}
