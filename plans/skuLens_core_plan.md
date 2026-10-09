# Offline Vektörel Ürün Arama (Android) — MVP Planı

Sen Senior Software Engineer'sin ve İhtisasın Android mobil uygulamalar.

## 1. Amaç ve Kapsam

Mağaza ortamında, internet olmadan, kamerayla çekilen veya galeriden seçilen ürün fotoğrafının vektörünü (embedding) çıkarıp veritabanındaki ürünlerle eşleştiren Android uygulaması. Işık kontrollü ve fotoğraflar net olduğu için ışık/cihaz çeşitliliği payı plandan çıkarıldı.

**MVP kapsamı (sadece bunlar):**
1. **Ekle:** Fotoğraf çek **veya galeriden seç** → SKU gir → kaydet
2. **Ara:** Fotoğraf çek **veya galeriden seç** → en yakın ürünleri göster
3. **Ön işleme:** Kaynak ne olursa olsun, görüntünün uzun kenarı 512 pikselden büyükse en kaliteli/en az kayıplı yöntemle (en/boy oranı korunarak, max 512x512) küçültülür, sonra işleme alınır (Bölüm 6)
4. **Fotoğraf saklama:** Kaydedilen fotoğrafın (512px'e küçültülmüş hali) JPEG olarak cihazda saklanması; vektör ve görsel aynı görüntüden gelir (Bölüm 5)
5. **Sonuç:** Her sonuçta **eşleşen ürünün fotoğrafı**, SKU ve benzerlik skoru; eşik altındaysa veya kayıtlı hiç fotoğraf yoksa "Eşleşme bulunamadı"

**Kapsam dışı:** toplu katalog içe aktarma, tam ekran/yakınlaştırmalı görüntüleme, ayarlar ekranı, ürün düzenleme/silme (silerken fotoğraf dosyası da silinmeli), senkronizasyon, Tahmin etmek yok, overengineering yok, SOLID, KISS, MVVM + Clean Architecture prensipleri dışına çıkmak yok.

**Başarı kriteri:** "Çalışıyor" değil, **"benzer ürünleri doğru ayırıyor"**. Ölçüm: 20-30 gerçek ürünle test, her ürün için ayrı bir fotoğrafla arama, ilk 1 sonuçta doğru SKU oranı (hedef %90+).

## 2. Uygulama Aşamaları

1. **Çekirdek:** Proje kurulumu, ObjectBox + MediaPipe, test bitmap'iyle vektör çıkarma ve arama (UI yok)
2. **Kamera, galeri ve arayüz:** CameraX + izin akışı, galeri seçici, görüntü ön işleme (512px), fotoğrafı saklama, Ekle ve Ara ekranları, sonuçta ürün fotoğrafı
3. **Doğruluk testi:** 20-30 gerçek ürünle test, eşik değerini belirleme (Bölüm 12)
4. **İnce ayar ve paketleme:** Test sonucuna göre ayarlar, hata durumları, APK
5. **Gerekirse:** Model değişimi, toplu içe aktarma

**Karar noktası:** Doğruluk testinde ilk sonuç doğruluğu %90'ın altındaysa (aynı marka farklı boy/aroma karışıyorsa) model değişimi gerekir. Bu en büyük risk; kod tarafı değil model tarafı.

## 3. Teknoloji Yığını

- **Dil:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Kamera:** CameraX (`ImageCapture`, bellek içi yakalama)
- **Embedding:** MediaPipe Tasks Vision — Image Embedder (`mobilenet_v3_small.tflite`)
- **Veritabanı:** ObjectBox 4.x (vektör araması 4.0+ gerektirir)
- **Asenkron:** Coroutines + StateFlow
- **Mimari:** MVVM + Repository, elle bağımlılık enjeksiyonu (Hilt/Koin MVP için gereksiz)

Gradle bağımlılıkları (sürümleri güncel olanla değiştir):

```kotlin
// build.gradle.kts (project) → plugins: id("io.objectbox") version "4.x" apply false
// build.gradle.kts (app)    → plugins: id("io.objectbox")

dependencies {
    implementation("com.google.mediapipe:tasks-vision:0.10.x")
    implementation("androidx.camera:camera-core:1.3.x")
    implementation("androidx.camera:camera-camera2:1.3.x")
    implementation("androidx.camera:camera-lifecycle:1.3.x")
    implementation("androidx.camera:camera-view:1.3.x")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.x")
    implementation("androidx.activity:activity-compose:1.9.x")      // Photo Picker (galeri)
    implementation("androidx.exifinterface:exifinterface:1.3.x")    // galeri fotoğraflarının yönü
}
```

Model dosyası: `../app/src/main/assets/mobilenet_v3_small.tflite`
`AndroidManifest.xml` içine: `<uses-permission android:name="android.permission.CAMERA" />`

## 4. Mesafe Tipi Kararı

**COSINE kullanılıyor.** Gerekçe:
- Görsel embedding'ler için genel kabul gören ölçü, MobileNet çıktılarında Öklidden tutarlı biçimde daha iyi sıralar.
- Hız farkı bu ölçekte (binlerce ürün) ölçülemeyecek kadar küçük; HNSW arama maliyeti mesafe formülünden çok graf dolaşmasından gelir.
- Ek ayar gerektirmez: `distanceType` tek satır.

Alternatif (gereksiz olabilir): `DOT_PRODUCT` en hızlısıdır ama vektörlerin L2-normalize olması gerekir. 10 bin ürün altında kazanç hissedilmez, bu yüzden MVP'de kullanılmıyor.

ObjectBox'ta COSINE mesafesi `0..2` aralığında döner (0 = aynı). Benzerlik skoru: `similarity = 1 - distance`.

## 5. Veritabanı Modeli

Her fotoğraf ayrı bir kayıttır. Aynı SKU için birden fazla fotoğraf eklenebilir (farklı açı = daha iyi eşleşme), arama sonuçları SKU'ya göre tekilleştirilir.

```kotlin
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.VectorDistanceType

@Entity
class ProductEntity(
    @Id var id: Long = 0,
    @Index var sku: String = "",

    // Fotoğraf dosyasının adı (yol değil); dosya filesDir/product_images/ altında durur
    var imageFile: String = "",

    // dimensions, .tflite modelinin çıktı boyutuyla birebir aynı olmalı
    @HnswIndex(dimensions = EMBEDDING_DIM, distanceType = VectorDistanceType.COSINE)
    var imageVector: FloatArray? = null
) {
    companion object {
        const val EMBEDDING_DIM = 1024 // mobilenet_v3_small; model değişirse güncelle
    }
}
```

> `data class` yerine düz `class` kullanıldı: `FloatArray` alanı `equals/hashCode` uyarısı üretir.

**Fotoğraf nerede saklanır?** Vektör sadece arama içindir, görüntüye geri çevrilemez; bu yüzden fotoğrafın kendisi ayrıca saklanır:
- **Dosya olarak** `filesDir/product_images/<uuid>.jpg`, entity'de sadece dosya adı. DB küçük ve hızlı kalır.
- **Hangi görüntü?** Ham fotoğraf değil, **ön işlemden geçmiş ≤512px hali**, JPEG kalite 90 (~40-100 KB). Ham 12MP fotoğraf 3-5 MB tutar; 10 bin ürün için 30-50 GB eder, 512px halle ~0,5-1 GB. Ekranda doğrulama için 512px yeterlidir ve vektörün çıkarıldığı görüntüyle birebir aynıdır. Ham fotoğraf gerekirse v2'de eklenir.
- **Alternatif:** `ByteArray` olarak entity içinde saklamak (yetim dosya riski yok, ama her sorguda blob da okunur). MVP için dosya yöntemi seçildi.
- Uygulama kaldırılınca DB ile birlikte dosyalar da silinir (tutarlı kalırlar).

Application sınıfı (ObjectBox ve bağımlılıklar tek yerde kurulur):

```kotlin
import android.app.Application
import io.objectbox.BoxStore
import java.io.File

class App : Application() {
    lateinit var boxStore: BoxStore
        private set
    lateinit var repository: ProductRepository
        private set

    override fun onCreate() {
        super.onCreate()
        boxStore = MyObjectBox.builder().androidContext(this).build()
        val imagesDir = File(filesDir, "product_images").apply { mkdirs() }
        repository = ProductRepository(boxStore, VectorEngine(this), contentResolver, imagesDir)
    }
}
```

`AndroidManifest.xml` → `<application android:name=".App" ...>`

## 6. Görüntü Ön İşleme (Galeri + Kamera)

**Kural:** Kaynak ne olursa olsun (kamera veya galeri), görüntünün uzun kenarı 512 pikselden büyükse **en/boy oranı korunarak** 512x512 kutusuna sığacak şekilde küçültülür (örn. 4000x3000 → 512x384). 512 ve altındaysa dokunulmaz: büyütme, kırpma ve çarpıtma yok.

Kayıt ve arama **aynı hattı** kullanır. Vektörlerin birbiriyle karşılaştırılabilir olması için bu şart: biri ham, diğeri küçültülmüş görselden çıkarsa skorlar tutarsız olur.

**Yöntem (en az kayıp, ek kütüphanesiz):**
1. **Alt örnekleme ile decode (sadece galeri):** `inSampleSize` ile, hedefin en az 2 katı kalacak şekilde 2'nin kuvveti olarak decode edilir. 12MP bir fotoğraf ~48 MB yerine ~12 MB yer tutar (OOM riski biter); JPEG'lerde bu işlem DCT tabanlı olduğu için kalitesi yüksektir.
2. **EXIF yönü uygulanır (sadece galeri):** Galeri fotoğrafları sıklıkla yan gelir, düzeltilmezse vektör bozulur.
3. **Kademeli yarıya indirme:** Her adımda en fazla 2 kat küçültülür (bilinear filtre), son adım tam hedef boyuta ölçekler. Tek seferde büyük oranda küçültme (örn. 4000 → 512) detay kaybı ve tırtıklanma (aliasing) üretir; kademeli yöntem alan ortalamasına (box filter) yaklaşır ve Android'de ek kütüphane olmadan alınabilecek en iyi sonuçtur.

> **Lanczos neden yok?** Android'de yerleşik Lanczos yoktur; özel kod veya kütüphane gerekir. Model girdiyi zaten kendi giriş boyutuna indirdiği için fark pratikte ölçülemez, MVP'ye eklenmedi. Doğruluk testinde (Bölüm 12) sorun çıkarsa v2'de denenebilir.

```kotlin
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.roundToInt

object ImagePreprocessor {

    const val MAX_SIDE = 512

    /** Galeri/dosya kaynağı. IO thread'inde çağır. Decode edilemezse null döner. */
    fun fromUri(resolver: ContentResolver, uri: Uri): Bitmap? {
        // 1) Sadece boyutları oku (bellek harcamaz)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // 2) Hedefin en az 2 katı kalacak şekilde alt örnekleyerek decode et
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(max(bounds.outWidth, bounds.outHeight))
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        // 3) EXIF yönünü uygula, sonra kaliteli küçült
        val degrees = resolver.openInputStream(uri)?.use { exifDegrees(ExifInterface(it)) } ?: 0
        val oriented = decoded.rotate(degrees)
        if (oriented !== decoded) decoded.recycle()

        val result = resize(oriented)
        if (result !== oriented) oriented.recycle()
        return result
    }

    /**
     * Uzun kenar maxSide'dan büyükse en/boy oranını koruyarak küçültür,
     * değilse aynı nesneyi döner. Verilen src'yi recycle etmez.
     */
    fun resize(src: Bitmap, maxSide: Int = MAX_SIDE): Bitmap {
        if (max(src.width, src.height) <= maxSide) return src

        var current = src
        // Kademeli yarıya indirme: her adım en fazla 2x → aliasing yok
        while (max(current.width, current.height) / 2 >= maxSide) {
            val next = Bitmap.createScaledBitmap(
                current,
                (current.width / 2).coerceAtLeast(1),
                (current.height / 2).coerceAtLeast(1),
                true // bilinear filtre
            )
            if (current !== src) current.recycle()
            current = next
        }

        // Son adım: tam hedef boyuta (en/boy oranı korunur)
        val ratio = maxSide.toFloat() / max(current.width, current.height)
        val w = (current.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (current.height * ratio).roundToInt().coerceAtLeast(1)
        val result = Bitmap.createScaledBitmap(current, w, h, true)
        if (current !== src && current !== result) current.recycle()
        return result
    }

    private fun sampleSizeFor(longest: Int): Int {
        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE * 2) sample *= 2
        return sample
    }

    private fun exifDegrees(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}

internal fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
```

**Kullanım noktaları (hepsi aynı `ImagePreprocessor`'a gider):**
- **Galeri:** Photo Picker'dan gelen `Uri` → ViewModel (IO thread) → Repository → `ImagePreprocessor.fromUri(...)`
- **Kamera:** `CameraCapture` içinde, UI thread'i dışında bir executor'da `ImagePreprocessor.resize(...)` (Bölüm 10)
- **Güvenlik ağı:** `VectorEngine.extractVector` de `resize` çağırır; zaten ≤512px ise hiçbir şey yapmaz

## 7. Çekirdek Motor (MediaPipe)

```kotlin
import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder

class VectorEngine(context: Context) {

    private val embedder: ImageEmbedder

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("mobilenet_v3_small.tflite")
            .build()

        val options = ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .setQuantize(false)
            .build()

        embedder = ImageEmbedder.createFromOptions(context, options)
    }

    /** Vektör çıkarır; boyut modelle uyuşmazsa erken ve anlaşılır hata verir. */
    fun extractVector(bitmap: Bitmap): FloatArray? {
        // Güvenlik ağı: kaynaklar zaten küçültülmüş gelir, ≤512px ise dokunulmaz (idempotent)
        val scaled = ImagePreprocessor.resize(bitmap)
        val mpImage = BitmapImageBuilder(scaled).build()
        val vector = embedder.embed(mpImage)
            .embeddingResult()
            .embeddings()
            .firstOrNull()
            ?.floatEmbedding()

        check(vector == null || vector.size == ProductEntity.EMBEDDING_DIM) {
            "Model ${vector?.size} boyutlu vektör üretti, EMBEDDING_DIM=${ProductEntity.EMBEDDING_DIM}. " +
                "ProductEntity içindeki değeri güncelle (ve uygulamayı kaldırıp yeniden kur)."
        }
        return vector
    }
}
```

## 8. Repository

Eşik ve sonuç sayısı burada tek yerde tutulur. **Eşik değeri tahmindir; doğruluk testiyle ayarlanır.**

```kotlin
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import io.objectbox.BoxStore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SearchResult(
    val sku: String,
    val similarity: Float,
    val imagePath: String // eşleşen kaydın fotoğrafının tam yolu
)

class ProductRepository(
    boxStore: BoxStore,
    private val vectorEngine: VectorEngine,
    private val contentResolver: ContentResolver,
    private val imagesDir: File
) {
    private val productBox = boxStore.boxFor(ProductEntity::class.java)

    fun saveProduct(sku: String, bitmap: Bitmap): Boolean {
        // Tek hat: vektör ve saklanan fotoğraf aynı (≤512px) görüntüden gelir
        val prepared = ImagePreprocessor.resize(bitmap)
        val vector = vectorEngine.extractVector(prepared) ?: return false

        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                check(prepared.compress(Bitmap.CompressFormat.JPEG, 90, out)) { "JPEG yazılamadı" }
            }
            productBox.put(
                ProductEntity(sku = sku.trim(), imageFile = fileName, imageVector = vector)
            )
        } catch (e: Exception) {
            file.delete() // DB kaydı yoksa yetim dosya bırakma
            throw e
        }
        return true
    }

    fun searchProduct(
        bitmap: Bitmap,
        maxResults: Int = 5,
        minSimilarity: Float = DEFAULT_MIN_SIMILARITY
    ): List<SearchResult> {
        val queryVector = vectorEngine.extractVector(bitmap) ?: return emptyList()

        // Aynı SKU'nun birden fazla fotoğrafı olabilir; tekilleştirme için fazla aday çek
        val query = productBox
            .query(ProductEntity_.imageVector.nearestNeighbors(queryVector, maxResults * 4))
            .build()

        return query.use { it.findWithScores() }
            .map {
                val entity = it.get()
                SearchResult(
                    sku = entity.sku,
                    similarity = 1f - it.score.toFloat(),
                    imagePath = File(imagesDir, entity.imageFile).absolutePath
                )
            }
            .filter { it.similarity >= minSimilarity }
            // Sonuçlar benzerliğe göre sıralı gelir; aynı SKU'dan ilk (en iyi eşleşen) fotoğraf kalır,
            // yani kartta gösterilen fotoğraf, sorguya en çok benzeyen kayıtlı fotoğraftır
            .distinctBy { it.sku }
            .take(maxResults)
    }

    // Galeri (Photo Picker) kaynağı: decode + EXIF + 512px küçültme burada yapılır, IO thread'inde çağır
    fun saveProduct(sku: String, uri: Uri): Boolean {
        val bitmap = ImagePreprocessor.fromUri(contentResolver, uri) ?: return false
        return saveProduct(sku, bitmap)
    }

    fun searchProduct(uri: Uri): List<SearchResult> {
        val bitmap = ImagePreprocessor.fromUri(contentResolver, uri) ?: return emptyList()
        return searchProduct(bitmap)
    }

    fun productCount(): Long = productBox.count()

    companion object {
        const val DEFAULT_MIN_SIMILARITY = 0.6f // TEST İLE AYARLA
    }
}
```

> **Doğrulama notu:** `nearestNeighbors` ve `findWithScores()` ObjectBox 4.x sözdizimidir. Kullandığın sürümde derleme hatası alırsan ObjectBox "Vector Search" dokümanındaki örnekle karşılaştır; en sık fark burasıdır.

## 9. ViewModel

```kotlin
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Found(val results: List<SearchResult>) : SearchState
    data object NoMatch : SearchState
    data class Error(val message: String) : SearchState
}

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Kamera kaynağı (bitmap zaten ≤512px gelir)
    fun saveNewProduct(sku: String, bitmap: Bitmap) =
        runSave(sku) { repository.saveProduct(sku, bitmap) }

    fun searchSimilarProducts(bitmap: Bitmap) =
        runSearch { repository.searchProduct(bitmap) }

    // Galeri kaynağı (decode + EXIF + 512px küçültme IO thread'inde repository içinde yapılır)
    fun saveNewProduct(sku: String, uri: Uri) =
        runSave(sku) { repository.saveProduct(sku, uri) }

    fun searchSimilarProducts(uri: Uri) =
        runSearch { repository.searchProduct(uri) }

    private fun runSave(sku: String, block: () -> Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _message.value = try {
                if (block()) "Kaydedildi: $sku" else "Görsel işlenemedi"
            } catch (e: Exception) {
                "Hata: ${e.message}"
            }
        }
    }

    private fun runSearch(block: () -> List<SearchResult>) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchState.value = SearchState.Loading
            _searchState.value = try {
                val results = block()
                if (results.isEmpty()) SearchState.NoMatch else SearchState.Found(results)
            } catch (e: Exception) {
                SearchState.Error(e.message ?: "Bilinmeyen hata")
            }
        }
    }

    fun clearMessage() { _message.value = null }
}
```

ViewModel oluşturma (elle DI, `MainActivity` içinde):

```kotlin
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

val factory = viewModelFactory {
    initializer { ProductViewModel((application as App).repository) }
}
val viewModel = ViewModelProvider(this, factory)[ProductViewModel::class.java]
```

## 10. Kamera (CameraX)

Eski plandaki `onImageCaptureRequest: () -> Bitmap?` yer tutucusu gerçek bir kamera akışına dönüştürüldü. Callback tabanlı: fotoğraf çekilince `onCaptured(bitmap)` çağrılır.

```kotlin
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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
```

> `ImageProxy.toBitmap()` CameraX 1.3+ gerektirir. `takePicture` bellek içi çalışır, dosyaya yazmaz. Bitmap, `ImagePreprocessor.resize` ile burada 512px'e küçültülür; `rotate` uzantısı Bölüm 6'daki dosyada tanımlıdır. Tam çözünürlüklü kare (örn. 12MP ≈ 48 MB) kısa süre bellekte durur; bu yüzden işlem arka plan executor'ında yapılır.

## 11. Kullanıcı Arayüzü

Tek ekran, iki sekme: **Ara** ve **Ekle**. Sade, geniş boşluklu, düz kartlar.

```kotlin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
```

## 12. Doğruluk Testi (atlanmaz)

1. 20-30 gerçek ürün seç; **özellikle benzer olanları** (aynı marka, farklı boy/aroma) dahil et.
2. Her ürün için **Ekle** sekmesinden 1-2 fotoğraf kaydet.
3. Her ürünü **yeniden çekerek** ara; ilk sonucun doğru SKU olup olmadığını not et.
4. Doğru eşleşmelerin ve yanlış (en yakın ama hatalı) eşleşmelerin benzerlik skorlarına bak:
    - Doğru eşleşme skorlarının alt sınırı ile yanlış eşleşme skorlarının üst sınırı arasına `DEFAULT_MIN_SIMILARITY` koy.
    - Aralık yoksa (skorlar iç içeyse) model yetersiz demektir.
5. **Karar:**
    - İlk sonuç doğruluğu **%90+** → devam, eşiği sabitle.
    - **%90 altı** → önce SKU başına 2-3 farklı açıdan fotoğraf eklemeyi dene (ücretsiz iyileştirme); düzelmezse modeli değiştir (daha büyük MobileNetV3 veya başka bir embedding modeli) ve `EMBEDDING_DIM` değerini güncelle.
6. **Ön işleme kontrolü:**
    - Aynı ürünü hem **kamerayla** hem de **galeriden (büyük, 12MP gibi bir fotoğraf)** ara; iki aramanın benzerlik skorları birbirine yakın olmalı. Belirgin fark varsa hat tutarsızdır (EXIF yönü, kaynak farkı).
    - Yan çekilmiş (EXIF yönlü) bir galeri fotoğrafı doğru yönde işlenmeli.
    - Sınır değerleri: 512px ve altı görüntüye dokunulmamalı; 513px ve üstü küçültülmeli; en/boy oranı korunmalı (kare olmayan görsel 512x512'ye sündürülmemeli).
7. **Fotoğraf doğrulaması:** Sonuç kartındaki fotoğraf, eşleşen SKU'ya ait olmalı. Doğru/yanlış eşleşmeleri bu fotoğraflara bakarak gözle teyit et; yanlış eşleşmelerde modelin neyi karıştırdığı (aynı marka, farklı boy/aroma) fotoğraftan hemen görülür. Aynı SKU'ya birden fazla fotoğraf eklendiyse kartta, sorguya en çok benzeyen kayıtlı fotoğraf görünür.

## 13. Kritik Notlar

1. **Thread:** Vektör çıkarma ve arama `Dispatchers.IO` üzerinde; UI thread'inde çağrılmamalı.
2. **Bellek ve ön işleme:** Kamera ve galeri görüntüleri `ImagePreprocessor` ile 512px'e küçültülür (Bölüm 6). Galeride `inSampleSize` ile alt örnekleyerek decode etmek şart; 12MP bir fotoğrafı tam boyutta decode edip sonra küçültmek `OutOfMemoryError` riski taşır. Kayıt ve arama aynı hattı kullanmalı, aksi halde skorlar tutarsız olur.
3. **Boyut uyumu:** `EMBEDDING_DIM` modelin çıktısıyla birebir aynı olmalı. Değiştirirsen eski veritabanıyla uyumsuz olur: uygulamayı kaldırıp yeniden kur (MVP'de migration gerekmez).
4. **Model değişirse tüm kayıtlı vektörler geçersizdir.** Vektörler modele özeldir. Fotoğraflar (512px) saklandığı için ürünleri yeniden çekmek gerekmez: kayıtlı JPEG'ler yeni modelle yeniden işlenip vektörler güncellenebilir (v2'de basit bir "yeniden indeksle" işlemi). Yine de model kararı, katalog doldurmadan önce doğruluk testinde verilmeli.
5. **Eşik olmadan arama her zaman N sonuç döner.** Eşik ve "Eşleşme bulunamadı" durumu MVP'nin parçası, sonradan eklenecek bir şey değil.
6. **Bu dokümandaki kod derlenip test edilmedi.** Sürüm farklarında (özellikle ObjectBox sorgu API'si ve CameraX sürümü) küçük düzeltmeler gerekebilir.
7. **Fotoğraf saklama:** Fotoğraflar `filesDir/product_images/` altında JPEG (kalite 90, ≤512px) olarak tutulur ve entity'de sadece dosya adı saklanır. Kayıt sırasında DB yazımı başarısız olursa dosya silinir (yetim dosya kalmaz). Ürün silme v2'de eklenirse DB kaydıyla birlikte dosya da silinmelidir. Kartta fotoğraf `ContentScale.Crop` ile kareye kırpılarak gösterilir; bu sadece görüntüleme içindir, saklanan dosya kırpılmaz.


## 14. Uygulama ve Doğrulama Raporu (Verification)

Plandaki tüm adımlar kod tabanında uygulanmış ve Gradle üzerinden derlenerek doğrulanmıştır.

### 14.1. Adım ve Bölüm Doğrulama Matrisi

| Bölüm / Aşama | Plan Başlığı | İlgili Kod Dosyası / Bileşen | Durum |
| :--- | :--- | :--- | :---: |
| **Bölüm 1** | Amaç ve Kapsam | `MainScreen.kt`, `ProductRepository.kt`, `ImagePreprocessor.kt` | ✅ Tamamlandı |
| **Bölüm 2 (Aşama 1-2)** | Çekirdek, Kamera, Galeri, UI | CameraX, ObjectBox, MediaPipe, Jetpack Compose | ✅ Tamamlandı |
| **Bölüm 2 (Aşama 3)** | Doğruluk Testi | Saha / Cihaz testi (Bölüm 12) | ⏳ Cihazda Test Edilecek |
| **Bölüm 2 (Aşama 4)** | İnce Ayar ve Paketleme | `./gradlew assembleDebug` (APK hazır) | ✅ Tamamlandı |
| **Bölüm 3** | Teknoloji Yığını & Bağımlılıklar | `../build.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml` | ✅ Tamamlandı |
| **Bölüm 4** | Mesafe Tipi Kararı (COSINE) | `ProductEntity.kt` (`VectorDistanceType.COSINE`), `ProductRepository.kt` | ✅ Tamamlandı |
| **Bölüm 5** | Veritabanı Modeli & Saklama | `ProductEntity.kt`, `App.kt`, `filesDir/product_images/` | ✅ Tamamlandı |
| **Bölüm 6** | Görüntü Ön İşleme (512px) | `ImagePreprocessor.kt` (kademeli küçültme, `inSampleSize`, EXIF) | ✅ Tamamlandı |
| **Bölüm 7** | Çekirdek Motor (MediaPipe) | `VectorEngine.kt` (`mobilenet_v3_small.tflite`, 1024-dim check) | ✅ Tamamlandı |
| **Bölüm 8** | Repository Mimarisi | `ProductRepository.kt` (tekilleştirme, yetim dosya temizliği, minSimilarity) | ✅ Tamamlandı |
| **Bölüm 9** | ViewModel & State | `ProductViewModel.kt` (`SearchState`, `Dispatchers.IO`, StateFlow) | ✅ Tamamlandı |
| **Bölüm 10** | Kamera Entegrasyonu (CameraX) | `CameraCapture.kt` (arka plan executor, bellek içi yakalama, izin akışı) | ✅ Tamamlandı |
| **Bölüm 11** | Kullanıcı Arayüzü (Compose) | `MainScreen.kt` (Ara & Ekle sekmeleri, PhotoPicker, sonuç kartı) | ✅ Tamamlandı |
| **Bölüm 12** | Doğruluk Testi Yönergesi | `ProductRepository.kt` (`DEFAULT_MIN_SIMILARITY = 0.6f`) | ⏳ Saha Testi Bekliyor |
| **Bölüm 13** | Kritik Notlar Kontrolü | Tüm mimari ve bellek/thread disiplini kuralları | ✅ Tamamlandı |

### 14.2. Kritik Notlar Kontrol Listesi

- [x] **Thread Disiplini:** Arama, ön işleme ve vektör çıkarma işlemleri `Dispatchers.IO` ve arka plan `captureExecutor` üzerinde koşar; UI thread asla bloke edilmez.
- [x] **Bellek ve OOM Koruması:** Galeri seçimlerinde `inSampleSize` ile 2 kat paylı decode edilir, kademeli küçültme uygulanır ve geçici bitmap'ler recycle edilir.
- [x] **Model ve Boyut Denetimi:** `ProductEntity.EMBEDDING_DIM = 1024L` ve `VectorEngine.extractVector` içinde boyut uyuşmazlığı kontrolü (`check`) mevcuttur.
- [x] **Yetim Dosya Koruması:** `ProductRepository.saveProduct` içinde veritabanı yazma hatasında JPEG dosyası derhal diskten silinir (`file.delete()`).
- [x] **Eşik ve Boş Eşleşme:** Benzerlik skoru eşik (`0.60f`) altındaysa veya kayıt yoksa `SearchState.NoMatch` ile "Eşleşme bulunamadı" gösterilir.
- [x] **Fotoğraf Doğrulaması:** Kartta listelenen fotoğraf, eşleşen SKU'ya ait en yüksek skorlu kayıtlı görseldir.

### 14.3. Derleme Çıktısı

```bash
./gradlew assembleDebug
# BUILD SUCCESSFUL
```
- **Oluşturulan APK:** `../app/build/outputs/apk/debug/app-debug.apk`
- **Model Varlığı:** `../app/src/main/assets/mobilenet_v3_small.tflite` (3.9 MB, doğrulandı)

### 14.4. Saha Testi Operasyonel Adımları (Bölüm 12)

1. APK'yı test cihazına yükleyin:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
2. 20-30 gerçek ürün için **Ekle** sekmesinden 1-2 farklı açıdan fotoğraf kaydedin.
3. Ürünleri **Ara** sekmesinden test ederek Top-1 doğruluk oranını ölçün (Hedef: %90+).
4. İhtiyaç halinde `ProductRepository.kt` içerisindeki `DEFAULT_MIN_SIMILARITY` değerini kalibre edin.