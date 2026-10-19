package com.coder.skulens.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coder.skulens.ui.CameraCapture
import com.coder.skulens.ui.MainViewModel

@Composable
fun AddTab(viewModel: MainViewModel) {
    val message by viewModel.message.collectAsState()
    val skuInput by viewModel.skuInput.collectAsState()
    val confirmedSku by viewModel.confirmedSku.collectAsState()
    val isConfirmed = confirmedSku.isNotBlank()

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.saveNewProduct(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        FeedbackBanner(
            message = message,
            isSuccess = message?.let(viewModel::isSuccessMessage) ?: false,
            onClearMessage = viewModel::clearMessage
        )

        if (isConfirmed) {
            CameraCapture(
                captureLabel = "Fotoğraf Çek ve Kaydet",
                onCaptured = viewModel::saveNewProduct,
                topBadgeText = "HD VEKTÖR ÇEKİMİ",
                bottomHintText = "Ürünü Kadraja Hizalayın",
                helperPrompt = "Net ve aydınlık bir ürün görseli çekin.",
                showOfflineModelBadge = true,
                onGalleryClick = {
                    galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LockedCameraSection()
        }

        Spacer(Modifier.height(16.dp))
    }
}
