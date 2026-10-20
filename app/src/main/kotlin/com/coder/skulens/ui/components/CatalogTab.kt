package com.coder.skulens.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coder.skulens.data.ProductItem
import com.coder.skulens.ui.CatalogViewModel
import com.coder.skulens.ui.ProductImage
import com.coder.skulens.ui.theme.CodeSku
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.OnSurface
import com.coder.skulens.ui.theme.OnSurfaceVariant
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.SurfaceContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerLow
import com.coder.skulens.ui.theme.SurfaceContainerLowest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CatalogTab(
    viewModel: CatalogViewModel,
    onNavigateToAddTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search & Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                placeholder = {
                    Text(
                        text = "SKU koduna göre ara...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Temizle",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerLowest,
                    unfocusedContainerColor = SurfaceContainerLowest,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(10.dp))

            // Sub-header count
            if (products.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) {
                            "Toplam ${products.size} Ürün"
                        } else {
                            "${filteredProducts.size} / ${products.size} Eşleşen"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "En Yeniler",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(6.dp))
            }

            // List Content / Empty States
            if (products.isEmpty() && !isLoading) {
                // Entire catalog is empty
                EmptyCatalogState(onNavigateToAddTab = onNavigateToAddTab)
            } else if (filteredProducts.isEmpty() && !isLoading) {
                // Search query matched 0 items
                EmptySearchState(
                    query = searchQuery,
                    onClearQuery = { viewModel.onSearchQueryChange("") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { item ->
                        ProductCatalogCard(
                            item = item,
                            onClick = { viewModel.selectProduct(item) }
                        )
                    }
                }
            }
        }

        // Product Detail Modal Dialog (Shared Generic Dialog)
        selectedProduct?.let { product ->
            var activePhotoIndex by remember(product.sku) { mutableIntStateOf(0) }
            val currentPhotoPath = product.allImagePaths.getOrElse(activePhotoIndex) { product.imagePath }
            ProductDetailsDialog(
                title = "Ürün Detayı",
                sku = product.sku,
                imagePath = currentPhotoPath,
                creationDate = product.creationDate,
                itemIndex = if (product.totalPhotosCount > 1) activePhotoIndex else null,
                itemTotalCount = if (product.totalPhotosCount > 1) product.totalPhotosCount else null,
                itemPagerLabel = "Açı",
                onPreviousItem = { if (activePhotoIndex > 0) activePhotoIndex-- },
                onNextItem = { if (activePhotoIndex < product.totalPhotosCount - 1) activePhotoIndex++ },
                onDelete = { viewModel.deleteProduct(product) },
                primaryButtonText = "Kapat",
                onPrimaryClick = { viewModel.selectProduct(null) },
                onDismiss = { viewModel.selectProduct(null) }
            )
        }
    }
}

/**
 * TableView cell showing:
 * - Square stacked image at left
 * - Vertical 2 labels: top label for SKU, bottom label for creationDate & angle count
 */
@Composable
fun ProductCatalogCard(
    item: ProductItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relativeDate = remember(item.creationDate) {
        DateUtils.getRelativeTimeSpanString(
            item.creationDate,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceContainerHigh, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Stacked Images if >1
        StackedProductImage(item = item)

        Spacer(Modifier.width(14.dp))

        // Right: Vertical 2 Labels
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Label 1: SKU
            Text(
                text = item.sku,
                style = CodeSku,
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Label 2: Creation Date + Angle Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = relativeDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                if (item.totalPhotosCount > 1) {
                    Text(
                        text = "• ${item.totalPhotosCount} Açı",
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StackedProductImage(
    item: ProductItem,
    modifier: Modifier = Modifier
) {
    val count = item.totalPhotosCount
    Box(
        modifier = modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        if (count >= 3) {
            // Layer 3 (deepest)
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .offset(x = 6.dp, y = (-6).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh)
                    .border(1.dp, Secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )
        }
        if (count >= 2) {
            // Layer 2 (middle)
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .offset(x = 3.dp, y = (-3).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh)
                    .border(1.dp, Secondary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            )
        }
        // Layer 1 (Top image)
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            ProductImage(
                path = item.imagePath,
                modifier = Modifier.fillMaxSize()
            )
            if (count > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "$count",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogState(onNavigateToAddTab: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Katalog Henüz Boş",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Kayıtlı ürününüz bulunmuyor. Yeni bir ürün eşlemek için Ekle sekmesini kullanabilirsiniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onNavigateToAddTab,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer)
            ) {
                Text(
                    text = "Yeni Ürün Ekle",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(query: String, onClearQuery: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "\"$query\" için sonuç bulunamadı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Farklı bir SKU kodu deneyin veya filtreyi sıfırlayın.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            TextButton(onClick = onClearQuery) {
                Text(
                    text = "Aramayı Temizle",
                    style = MaterialTheme.typography.labelLarge,
                    color = Secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
