package com.coder.skulens.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coder.skulens.engine.DetectedGarment
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.OnSurfaceVariant
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerHighest
import com.coder.skulens.ui.theme.SurfaceContainerLowest

private val GarmentUnselectedBorderColor = Color(0xFFFFC107) // Yüksek görünürlüklü Amber
private val GarmentSelectedGlowColor = Color(0xFF00E5FF)     // Glow Mavi (Electric Neon Cyan)
private val GarmentSelectedTextColor = Color(0xFF00201A)     // Glow mavi üzerinde yüksek kontrastlı koyu metin

data class MultiDetectionState(
    val sourceBitmap: Bitmap,
    val garments: List<DetectedGarment>,
    val maxSelectable: Int
)

@Composable
fun MultiGarmentSelectorDialog(
    state: MultiDetectionState,
    onConfirm: (List<DetectedGarment>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIndices = remember { mutableStateListOf<Int>() }

    LaunchedEffect(state) {
        selectedIndices.clear()
        val initialCount = minOf(state.garments.size, state.maxSelectable)
        for (i in 0 until initialCount) {
            selectedIndices.add(i)
        }
    }

    fun toggleSelection(index: Int) {
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index)
        } else {
            if (selectedIndices.size < state.maxSelectable) {
                selectedIndices.add(index)
            }
        }
    }

    val allSelectableCount = minOf(state.garments.size, state.maxSelectable)
    val isAllSelected = selectedIndices.size == allSelectableCount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, SurfaceContainerHigh, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Elbise Seçimi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${state.garments.size} elbise tespit edildi • ${selectedIndices.size}/${state.maxSelectable} seçildi",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Interactive Photo with Bounding Boxes
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val availableWidth = maxWidth
                    val bmpWidth = state.sourceBitmap.width.toFloat()
                    val bmpHeight = maxOf(1f, state.sourceBitmap.height.toFloat())
                    val aspect = bmpWidth / bmpHeight
                    val maxPhotoHeight = 350.dp

                    val targetHeight = availableWidth / aspect
                    val (photoWidth, photoHeight) = if (targetHeight <= maxPhotoHeight) {
                        availableWidth to targetHeight
                    } else {
                        (maxPhotoHeight * aspect) to maxPhotoHeight
                    }

                    Box(
                        modifier = Modifier
                            .size(width = photoWidth, height = photoHeight)
                            .clipToBounds()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        Image(
                            bitmap = state.sourceBitmap.asImageBitmap(),
                            contentDescription = "Fotoğraf",
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.FillBounds
                        )

                        // Overlay bounding boxes
                        state.garments.forEachIndexed { index, garment ->
                            val isSelected = selectedIndices.contains(index)
                            val box = garment.box

                            val boxLeft = (photoWidth * box.left).coerceIn(0.dp, photoWidth)
                            val boxTop = (photoHeight * box.top).coerceIn(0.dp, photoHeight)
                            val boxWidth = (photoWidth * box.width).coerceAtMost(photoWidth - boxLeft)
                            val boxHeight = (photoHeight * box.height).coerceAtMost(photoHeight - boxTop)

                            Box(
                                modifier = Modifier
                                    .offset(x = boxLeft, y = boxTop)
                                    .size(width = boxWidth, height = boxHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) GarmentSelectedGlowColor.copy(alpha = 0.20f)
                                        else Color.Black.copy(alpha = 0.25f)
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 4.5.dp,
                                                color = GarmentSelectedGlowColor.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        } else Modifier
                                    )
                                    .border(
                                        width = if (isSelected) 2.5.dp else 2.dp,
                                        color = if (isSelected) GarmentSelectedGlowColor else GarmentUnselectedBorderColor,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { toggleSelection(index) }
                            ) {
                                // Badge at top start
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .align(Alignment.TopStart)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) GarmentSelectedGlowColor
                                            else Color(0xFF1E1E1E).copy(alpha = 0.85f)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GarmentSelectedTextColor,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        Text(
                                            text = "#${index + 1}",
                                            color = if (isSelected) GarmentSelectedTextColor else GarmentUnselectedBorderColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Thumbnail strip and quick toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Açılar / Renkler",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = {
                            if (isAllSelected) {
                                selectedIndices.clear()
                            } else {
                                selectedIndices.clear()
                                for (i in 0 until allSelectableCount) {
                                    selectedIndices.add(i)
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, SurfaceContainerHighest),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp)
                    ) {
                        Text(
                            text = if (isAllSelected) "Seçimi Kaldır" else "Tümünü Seç",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Scrollable Thumbnails Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.garments.forEachIndexed { index, garment ->
                        val isSelected = selectedIndices.contains(index)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.5.dp,
                                            color = GarmentSelectedGlowColor.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    } else Modifier
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GarmentSelectedGlowColor else SurfaceContainerHigh,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { toggleSelection(index) }
                        ) {
                            Image(
                                bitmap = garment.crop.asImageBitmap(),
                                contentDescription = "Elbise #${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GarmentSelectedGlowColor.copy(alpha = 0.2f))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(GarmentSelectedGlowColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = GarmentSelectedTextColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Vazgeç",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            val chosen = selectedIndices.sorted().map { state.garments[it] }
                            onConfirm(chosen)
                        },
                        enabled = selectedIndices.isNotEmpty(),
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = OnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedIndices.isEmpty()) "Seçim Yapın" else "Ekle (${selectedIndices.size} Açı)",
                            color = OnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
