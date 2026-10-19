package com.coder.skulens.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.ui.theme.OnPrimaryContainer
import com.coder.skulens.ui.theme.OnSurfaceVariant
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.PrimaryFixed
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.SecondaryContainer
import com.coder.skulens.ui.theme.SecondaryFixed
import com.coder.skulens.ui.theme.Surface
import com.coder.skulens.ui.theme.SurfaceContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerLow
import com.coder.skulens.ui.theme.SurfaceContainerLowest

/**
 * Predefined loading categories representing primary app operations:
 * - [Search]: Visual vector search & feature extraction
 * - [Add]: Saving new product & embedding vector to local database
 * - [List]: Querying and assembling the catalog
 * - [General]: Generic background operation
 */
enum class LoadingType(
    val defaultTitle: String,
    val defaultSubtitle: String,
    val defaultBadgeText: String
) {
    Search(
        defaultTitle = "Vektörel Arama Yapılıyor",
        defaultSubtitle = "Görsel özellikler analiz ediliyor ve ürün kataloğuyla eşleştiriliyor...",
        defaultBadgeText = "Vektör Taraması Aktif"
    ),
    Add(
        defaultTitle = "Ürün Kaydediliyor",
        defaultSubtitle = "Ürün görsel özellikleri çıkarılıyor ve yerel veritabanına işleniyor...",
        defaultBadgeText = "Vektör Kodlaması"
    ),
    List(
        defaultTitle = "Ürün Kataloğu Yükleniyor",
        defaultSubtitle = "Kayıtlı ürün verileri taranıyor ve hazırlanıyor...",
        defaultBadgeText = "Katalog Okunuyor"
    ),
    General(
        defaultTitle = "İşlem Yapılıyor",
        defaultSubtitle = "Lütfen bekleyin, işlem tamamlanıyor...",
        defaultBadgeText = "İşleniyor"
    );

    val defaultIcon: ImageVector
        get() = when (this) {
            Search -> Icons.Default.DocumentScanner
            Add -> Icons.Default.AddPhotoAlternate
            List -> Icons.AutoMirrored.Filled.FormatListBulleted
            General -> Icons.Default.Memory
        }
}

/**
 * Brand-new full-screen loading screen tailored to the Stitch "Calibrated Precision Field Instrument" design system.
 *
 * @param type The operation preset ([LoadingType.Search], [LoadingType.Add], [LoadingType.List], [LoadingType.General]).
 * @param title Optional title override. Defaults to [LoadingType.defaultTitle].
 * @param subtitle Optional subtitle override. Defaults to [LoadingType.defaultSubtitle].
 * @param icon Optional icon override. Defaults to [LoadingType.defaultIcon].
 * @param statusBadgeText Optional status badge text override. Defaults to [LoadingType.defaultBadgeText].
 * @param progress Optional determinate progress value between 0f and 1f. When null, displays an indeterminate scanning animation.
 * @param isOverlay If true, renders a slightly translucent scrim overlay to cover existing content. If false, renders an opaque full-screen surface.
 * @param onCancel Optional cancellation callback. When provided, shows an "İptal Et" button.
 */
@Composable
fun FullScreenLoading(
    modifier: Modifier = Modifier,
    type: LoadingType = LoadingType.General,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    statusBadgeText: String? = null,
    progress: Float? = null,
    isOverlay: Boolean = false,
    showOfflineBadge: Boolean = true,
    onCancel: (() -> Unit)? = null
) {
    val resolvedTitle = title ?: type.defaultTitle
    val resolvedSubtitle = subtitle ?: type.defaultSubtitle
    val resolvedIcon = icon ?: type.defaultIcon
    val resolvedBadgeText = statusBadgeText ?: type.defaultBadgeText

    val infiniteTransition = rememberInfiniteTransition(label = "loadingAnimations")

    // Radar Ripple Wave 1
    val radarScale1 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale1"
    )
    val radarAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha1"
    )

    // Radar Ripple Wave 2 (delayed phase)
    val radarScale2 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale2"
    )
    val radarAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha2"
    )

    // Orbital ring rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Subtle breathing pulse for center icon
    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    // Telemetry dot pulse
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val backgroundColor = if (isOverlay) Surface.copy(alpha = 0.95f) else Surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Consume click events so user cannot interact with background UI while loading
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Central Precision Telemetry Hub
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Radar Wave 1
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(radarScale1)
                        .alpha(radarAlpha1)
                        .border(
                            width = 2.dp,
                            color = SecondaryFixed,
                            shape = CircleShape
                        )
                )

                // Outer Radar Wave 2
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(radarScale2)
                        .alpha(radarAlpha2)
                        .border(
                            width = 1.5.dp,
                            color = Secondary,
                            shape = CircleShape
                        )
                )

                // Calibrated Orbital Progress Ring
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(126.dp)
                        .rotate(rotationAngle),
                    color = Secondary,
                    trackColor = PrimaryContainer.copy(alpha = 0.12f),
                    strokeWidth = 3.5.dp,
                    strokeCap = StrokeCap.Round
                )

                // Secondary Accent Dial Ring
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(108.dp)
                        .rotate(-rotationAngle * 0.7f),
                    color = SecondaryFixed,
                    trackColor = Color.Transparent,
                    strokeWidth = 1.5.dp,
                    strokeCap = StrokeCap.Round
                )

                // Central Instrument Core Hub
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolvedIcon,
                        contentDescription = null,
                        tint = SecondaryFixed,
                        modifier = Modifier
                            .size(38.dp)
                            .scale(iconPulse)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Operation Title
            Text(
                text = resolvedTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle Description
            Text(
                text = resolvedSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            // Progress or Telemetry Scanline Bar
            if (progress != null) {
                Column(
                    modifier = Modifier.width(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Secondary,
                        trackColor = SurfaceContainerHigh
                    )
                    Text(
                        text = "%${(progress * 100).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Indeterminate Precision Scan Track
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    val scanFraction by infiniteTransition.animateFloat(
                        initialValue = -0.5f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "scanTrack"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(4.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = (scanFraction * 160).dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        SecondaryFixed,
                                        Secondary,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Offline Telemetry Badge
            if (showOfflineBadge) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceContainerLow)
                        .border(1.dp, SurfaceContainerHigh, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(1f)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(Secondary)
                    )
                    Text(
                        text = "$resolvedBadgeText • %100 Çevrimdışı",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            // Optional Cancellation Action
            if (onCancel != null) {
                Spacer(Modifier.height(18.dp))
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "İptal Et",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Search Loading", showBackground = true)
@Composable
private fun SearchLoadingPreview() {
    com.coder.skulens.ui.theme.SkuLensTheme {
        FullScreenLoading(type = LoadingType.Search)
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Add Loading", showBackground = true)
@Composable
private fun AddLoadingPreview() {
    com.coder.skulens.ui.theme.SkuLensTheme {
        FullScreenLoading(type = LoadingType.Add)
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "List Loading", showBackground = true)
@Composable
private fun ListLoadingPreview() {
    com.coder.skulens.ui.theme.SkuLensTheme {
        FullScreenLoading(type = LoadingType.List)
    }
}

