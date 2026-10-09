package com.coder.skulens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(viewModel: ProductViewModel) {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Ara") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Ekle") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).padding(16.dp)) {
            if (tab == 0) SearchTab(viewModel) else AddTab(viewModel)
        }
    }
}

@Composable
fun SearchTab(viewModel: ProductViewModel) {
    val state by viewModel.searchState.collectAsState()
    var capturing by remember { mutableStateOf(true) }

    // Photo Picker: izin gerektirmez
    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.searchSimilarProducts(uri)
            capturing = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (capturing) {
            CameraCapture(
                captureLabel = "Fotoğraf Çek ve Ara",
                onCaptured = { bitmap ->
                    viewModel.searchSimilarProducts(bitmap)
                    capturing = false
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(12.dp))
            GalleryButton {
                galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        } else {
            Button(
                onClick = { capturing = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Yeni Arama") }

            Spacer(Modifier.height(24.dp))

            when (val s = state) {
                SearchState.Idle -> Unit
                SearchState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                SearchState.NoMatch -> Text("Eşleşme bulunamadı", style = MaterialTheme.typography.titleMedium)
                is SearchState.Error -> Text("Hata: ${s.message}")
                is SearchState.Found -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.results) { ProductResultCard(it) }
                }
            }
        }
    }
}

@Composable
fun AddTab(viewModel: ProductViewModel) {
    val message by viewModel.message.collectAsState()
    var sku by remember { mutableStateOf("") }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.saveNewProduct(sku, uri) }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = sku,
            onValueChange = { sku = it },
            label = { Text("SKU") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        if (sku.isBlank()) {
            Text("Önce SKU gir", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CameraCapture(
                captureLabel = "Fotoğraf Çek ve Kaydet",
                onCaptured = { bitmap -> viewModel.saveNewProduct(sku, bitmap) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(12.dp))
            GalleryButton {
                galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(2500)
                viewModel.clearMessage()
            }
        }
    }
}

@Composable
fun GalleryButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) { Text("Galeriden Seç") }
}

@Composable
fun ProductResultCard(result: SearchResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(path = result.imagePath, modifier = Modifier.size(96.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = result.sku,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Benzerlik: %${(result.similarity * 100).toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Saklanan ≤512px JPEG'i IO thread'inde decode eder; yüklenirken boş kutu gösterir. */
@Composable
fun ProductImage(path: String, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
