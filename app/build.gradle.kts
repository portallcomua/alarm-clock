plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystoreBase64 = System.getenv("ANDROID_KEYSTORE_BASE64")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val releaseKeystoreFile = layout.buildDirectory.file("release-signing/alarm-clock-release.jks").get().asFile

if (!releaseKeystoreBase64.isNullOrBlank()) {
    releaseKeystoreFile.parentFile.mkdirs()
    releaseKeystoreFile.writeBytes(java.util.Base64.getDecoder().decode(releaseKeystoreBase64))
}

android {
    namespace = "com.example.tvclock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tvclock"
        minSdk = 24
        targetSdk = 34
        versionCode = providers.gradleProperty("appVersionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("appVersion").orElse("1.0").get()
    }

    signingConfigs {
        if (!releaseKeystoreBase64.isNullOrBlank()) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!releaseKeystoreBase64.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.code.gson:gson:2.10.1") // Для збереження списку будильників
    testImplementation("junit:junit:4.13.2")
}
