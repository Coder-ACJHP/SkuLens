package com.coder.skulens.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.ui.CameraCapture
import com.coder.skulens.ui.MainViewModel
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.SurfaceContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerLowest

@Composable
fun AddTab(viewModel: MainViewModel) {
    val message by viewModel.message.collectAsState()
    val skuInput by viewModel.skuInput.collectAsState()
    val confirmedSku by viewModel.confirmedSku.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val duplicateProduct by viewModel.duplicateProduct.collectAsState()
    val stagedBitmaps by viewModel.stagedBitmaps.collectAsState()
    val isConfirmed = confirmedSku.isNotBlank()

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.addStagedAngle(uri)
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(isConfirmed) {
        if (isConfirmed) {
            // SKU onaylandıktan sonra vizör ve deklanşör butonunun görünmesi için aşağı kaydır
            kotlinx.coroutines.delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        } else {
            scrollState.scrollTo(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SkuInputCard(
                skuInput = skuInput,
                confirmedSku = confirmedSku,
                isConfirmed = isConfirmed,
                onSkuInputChange = viewModel::onSkuInputChange,
                onClearInput = viewModel::clearSkuInput,
                onConfirmSku = viewModel::confirmSku,
                onEditSku = viewModel::editSku
            )

            if (isConfirmed) {
                val stagedCount = stagedBitmaps.size
                val nextAngle = (stagedCount + 1).coerceAtMost(3)
                CameraCapture(
                    captureLabel = if (stagedCount >= 3) "3/3 Doldu" else "Açı Çek ($nextAngle/3)",
                    onCaptured = viewModel::addStagedAngle,
                    isActive = !isSaving && stagedCount < 3 && message == null,
                    topBadgeText = "AÇI $stagedCount/3",
                    bottomHintText = "Ürünü Kadraja Hizalayın",
                    helperPrompt = when {
                        stagedCount == 0 -> "1. Açıyı net ve aydınlık çekin."
                        stagedCount < 3 -> "Farklı bir açı çekin ($nextAngle/3) veya Kaydet'e basın."
                        else -> "3 açı tamamlandı. Kaydet butonuna basın."
                    },
                    secondaryButtonText = if (stagedCount == 0) "Kaydet" else "Kaydet ($stagedCount Açı)",
                    onSecondaryClick = viewModel::saveStagedProduct,
                    secondaryButtonEnabled = stagedCount > 0 && !isSaving,
                    onGalleryClick = if (stagedCount < 3 && !isSaving) {
                        { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    } else null,
                    slotsContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (i in 0 until 3) {
                                val bitmap = stagedBitmaps.getOrNull(i)
                                AngleThumbnailSlot(
                                    slotNumber = i + 1,
                                    bitmap = bitmap,
                                    onRemove = { viewModel.removeStagedAngle(i) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LockedCameraSection()
            }

            Spacer(Modifier.height(16.dp))
        }

        if (isSaving) {
            FullScreenLoading(
                type = LoadingType.Add,
                isOverlay = true
            )
        }

        FeedbackPopup(
            message = message,
            isSuccess = message?.let(viewModel::isSuccessMessage) ?: false,
            onDismiss = viewModel::clearMessage
        )

        duplicateProduct?.let { product ->
            ProductDetailsDialog(
                title = "SKU Zaten Kayıtlı",
                sku = product.sku,
                imagePath = product.imagePath,
                creationDate = product.creationDate,
                noticeMessage = "Bu SKU zaten veritabanında kayıtlı. Her ürün tek oturumda eklenmelidir.",
                primaryButtonText = "Kapat",
                onPrimaryClick = viewModel::dismissDuplicateProduct,
                onDismiss = viewModel::dismissDuplicateProduct
            )
        }
    }
}

@Composable
fun AngleThumbnailSlot(
    slotNumber: Int,
    bitmap: Bitmap?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFilled = bitmap != null
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFilled) SurfaceContainerLowest else SurfaceContainer)
            .border(
                1.dp,
                if (isFilled) Primary.copy(alpha = 0.5f) else SurfaceContainerHigh,
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Açı $slotNumber",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Remove (x) Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Sil",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Bottom Slot Tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "Açı $slotNumber",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Açı $slotNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
