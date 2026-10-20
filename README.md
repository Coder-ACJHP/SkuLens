# SkuLens

> **İnternetsiz (Offline) İki Aşamalı (Two-Stage) Vektörel Ürün Arama ve SKU Eşleştirme Android Uygulaması**

SkuLens; mağaza, depo veya saha ortamında internet bağlantısına ihtiyaç duymadan, kameradan çekilen veya galeriden seçilen ürün fotoğraflarının görsel embedding (vektör) çıkarımını tamamen cihaz üzerinde (on-device) iki aşamalı modern bir yapay zeka hattı ile gerçekleştirerek veritabanındaki ürünlerle anlık eşleştiren modern bir Android uygulamasıdır.

---

## 🚀 Temel Özellikler

- **Çevrimdışı (%100 Offline) Çalışma:** Sunucu veya bulut bağlantısı gerektirmez. Tüm yapay zeka çıkarımları ve vektör aramaları cihaz üzerinde koşar.
- **İki Aşamalı Görsel Arama Hattı (Two-Stage Pipeline):**
  - **Aşama 1 (Nesne Tespiti & Sıkı Kırpma / Arka Plan Temizleme):** Hafif nesne tespit modeli (`clothing_detector.tflite`) ile kıyafet/ürün sınırları milimetrik tespit edilir, mağaza/raf arka plan gürültüsü ve çevre metinler elenerek yalnızca ana ürün alanı izole edilir.
  - **Aşama 2 (ViT Embedding Çıkarımı):** İzole edilmiş ürün görseli, Vision Transformer tabanlı modele (`fashion_clip.tflite`) aktarılarak 512 boyutlu $L_2$ normalize edilmiş anlamsal özellik vektörü çıkarılır.
- **Max-Score Çoklu Açı Eşleştirme:** Her ürün için 3 farklı açıya kadar (ön, arka, detay) fotoğraf saklanabilir. Arama sorgusu sırasında adayın tüm açıları taranır ve en yüksek kosinüs benzerliği veren açı nihai ürün skoru olarak belirlenir (`maxScoreMatch`). Bu sayede arka açıdan çekilen bir fotoğraf, ön açıdan dolayı cezalandırılmaz.
- **Esnek & Genişletilebilir Mimari (Strategy Pattern):** `IVisualSearchPipeline`, `ITightCropper` ve `IImageEmbedder` soyutlamaları sayesinde gelecekteki model değişikliklerinde temel uygulama mimarisi dokunulmadan korunur.
- **Düşük Depolama Ayak İzi:** DB içinde büyük blob'lar yerine $\le 512\text{px}$ kaliteli JPEG'ler (`filesDir/product_images/`) tutulur. Başarısız işlemlerde yetim dosya temizliği otomatik yapılır.
- **Akıllı HNSW Boyut Geçişi:** ObjectBox HNSW indeksi 512 boyuta güncellenmiştir. Boyut uyuşmazlıklarında veritabanı güvenle kurtarılır ve kayıtlı görsellerden vektörler otomatik güncellenir.

---

## 🛠️ Teknoloji Yığını

| Katman | Teknoloji / Kütüphane | Açıklama |
| :--- | :--- | :--- |
| **Dil** | Kotlin 2.0.21 | Modern ve güvenli sözdizimi |
| **Kullanıcı Arayüzü** | Jetpack Compose + Material 3 | Modern deklaratif arayüz mimarisi |
| **Kamera** | CameraX 1.4.0 (`camera-view`, `camera-lifecycle`) | Bellek içi yakalama ve arka plan iş parçacığında işleme |
| **Model Çalıştırıcı** | TensorFlow Lite (`2.16.1`) & MediaPipe Vision | NCHW / NHWC tensor desteği, XNNPACK çoklu iş parçacığı |
| **Aşama 1 Modeli** | `clothing_detector.tflite` | 640x640 ürün tespit ve arka plan temizleme modeli |
| **Aşama 2 Modeli** | `fashion_clip.tflite` | 224x224 ViT tabanlı 512 boyutlu görsel embedding modeli |
| **Vektör Veritabanı** | ObjectBox 4.0.3 (HNSW Index) | 512 boyutlu, `COSINE` mesafeli yüksek hızlı yerel vektör arama |
| **Asenkron Yapı** | Kotlin Coroutines & StateFlow | UI ve IO iş parçacıklarının katı ayrımı |
| **Mimari** | MVVM + Strategy Pattern | Engine, Data, Presentation ve Model katmanları |
| **Büyük Dosya Takibi** | Git LFS | `.tflite` model dosyalarının Git LFS ile versiyonlanması |

---

## 📂 Proje Yapısı

```text
SkuLens/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   ├── clothing_detector.tflite        # Aşama 1 nesne tespit modeli (Git LFS)
│   │   │   │   └── fashion_clip.tflite             # Aşama 2 ViT embedding modeli (Git LFS)
│   │   │   ├── kotlin/com/coder/skulens/
│   │   │   │   ├── App.kt                          # Application sınıfı & Pipeline DI
│   │   │   │   ├── engine/                         # İki Aşamalı Görsel Arama Hattı
│   │   │   │   │   ├── IVisualSearchPipeline.kt    # Ana pipeline strateji arayüzü
│   │   │   │   │   ├── ITightCropper.kt            # Aşama 1 kırpma strateji sözleşmesi
│   │   │   │   │   ├── IImageEmbedder.kt           # Aşama 2 ViT vektörleme sözleşmesi
│   │   │   │   │   ├── BoundingBoxCropper.kt       # Aşama 1 TFLite dedektör & sınır kırpma
│   │   │   │   │   ├── ViTImageEmbedder.kt         # Aşama 2 TFLite ViT & L2 normalizasyon
│   │   │   │   │   ├── TwoStageVisionPipeline.kt   # İki aşamayı birleştiren ana orkestratör
│   │   │   │   │   └── ImagePreprocessor.kt        # EXIF, URI decode ve kademeli boyutlandırma
│   │   │   │   ├── data/                           # Veri katmanı & ObjectBox
│   │   │   │   │   ├── ProductEntity.kt            # ObjectBox 512-dim HnswIndex varlığı
│   │   │   │   │   └── ProductRepository.kt        # Max-Score eşleştirme & ürün yönetimi
│   │   │   │   └── ui/                             # Jetpack Compose UI bileşenleri
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── kotlin/com/coder/skulens/
│   │           └── ProductRulesTest.kt             # Pipeline, L2 ve Max-Score birim testleri
│   ├── build.gradle.kts
│   ├── version.properties
│   └── proguard-rules.pro
├── .gitattributes                                  # Git LFS kuralları (*.tflite)
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## ⚙️ Kurulum ve Derleme

### Gereksinimler
- **JDK:** 21 (Homebrew OpenJDK 21 veya Android Studio JDK)
- **Android SDK:** API 35 (Minimum SDK: 26)
- **Git LFS:** Model dosyalarının çekilmesi için `git-lfs` gereklidir.

### 1. Depoyu Klonlama ve Git LFS
```bash
git clone <repo-url>
cd SkuLens
git lfs pull
```

`local.properties` dosyanızda Android SDK yolunuzun tanımlı olduğundan emin olun:
```properties
sdk.dir=/Users/<kullanici_adiniz>/Library/Android/sdk
```

### 2. Debug APK Derleme
Terminalden şu komutu çalıştırarak projeyi derleyebilirsiniz:
```bash
./gradlew assembleDebug
```
Derleme tamamlandığında APK dosyası şurada oluşturulur:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Cihaza Yükleme
ADB ile bağlı bir fiziksel cihaza veya emülatöre yüklemek için:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Lisans
Bu proje [LICENSE](LICENSE) dosyası altında lisanslanmıştır.
