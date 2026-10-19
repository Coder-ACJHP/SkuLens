package com.coder.skulens.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coder.skulens.ui.theme.CodeSku
import com.coder.skulens.ui.theme.ErrorContainer
import com.coder.skulens.ui.theme.OnErrorContainer
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.OnSuccessContainer
import com.coder.skulens.ui.theme.OnSurface
import com.coder.skulens.ui.theme.OnSurfaceVariant
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.SkuLensTheme
import com.coder.skulens.ui.theme.SuccessContainer
import com.coder.skulens.ui.theme.SurfaceContainerLow
import com.coder.skulens.ui.theme.SurfaceContainerLowest

/**
 * Awesome, high-tech feedback popup dialog for save successes and errors.
 * Replaces inline banners with a focused, precision-designed modal dialog.
 *
 * @param message The feedback message (e.g. "Kaydedildi: 8690123456789" or "Görsel işlenemedi"). If null, dialog is dismissed.
 * @param isSuccess True if the operation succeeded, false for error.
 * @param onDismiss Invoked when the user dismisses the popup (button, close icon, or backdrop tap).
 */
@Composable
fun FeedbackPopup(
    message: String?,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    if (message == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    val isSkuSaved = isSuccess && message.startsWith("Kaydedildi:")
    val extractedSku = if (isSkuSaved) message.removePrefix("Kaydedildi:").trim() else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(250)) + scaleIn(tween(250, easing = FastOutSlowInEasing), initialScale = 0.9f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(SurfaceContainerLowest)
                    .border(
                        width = 1.dp,
                        color = if (isSuccess) SuccessContainer else ErrorContainer,
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(24.dp)
            ) {
                // Top-right Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Hero Animated Status Icon
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing Halo Ring
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(if (isSuccess) SuccessContainer else ErrorContainer)
                        )

                        // Center Icon Capsule
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isSuccess) SuccessContainer else ErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) OnSuccessContainer else OnErrorContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Title
                    Text(
                        text = if (isSuccess) "Ürün Kaydedildi" else "Kayıt Başarısız",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        textAlign = TextAlign.Center
                    )

                    // Message Details
                    if (extractedSku != null) {
                        // High-tech SKU Code Chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = extractedSku,
                                style = CodeSku,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "Görsel özellikleri çıkarıldı ve yerel vektör kataloğuna başarıyla eklendi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else {
                        // Error description
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSuccess) OnSurfaceVariant else OnErrorContainer,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        if (!isSuccess) {
                            Text(
                                text = "Lütfen ürünün net ve iyi aydınlatılmış bir görselini çekerek tekrar deneyin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Primary Action Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSuccess) PrimaryContainer else MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = if (isSuccess) "Tamam" else "Kapat",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimary
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Success Popup", showBackground = true)
@Composable
private fun FeedbackPopupSuccessPreview() {
    SkuLensTheme {
        FeedbackPopup(
            message = "Kaydedildi: 8690123456789",
            isSuccess = true,
            onDismiss = {}
        )
    }
}

@Preview(name = "Error Popup", showBackground = true)
@Composable
private fun FeedbackPopupErrorPreview() {
    SkuLensTheme {
        FeedbackPopup(
            message = "Görsel işlenemedi",
            isSuccess = false,
            onDismiss = {}
        )
    }
}
