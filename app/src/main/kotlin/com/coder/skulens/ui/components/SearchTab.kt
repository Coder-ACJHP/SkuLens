package com.coder.skulens.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coder.skulens.ui.CameraCapture
import com.coder.skulens.ui.MainViewModel
import com.coder.skulens.ui.ProductResultCard
import com.coder.skulens.ui.SearchState
import com.coder.skulens.ui.theme.PrimaryContainer

@Composable
fun SearchTab(viewModel: MainViewModel) {
    val state by viewModel.searchState.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()

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
            ) {
                if (isCapturing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        CameraCapture(
                            captureLabel = "Fotoğraf Çek ve Ara",
                            onCaptured = { bitmap ->
                                viewModel.searchSimilarProducts(bitmap)
                            },
                            onGalleryClick = {
                                galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.startNewSearch() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer)
                    ) {
                        Text(
                            text = "Yeni Arama",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    when (val s = state) {
                        SearchState.Idle, SearchState.Loading -> Unit
                        SearchState.NoMatch -> Text("Eşleşme bulunamadı", style = MaterialTheme.typography.titleMedium)
                        is SearchState.Error -> Text("Hata: ${s.message}")
                        is SearchState.Found -> LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.results.size) { index ->
                                ProductResultCard(s.results[index])
                            }
                        }
                    }
                }
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
    }
}
