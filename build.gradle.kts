buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.3")
    }
}

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}