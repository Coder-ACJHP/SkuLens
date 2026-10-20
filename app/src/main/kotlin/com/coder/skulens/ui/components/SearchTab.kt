package com.coder.skulens.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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
import com.coder.skulens.ui.SearchState

@Composable
fun SearchTab(viewModel: MainViewModel) {
    val state by viewModel.searchState.collectAsState()

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.searchSimilarProducts(uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                CameraCapture(
                    captureLabel = "Fotoğraf Çek ve Ara",
                    isActive = state is SearchState.Idle,
                    onCaptured = { bitmap ->
                        viewModel.searchSimilarProducts(bitmap)
                    },
                    onGalleryClick = {
                        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))
            OfflineAssuranceCard()
        }

        if (state is SearchState.Loading) {
            FullScreenLoading(
                type = LoadingType.Search,
                isOverlay = true
            )
        }

        when (val s = state) {
            is SearchState.Found -> {
                SearchResultDialog(
                    results = s.results,
                    onDismiss = viewModel::dismissSearchResult,
                    onNewSearch = viewModel::startNewSearch
                )
            }
            SearchState.NoMatch -> {
                SearchNoMatchDialog(
                    onDismiss = viewModel::dismissSearchResult,
                    onNewSearch = viewModel::startNewSearch
                )
            }
            else -> Unit
        }
    }
}
