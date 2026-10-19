package com.coder.skulens.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.ui.theme.InverseOnSurface
import com.coder.skulens.ui.theme.InverseSurface
import com.coder.skulens.ui.theme.OnPrimaryContainer
import com.coder.skulens.ui.theme.OnSecondaryFixed
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.SecondaryFixed
import com.coder.skulens.ui.theme.SecondaryFixedDim

@Composable
fun ViewfinderHud(
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    topBadgeText: String = "HD VEKTÖR",
    bottomHintText: String = "Barkod veya Ambalajı Hizalayın",
    showAngleTelemetry: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Pulse animation for telemetry indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Animated scanning beam vertical position (0f to 1f)
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanBeam"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Subtle Vignette Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            InverseSurface.copy(alpha = 0.75f),
                            Color.Transparent,
                            InverseSurface.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top Telemetry Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Offline Vector Telemetry Chip
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(InverseSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SecondaryFixed.copy(alpha = pulseAlpha))
                )
                Text(
                    text = topBadgeText,
                    color = InverseOnSurface,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                )
            }

            // Flash / Torch Toggle Button
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTorchOn) SecondaryFixed else InverseSurface.copy(alpha = 0.85f)
                    )
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flaş Değiştir",
                    tint = if (isTorchOn) OnSecondaryFixed else InverseOnSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Precision Framing Reticle
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 220.dp, height = if (showAngleTelemetry) 250.dp else 220.dp)
        ) {
            val reticleHeight = maxHeight

            // 4 Corner Brackets
            PrecisionCorner(Alignment.TopStart)
            PrecisionCorner(Alignment.TopEnd)
            PrecisionCorner(Alignment.BottomStart)
            PrecisionCorner(Alignment.BottomEnd)

            // Center Target Crosshair
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(24.dp)
                        .height(1.5.dp)
                        .background(SecondaryFixedDim.copy(alpha = 0.6f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(1.5.dp)
                        .height(24.dp)
                        .background(SecondaryFixedDim.copy(alpha = 0.6f))
                )
            }

            // Animated Scanning Beam
            val beamOffsetY = (reticleHeight - 4.dp) * scanProgress
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = beamOffsetY)
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 8.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                SecondaryFixed.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Bottom Telemetry & Status Capsule
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(InverseSurface.copy(alpha = 0.90f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = SecondaryFixed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = bottomHintText,
                    color = InverseOnSurface,
                    fontSize = 12.sp
                )
            }

            if (showAngleTelemetry) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryContainer.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = SecondaryFixed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Çevrimdışı Model Aktif",
                            color = OnPrimaryContainer,
                            fontSize = 11.sp
                        )
                    }

                    // Multi-angle dots
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SecondaryFixed)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(InverseOnSurface.copy(alpha = 0.4f))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(InverseOnSurface.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PrecisionCorner(alignment: Alignment) {
    val cornerLength = 24.dp
    val strokeWidth = 3.dp

    Box(
        modifier = Modifier
            .align(alignment)
            .size(cornerLength)
    ) {
        when (alignment) {
            Alignment.TopStart -> {
                Box(Modifier.align(Alignment.TopStart).fillMaxWidth().height(strokeWidth).background(SecondaryFixedDim))
                Box(Modifier.align(Alignment.TopStart).width(strokeWidth).fillMaxSize().background(SecondaryFixedDim))
            }
            Alignment.TopEnd -> {
                Box(Modifier.align(Alignment.TopEnd).fillMaxWidth().height(strokeWidth).background(SecondaryFixedDim))
                Box(Modifier.align(Alignment.TopEnd).width(strokeWidth).fillMaxSize().background(SecondaryFixedDim))
            }
            Alignment.BottomStart -> {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(strokeWidth).background(SecondaryFixedDim))
                Box(Modifier.align(Alignment.BottomStart).width(strokeWidth).fillMaxSize().background(SecondaryFixedDim))
            }
            Alignment.BottomEnd -> {
                Box(Modifier.align(Alignment.BottomEnd).fillMaxWidth().height(strokeWidth).background(SecondaryFixedDim))
                Box(Modifier.align(Alignment.BottomEnd).width(strokeWidth).fillMaxSize().background(SecondaryFixedDim))
            }
        }
    }
}
