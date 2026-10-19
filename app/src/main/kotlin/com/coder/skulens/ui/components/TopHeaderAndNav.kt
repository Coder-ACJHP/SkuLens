package com.coder.skulens.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.Surface
import com.coder.skulens.ui.theme.SurfaceContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh

@Composable
fun TopHeaderAndNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Top Brand & Offline Telemetry Row (Removed tab label as requested)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SkuLens",
                    style = MaterialTheme.typography.titleLarge,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Secondary.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "Çevrimdışı Mod",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Stitch Segmented Dual-Pill Nav Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(SurfaceContainer)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavPill(
                label = "Ara",
                icon = Icons.Default.Search,
                isSelected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) }
            )
            NavPill(
                label = "Ekle",
                icon = Icons.Default.AddCircle,
                isSelected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) }
            )
        }
    }
}

@Composable
private fun NavPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) PrimaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
