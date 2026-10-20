package com.coder.skulens.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coder.skulens.ui.ProductImage
import com.coder.skulens.ui.theme.CodeSku
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.OnSuccessContainer
import com.coder.skulens.ui.theme.OnSurface
import com.coder.skulens.ui.theme.OnSurfaceVariant
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.SuccessContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerLow
import com.coder.skulens.ui.theme.SurfaceContainerLowest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared generic product details modal dialog used across Catalog and Search screens.
 *
 * @param title Dialog title (e.g. "Ürün Detayı", "Eşleşen Ürün")
 * @param sku Product SKU code
 * @param imagePath Absolute file path to product image
 * @param creationDate Timestamp of product creation
 * @param similarity Optional similarity percentage (0.0 to 1.0) shown in search results
 * @param itemIndex Current item index when paging multiple matches
 * @param itemTotalCount Total item count when paging multiple matches
 * @param onPreviousItem Invoked when left arrow is tapped
 * @param onNextItem Invoked when right arrow is tapped
 * @param onDelete Optional delete action callback (renders delete button with confirmation)
 * @param primaryButtonText Label for primary action button (e.g. "Kapat", "Yeni Arama")
 * @param onPrimaryClick Action for primary button
 * @param secondaryButtonText Optional label for secondary action button
 * @param onSecondaryClick Action for secondary button
 * @param onDismiss Action when dialog backdrop or close button is tapped
 */
@Composable
fun ProductDetailsDialog(
    title: String,
    sku: String,
    imagePath: String,
    creationDate: Long = 0L,
    similarity: Float? = null,
    noticeMessage: String? = null,
    itemIndex: Int? = null,
    itemTotalCount: Int? = null,
    itemPagerLabel: String = "Fotoğraf",
    onPreviousItem: (() -> Unit)? = null,
    onNextItem: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    primaryButtonText: String = "Kapat",
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isConfirmingDelete by remember { mutableStateOf(false) }

    val formattedDate = remember(creationDate) {
        if (creationDate > 0) {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(creationDate))
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, SurfaceContainerHigh, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            // Close Button
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )

                if (noticeMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = noticeMessage,
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Large Product Image
                ProductImage(
                    path = imagePath,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(Modifier.height(16.dp))

                // SKU Chip with Copy Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
                        text = sku,
                        style = CodeSku,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(sku)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Kopyala",
                            tint = Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Similarity Badge (Search results)
                if (similarity != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SuccessContainer)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = OnSuccessContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Benzerlik: %${(similarity * 100).toInt()}",
                            fontSize = 12.sp,
                            color = OnSuccessContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Creation Date
                if (formattedDate != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Kayıt: $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Multi-item Pager (When multiple matches exist)
                if (itemIndex != null && itemTotalCount != null && itemTotalCount > 1) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onPreviousItem?.invoke() },
                            enabled = itemIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Önceki",
                                tint = if (itemIndex > 0) Secondary else OnSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${itemIndex + 1} / $itemTotalCount $itemPagerLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        IconButton(
                            onClick = { onNextItem?.invoke() },
                            enabled = itemIndex < itemTotalCount - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Sonraki",
                                tint = if (itemIndex < itemTotalCount - 1) Secondary else OnSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Delete Confirmation or Action Buttons
                if (isConfirmingDelete && onDelete != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Bu ürünü ve görselini silmek istediğinize emin misiniz?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isConfirmingDelete = false },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape
                            ) {
                                Text("Vazgeç")
                            }
                            Button(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Sil", color = OnPrimary)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onDelete != null) {
                            OutlinedButton(
                                onClick = { isConfirmingDelete = true },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Sil")
                            }
                        } else if (secondaryButtonText != null && onSecondaryClick != null) {
                            OutlinedButton(
                                onClick = onSecondaryClick,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape
                            ) {
                                Text(secondaryButtonText)
                            }
                        }

                        Button(
                            onClick = onPrimaryClick,
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer)
                        ) {
                            Text(primaryButtonText, color = OnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
