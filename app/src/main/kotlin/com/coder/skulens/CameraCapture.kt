package com.coder.skulens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

@Composable
fun CameraCapture(
    captureLabel: String,
    onCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Kamera izni gerekli")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("İzin Ver")
            }
        }
        return
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { captureExecutor.shutdown() } }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                imageCapture.takePicture(
                    captureExecutor, // ağır işler (toBitmap, döndürme, 512px küçültme) UI thread'inde yapılmaz
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val oriented = image.toBitmap().rotate(image.imageInfo.rotationDegrees)
                            image.close()
                            val resized = ImagePreprocessor.resize(oriented) // ≤512px'e en iyi kalitede
                            ContextCompat.getMainExecutor(context).execute { onCaptured(resized) }
                        }
                        override fun onError(exception: ImageCaptureException) { /* loglanabilir */ }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(captureLabel) }
    }
}
