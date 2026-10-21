package com.coder.skulens.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import kotlin.math.roundToInt

/**
 * Modern iOS 17/18 Floating Island Tab Bar (SRP: Floating TabBar Container & Dock)
 * - 4 items: Ara, Ekle, Kayıtlar, Ayarlar
 * - Solid White background (alpha = 1.0f)
 * - Pure shadow (Border tamamen kaldırıldı, belirgin derin gölge eklendi)
 * - Sliding fluid capsule indicator with Spring physics
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ModernFloatingTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    clip = false,
                    spotColor = Color(0x66000000),
                    ambientColor = Color(0x33000000)
                )
                .background(Color.White, shape = CircleShape)
                .padding(4.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val itemWidth = maxWidth / 4
                val density = LocalDensity.current
                val itemWidthPx = with(density) { itemWidth.toPx() }

                val animatedOffsetPx by animateFloatAsState(
                    targetValue = itemWidthPx * selectedTab,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "slidingIndicatorOffset"
                )

                // Kayan akışkan arka plan kapsülü
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = animatedOffsetPx.roundToInt(), y = 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryContainer.copy(alpha = 0.18f),
                                    Primary.copy(alpha = 0.10f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernTabItem(
                        label = "Ara",
                        icon = Icons.Default.Search,
                        isSelected = selectedTab == 0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(0)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ModernTabItem(
                        label = "Ekle",
                        icon = Icons.Default.AddCircle,
                        isSelected = selectedTab == 1,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(1)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ModernTabItem(
                        label = "Kayıtlar",
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        isSelected = selectedTab == 2,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(2)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ModernTabItem(
                        label = "Ayarlar",
                        icon = Icons.Default.Settings,
                        isSelected = selectedTab == 3,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(3)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModernFloatingTabBarPreview() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        ModernFloatingTabBar(selectedTab = 0, onTabSelected = {})
    }
}
