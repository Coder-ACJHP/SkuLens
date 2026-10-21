import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.objectbox")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val versionPropsFile = file("version.properties")

fun loadVersionProps(): Properties {
    val props = Properties()
    if (!versionPropsFile.exists()) {
        props["VERSION_CODE"] = "1"
        props["VERSION_NAME"] = "1.0.0"
        FileOutputStream(versionPropsFile).use { props.store(it, null) }
    } else {
        FileInputStream(versionPropsFile).use { props.load(it) }
    }
    return props
}

val versionProps = loadVersionProps()
val currentVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
val currentVersionName = versionProps.getProperty("VERSION_NAME", "1.0.0")

// Sadece APK paketleme (assemble / bundle / package) görevlerinde build sayısını artır
val isBuildingApk = gradle.startParameter.taskNames.any { name ->
    name.contains("assemble", ignoreCase = true) ||
    name.contains("bundle", ignoreCase = true) ||
    name.contains("package", ignoreCase = true)
}

val finalVersionCode = if (isBuildingApk) {
    val nextCode = currentVersionCode + 1
    versionProps["VERSION_CODE"] = nextCode.toString()
    FileOutputStream(versionPropsFile).use { versionProps.store(it, "Auto-incremented build number") }
    nextCode
} else {
    currentVersionCode
}

android {
    namespace = "com.coder.skulens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.coder.skulens"
        minSdk = 26
        targetSdk = 35
        versionCode = finalVersionCode
        versionName = currentVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // CameraX
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // MediaPipe Vision & TensorFlow Lite
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    // EXIF
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Crashlytics
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-crashlytics")


    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.11.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(kotlin("test"))
}
kotlin {
    jvmToolchain(21)
}