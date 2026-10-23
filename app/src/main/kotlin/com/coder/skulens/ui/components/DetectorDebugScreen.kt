package com.coder.skulens.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.App
import com.coder.skulens.engine.DetectedGarment
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.Surface
import com.coder.skulens.ui.theme.SurfaceContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectorDebugScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedGarments by remember { mutableStateOf<List<DetectedGarment>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var executionTimeMs by remember { mutableStateOf(0L) }
    var telemetryInfo by remember { mutableStateOf("") }

    var minConfidence by remember { mutableFloatStateOf(0.30f) }
    var iouThreshold by remember { mutableFloatStateOf(0.45f) }
    var expectedCount by remember { mutableIntStateOf(4) }

    val app = context.applicationContext as? App
    val cropper = app?.pipeline?.cropper

    fun runDetection(bitmap: Bitmap) {
        if (cropper == null) return
        scope.launch {
            isProcessing = true
            val (results, elapsed) = withContext(Dispatchers.Default) {
                val start = System.currentTimeMillis()
                val list = cropper.detectObjects(bitmap, minConfidence, iouThreshold)
                list to (System.currentTimeMillis() - start)
            }
            detectedGarments = results
            executionTimeMs = elapsed
            telemetryInfo = (cropper as? com.coder.skulens.engine.BoundingBoxCropper)?.lastRunTelemetry ?: ""
            isProcessing = false
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val bmp = ImagePreprocessor.fromUri(context.contentResolver, uri)
                if (bmp != null) {
                    withContext(Dispatchers.Main) {
                        sourceBitmap = bmp
                        runDetection(bmp)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Faz 0: Dedektör Test Laboratuvarı",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Çoklu elbise tespiti ve doğruluk analizi",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Test Fotoğrafı Seç", fontSize = 13.sp)
            }

            sourceBitmap?.let { bmp ->
                OutlinedButton(
                    onClick = { runDetection(bmp) },
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tekrar Koş")
                }
            }
        }

        // Threshold Sliders Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ayarlanabilir Eşikler", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Text(
                    text = "Skor Eşiği (Min Confidence): ${(minConfidence * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = minConfidence,
                    onValueChange = { minConfidence = it },
                    onValueChangeFinished = { sourceBitmap?.let { runDetection(it) } },
                    valueRange = 0.10f..0.85f
                )

                Text(
                    text = "NMS IoU Eşiği: ${(iouThreshold * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = iouThreshold,
                    onValueChange = { iouThreshold = it },
                    onValueChangeFinished = { sourceBitmap?.let { runDetection(it) } },
                    valueRange = 0.20f..0.80f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Beklenen Elbise Sayısı: $expectedCount",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 4, 5, 6).forEach { count ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (expectedCount == count) Primary else SurfaceContainerHigh)
                                    .clickable { expectedCount = count },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (expectedCount == count) OnPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Preview & Canvas with Bounding Boxes
        sourceBitmap?.let { bmp ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Test Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Draw bounding boxes on Compose Canvas
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height

                            detectedGarments.forEachIndexed { index, garment ->
                                val box = garment.box
                                val left = box.left * canvasW
                                val top = box.top * canvasH
                                val width = box.width * canvasW
                                val height = box.height * canvasH

                                val boxColor = when (index % 4) {
                                    0 -> Color(0xFF00E676)
                                    1 -> Color(0xFF2979FF)
                                    2 -> Color(0xFFFF9100)
                                    else -> Color(0xFFFF1744)
                                }

                                drawRect(
                                    color = boxColor,
                                    topLeft = Offset(left, top),
                                    size = Size(width, height),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }

                        if (isProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Metrics Banner
                    val isMatch = detectedGarments.size == expectedCount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMatch) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFB71C1C).copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tespit: ${detectedGarments.size} / Beklenen: $expectedCount " +
                                    if (isMatch) "✓ TAM İSABET" else "⚠️ FARK VAR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isMatch) Color(0xFF81C784) else Color(0xFFE57373)
                            )
                            Text(
                                text = "Model Çalışma Süresi: $executionTimeMs ms",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (telemetryInfo.isNotBlank()) {
                                Text(
                                    text = telemetryInfo,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                saveDebugImage(context, bmp, detectedGarments)
                            }
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Kaydet",
                                tint = Primary
                            )
                        }
                    }
                }
            }

            // Cropped Garments List
            if (detectedGarments.isNotEmpty()) {
                Text(
                    text = "Tespit Edilen ve Kırpılan Elbiseler (${detectedGarments.size}):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    detectedGarments.forEachIndexed { i, garment ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(110.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = garment.crop.asImageBitmap(),
                                    contentDescription = "Elbise ${i + 1}",
                                    modifier = Modifier
                                        .size(98.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "#${i + 1} (%${(garment.score * 100).toInt()})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Text(
                                    text = "${garment.crop.width}x${garment.crop.height}px",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } ?: run {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Henüz Fotoğraf Seçilmedi",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mağazadan çektiğiniz 4 elbiseli veya çoklu ürünlü fotoğrafı seçerek dedektörün isabetini test edin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun saveDebugImage(context: Context, src: Bitmap, garments: List<DetectedGarment>) {
    try {
        val resultBitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = AndroidColor.GREEN
        }
        val textPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 32f
            style = Paint.Style.FILL
            isFakeBoldText = true
        }
        val bgPaint = Paint().apply {
            color = AndroidColor.BLACK
            style = Paint.Style.FILL
        }

        garments.forEachIndexed { index, g ->
            val rect = g.rect
            val l = rect.left * resultBitmap.width
            val t = rect.top * resultBitmap.height
            val r = rect.right * resultBitmap.width
            val b = rect.bottom * resultBitmap.height

            canvas.drawRect(l, t, r, b, paint)
            val label = "#${index + 1} (${(g.score * 100).toInt()}%)"
            canvas.drawRect(l, t - 36f, l + 180f, t, bgPaint)
            canvas.drawText(label, l + 8f, t - 8f, textPaint)
        }

        val filename = "detector_debug_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/detector_debug")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "detector_debug")
            dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        Toast.makeText(context, "Kaydedildi: Pictures/detector_debug/$filename", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}