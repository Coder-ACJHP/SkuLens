# SkuLens

> **İnternetsiz (Offline) Vektörel Ürün Arama ve SKU Eşleştirme Android Uygulaması**

SkuLens; mağaza, depo veya saha ortamında internet bağlantısına ihtiyaç duymadan, kameradan çekilen veya galeriden seçilen ürün fotoğraflarının görsel embedding (vektör) çıkarımını tamamen cihaz üzerinde (on-device) gerçekleştirerek veritabanındaki ürünlerle anlık eşleştiren modern bir Android uygulamasıdır.

---

## 🚀 Temel Özellikler

- **Çevrimdışı (100% Offline) Çalışma:** Sunucu veya bulut bağlantısı gerektirmez. Tüm yapay zeka çıkarımları ve vektör aramaları cihaz üzerinde koşar.
- **Ürün Kaydı (Ekle):** Kamera veya galeriden ürün görseli seçimi → SKU kodu tanımlama → Anında vektör çıkarma ve yerel veritabanına kayıt.
- **Görsel Arama (Ara):** Fotoğraf çekerek veya seçerek veritabanındaki en yakın ürünleri benzerlik yüzdesi (`% Similarity`) ve eşleşen ürünün kayıtlı görseliyle listeleme.
- **Tek ve Tutarlı Ön İşleme Hattı (Unified Preprocessing Pipeline):** Kayıt ve arama aşamalarında aynı ön işleme hattı kullanılır:
  - Uzun kenarı 512px'den büyük görseller, en/boy oranı korunarak kademeli (bilinear) olarak küçültülür.
  - Galeriden gelen büyük görseller için `inSampleSize` alt örnekleme (OOM koruması).
  - EXIF yönlendirme (rotasyon) düzeltmesi.
- **Düşük Depolama Ayak İzi:** DB içinde büyük blob'lar yerine $\le 512\text{px}$ kaliteli JPEG'ler (`filesDir/product_images/`) tutulur. Başarısız işlemlerde yetim dosya temizliği otomatik yapılır.
- **SKU Tekilleştirme:** Aynı SKU için birden fazla açıdan fotoğraf eklenebilir; arama sonucunda o SKU'ya ait en yüksek benzerliğe sahip görsel tek kartta gösterilir.

---

## 🛠️ Teknoloji Yığını

| Katman | Teknoloji / Kütüphane | Açıklama |
| :--- | :--- | :--- |
| **Dil** | Kotlin 2.0.21 | Modern ve güvenli sözdizimi |
| **Kullanıcı Arayüzü** | Jetpack Compose + Material 3 | Modern deklaratif arayüz mimarisi |
| **Kamera** | CameraX 1.4.0 (`camera-view`, `camera-lifecycle`) | Bellek içi yakalama ve arka plan iş parçacığında ön işleme |
| **Görsel Vektörleme** | Google MediaPipe Tasks Vision (`0.10.14`) | `mobilenet_v3_small.tflite` (1024 boyutlu Float vektör) |
| **Vektör Veritabanı** | ObjectBox 4.0.3 (HNSW Index) | Yüksek hızlı yerel vektör arama, `COSINE` mesafesi |
| **Asenkron Yapı** | Kotlin Coroutines & StateFlow | UI ve IO iş parçacıklarının katı ayrımı |
| **Mimari** | MVVM + Clean Architecture | Domain, Data, Presentation, Engine ve Util katmanları |

---

## 📂 Proje Yapısı

```text
SkuLens/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── mobilenet_v3_small.tflite       # 1024-dim MediaPipe embedding modeli
│   │   │   ├── kotlin/com/coder/skulens/
│   │   │   │   ├── App.kt                          # Application sınıfı & DI
│   │   │   │   ├── engine/                         # Vektör ve ML model motoru
│   │   │   │   │   └── VectorEngine.kt             # MediaPipe ImageEmbedder entegrasyonu
│   │   │   │   ├── util/                           # Yardımcı sınıflar & araçlar
│   │   │   │   │   └── ImagePreprocessor.kt        # Kademeli küçültme, EXIF ve URI decode
│   │   │   │   ├── domain/                         # İş kuralları & soyutlamalar (Clean Architecture)
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── SearchResult.kt         # Arama sonucu domain modeli
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── ProductRepository.kt    # Repository arayüzü (Interface)
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── SaveProductUseCase.kt   # Ürün kaydetme use case'i
│   │   │   │   │       └── SearchProductUseCase.kt # Ürün arama use case'i
│   │   │   │   ├── data/                           # Veri katmanı & kalıcılık
│   │   │   │   │   ├── local/entity/
│   │   │   │   │   │   └── ProductEntity.kt        # ObjectBox HnswIndex varlığı (1024L vektör)
│   │   │   │   │   └── repository/
│   │   │   │   │       └── ProductRepositoryImpl.kt # Repository implementasyonu & dosya yönetimi
│   │   │   │   ├── presentation/                   # MVVM UI katmanı (Compose)
│   │   │   │   │   ├── MainActivity.kt             # Tek aktivite, Edge-to-Edge giriş
│   │   │   │   │   ├── MainScreen.kt               # Ara ve Ekle sekmeleri ekranı
│   │   │   │   │   ├── ProductViewModel.kt         # UI durum yönetimi (StateFlow)
│   │   │   │   │   ├── SearchState.kt              # UI durum tanımı (Sealed Interface)
│   │   │   │   │   └── components/                 # Yeniden kullanılabilir UI bileşenleri
│   │   │   │   │       ├── CameraCapture.kt        # CameraX önizleme ve yakalama
│   │   │   │   │       └── ProductResultCard.kt    # Arama sonucu kartı ve görsel yükleme
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties               # Gradle 8.11.1
├── build.gradle.kts                                # Root build yapılandırması
├── settings.gradle.kts
├── skuLens_plan.md                                 # Detaylı MVP plan dokümanı
└── README.md
```

---

## ⚙️ Kurulum ve Derleme

### Gereksinimler
- **JDK:** 21 (Homebrew OpenJDK 21 veya Android Studio JDK)
- **Android SDK:** API 35 (Minimum SDK: 26)
- **Gradle:** 8.11.1 (Wrapper ile birlikte gelir)

### 1. Depoyu Klonlama ve Hazırlık
```bash
git clone <repo-url>
cd SkuLens
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

## 🧪 Doğruluk Testi Prosedürü (Accuracy Test)

Plandaki hedef başarı kriteri: **20–30 gerçek ürünle testte ilk sonuçta (Top-1) %90+ doğru SKU oranı.**

1. **Ürün Kaydı:** Benzer ambalajlı veya aynı markaya ait farklı aromalı/boyutlu 20–30 ürünü belirleyin. **Ekle** sekmesinden her ürün için 1–2 farklı açıdan fotoğraf çekip kaydedin.
2. **Arama Doğrulaması:** Ürünleri **Ara** sekmesinden tekrar fotoğraflayarak veya galeriden seçerek sorgulayın.
3. **Eşik Değeri Ayarı (`DEFAULT_MIN_SIMILARITY`):**
   - Doğru eşleşmelerin benzerlik skorları ile yanlış/alakasız eşleşmelerin skorları gözlemlenir.
   - [`ProductRepository.kt`](file:///Users/coderacjhp/Downloads/Coder%20Projects/SkuLens/app/src/main/kotlin/com/coder/skulens/domain/repository/ProductRepository.kt) içerisindeki `DEFAULT_MIN_SIMILARITY` sabiti (varsayılan: `0.60f`) test sonuçlarına göre kalibre edilebilir.
4. **Fotoğraf Doğrulaması:** Kartta listelenen fotoğraf, eşleşen SKU'ya ait en yüksek skorlu kayıtlı fotoğraf olmalıdır.

---

## 📄 Lisans
Bu proje [LICENSE](LICENSE) dosyası altında lisanslanmıştır.
