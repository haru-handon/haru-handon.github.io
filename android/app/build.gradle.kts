import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ohsang.util.haruhandon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ohsang.util.haruhandon"
        minSdk = 26
        targetSdk = 36
        // GitHub Actions에서는 실행 번호가 버전 코드가 된다
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0.0"

        // AdMob: 디버그는 항상 Google 테스트 ID (내 광고를 직접 누르면 계정이 정지될 수 있어요)
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
    }

    // 업로드 키는 GitHub Secrets → 워크플로가 환경 변수로 넘겨준다
    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("upload") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")?.takeIf { it.isNotEmpty() }
                    ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("upload")
            // 스토어 빌드에만 실제 AdMob ID (GitHub Secrets: ADMOB_APP_ID, ADMOB_REWARDED_ID)
            System.getenv("ADMOB_APP_ID")?.takeIf { it.isNotBlank() }?.let { manifestPlaceholders["admobAppId"] = it }
            System.getenv("ADMOB_REWARDED_ID")?.takeIf { it.isNotBlank() }?.let {
                buildConfigField("String", "ADMOB_REWARDED_ID", "\"$it\"")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.glance.appwidget)

    // 보상형 광고 (문구 하나 더 받기)
    implementation("com.google.android.gms:play-services-ads:24.4.0")
}
