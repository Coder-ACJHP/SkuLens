package com.coder.skulens.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.BuildConfig
import com.coder.skulens.data.AppPreferences
import com.coder.skulens.data.StorageManager
import com.coder.skulens.data.StorageVolumeInfo
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.ui.theme.OnPrimary
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryContainer
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.Surface
import com.coder.skulens.ui.theme.SurfaceContainer
import com.coder.skulens.ui.theme.SurfaceContainerHigh
import com.coder.skulens.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedSize by remember { mutableIntStateOf(AppPreferences.getPhotoResizeSize(context)) }
    var storageVolumes by remember { mutableStateOf(emptyList<StorageVolumeInfo>()) }
    var targetVolumeToMigrate by remember { mutableStateOf<StorageVolumeInfo?>(null) }
    var isMigrating by remember { mutableStateOf(false) }
    var migrationProgress by remember { mutableStateOf("") }
    var showRestartDialog by remember { mutableStateOf(false) }

    fun refreshStorageVolumes() {
        storageVolumes = StorageManager.getAvailableStorageVolumes(context)
    }

    LaunchedEffect(Unit) {
        refreshStorageVolumes()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = Primary
                )
            }
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Photo Resize Size
            SettingsCard(
                title = "Fotoğraf Çözünürlüğü & Boyut",
                icon = Icons.Default.PhotoSizeSelectLarge
            ) {
                Text(
                    text = "Ürün fotoğraflarının saklanacağı ve model tarafından işleneceği maksimum kenar çözünürlüğü. Düşük boyutlar bellek ve disk tasarrufu sağlar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        384 to "384 px\nHızlı",
                        512 to "512 px\nDengeli",
                        768 to "768 px\nYüksek",
                        1024 to "1024 px\nUltra"
                    )

                    presets.forEach { (size, label) ->
                        val isSelected = selectedSize == size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryContainer else SurfaceContainerHigh)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedSize = size
                                    AppPreferences.setPhotoResizeSize(context, size)
                                    ImagePreprocessor.activeMaxSide = size
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Section 2: Storage & Database Path
            SettingsCard(
                title = "Veritabanı & SSD / SD Kart Konumu",
                icon = Icons.Default.Storage
            ) {
                Text(
                    text = "Vektör veritabanı ve fotoğrafların depolandığı disk. Takılı harici bellek kartı veya USB SSD seçebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                if (storageVolumes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    storageVolumes.forEach { volume ->
                        StorageVolumeItem(
                            volume = volume,
                            onSelect = {
                                if (!volume.isCurrent) {
                                    targetVolumeToMigrate = volume
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // Section 3: App Info & Credits
            AboutCard()

            Spacer(Modifier.height(16.dp))
        }
    }

    // Migration Confirmation Dialog
    targetVolumeToMigrate?.let { target ->
        AlertDialog(
            onDismissRequest = { targetVolumeToMigrate = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Depolama Konumunu Değiştir",
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hedef: ${target.title}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = target.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Mevcut ürün kayıtlarını ve fotoğrafları yeni konuma kopyalamak istiyor musunuz?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val destination = target.directory
                        targetVolumeToMigrate = null
                        isMigrating = true
                        migrationProgress = "Taşıma başlatılıyor..."

                        scope.launch(Dispatchers.IO) {
                            val success = StorageManager.migrateData(
                                context = context,
                                targetDir = destination,
                                copyExistingData = true,
                                onProgress = { msg ->
                                    migrationProgress = msg
                                }
                            )
                            withContext(Dispatchers.Main) {
                                isMigrating = false
                                if (success) {
                                    showRestartDialog = true
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Verileri Taşı & Geç")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { targetVolumeToMigrate = null }) {
                        Text("İptal")
                    }
                    OutlinedButton(
                        onClick = {
                            val destination = target.directory
                            targetVolumeToMigrate = null
                            isMigrating = true
                            migrationProgress = "Konum ayarlanıyor..."

                            scope.launch(Dispatchers.IO) {
                                val success = StorageManager.migrateData(
                                    context = context,
                                    targetDir = destination,
                                    copyExistingData = false,
                                    onProgress = { msg ->
                                        migrationProgress = msg
                                    }
                                )
                                withContext(Dispatchers.Main) {
                                    isMigrating = false
                                    if (success) {
                                        showRestartDialog = true
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Sıfırdan Başla")
                    }
                }
            }
        )
    }

    // Migration Progress Dialog
    if (isMigrating) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Veriler Aktarılıyor", fontWeight = FontWeight.Bold, color = Primary)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Primary)
                    Text(
                        text = migrationProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Lütfen uygulamayı kapatmayınız.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Restart Application Dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Yeniden Başlatma Gerekli", fontWeight = FontWeight.Bold, color = Primary)
            },
            text = {
                Text(
                    text = "Depolama konumu başarıyla değiştirildi. Yeni veritabanının ve fotoğrafların aktif olması için uygulamanın yeniden başlatılması gerekmektedir.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        StorageManager.restartApp(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Uygulamayı Şimdi Yeniden Başlat")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun StorageVolumeItem(
    volume: StorageVolumeInfo,
    onSelect: () -> Unit
) {
    val isCurrent = volume.isCurrent
    val borderColor = if (isCurrent) Primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(width = if (isCurrent) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
            .clickable(enabled = !isCurrent, onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) SurfaceContainerHigh else SurfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val volumeIcon = when {
                        volume.isRemovable -> Icons.Default.Usb
                        volume.id != "internal" -> Icons.Default.SdCard
                        else -> Icons.Default.Storage
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) PrimaryContainer else SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = volumeIcon,
                            contentDescription = null,
                            tint = if (isCurrent) OnPrimary else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = volume.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = volume.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Aktif",
                                color = OnPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Seç",
                        color = Secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Kapasite ve Boş Alan Göstergesi
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { volume.usedPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (isCurrent) Primary else Secondary,
                    trackColor = SurfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Boş: ${volume.freeFormatted}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCurrent) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Toplam: ${volume.totalFormatted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SiriusLens",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Primary,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "İnternetsiz Vektörel Ürün Arama & SKU Eşleştirme",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Credits Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Developer: Coder ACJHP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Designer: Efe Şen",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(2.dp))

            // Tagline with heart
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Engineered with",
                    style = MaterialTheme.typography.bodySmall,
                    color = Secondary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "love",
                    style = MaterialTheme.typography.bodySmall,
                    color = Secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(4.dp))

            // Version line: vX.X.X (build)b
            val versionName = BuildConfig.VERSION_NAME
            val versionCode = BuildConfig.VERSION_CODE
            val formattedVersion = "Version: v$versionName($versionCode)b"

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = formattedVersion,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}
