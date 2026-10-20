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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coder.skulens.data.SearchResult
import com.coder.skulens.ui.theme.ErrorContainer
import com.coder.skulens.ui.theme.OnErrorContainer
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.OnSurface
import com.coder.skulens.ui.theme.OnSurfaceVariant
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerLowest

/**
 * Search result popup dialog delegating to the shared generic [ProductDetailsDialog].
 */
@Composable
fun SearchResultDialog(
    results: List<SearchResult>,
    onDismiss: () -> Unit,
    onNewSearch: () -> Unit
) {
    val bestMatch = results.firstOrNull() ?: return

    var activeAngleIndex by remember(bestMatch.sku) { mutableIntStateOf(0) }
    val currentPhotoPath = bestMatch.allImagePaths.getOrElse(activeAngleIndex) { bestMatch.imagePath }
    val totalAngles = bestMatch.allImagePaths.size

    ProductDetailsDialog(
        title = "Eşleşen Ürün",
        sku = bestMatch.sku,
        imagePath = currentPhotoPath,
        creationDate = bestMatch.creationDate,
        similarity = bestMatch.similarity,
        itemIndex = if (totalAngles > 1) activeAngleIndex else null,
        itemTotalCount = if (totalAngles > 1) totalAngles else null,
        itemPagerLabel = "Açı",
        onPreviousItem = { if (activeAngleIndex > 0) activeAngleIndex-- },
        onNextItem = { if (activeAngleIndex < totalAngles - 1) activeAngleIndex++ },
        primaryButtonText = "Yeni Arama",
        onPrimaryClick = onNewSearch,
        secondaryButtonText = "Kapat",
        onSecondaryClick = onDismiss,
        onDismiss = onDismiss
    )
}

/**
 * Dialog shown when vector search yielded no matching product.
 */
@Composable
fun SearchNoMatchDialog(
    onDismiss: () -> Unit,
    onNewSearch: () -> Unit
) {
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
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = OnErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Eşleşme Bulunamadı",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Kayıtlı ürünler arasında yeterli benzerlikte (%60+) bir ürün bulunamadı. Lütfen ürünün daha aydınlık ve net bir fotoğrafını çekmeyi deneyin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = CircleShape
                    ) {
                        Text("Kapat")
                    }
                    Button(
                        onClick = onNewSearch,
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer)
                    ) {
                        Text("Yeni Arama", color = OnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
