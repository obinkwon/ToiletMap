import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.oblab.toiletmap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oblab.toiletmap"
        minSdk = 28 // 기획서: 카카오톡 기준 Android 9
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // 카카오맵 네이티브 라이브러리는 ARM용만 있다. x86 라이브러리가 섞이면 x86_64 에뮬레이터가 x86을 골라 크래시
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }

        // 카카오 네이티브 앱 키는 git에 안 올리려고 local.properties(KAKAO_NATIVE_APP_KEY)에서 읽는다
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"${localProps.getProperty("KAKAO_NATIVE_APP_KEY", "")}\"")
    }

    // 업로드 키 정보는 git에 안 올리려고 local.properties(RELEASE_STORE_FILE 등)에서 읽는다
    val releaseStoreFile = localProps.getProperty("RELEASE_STORE_FILE")
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // 키 설정이 없으면 서명 없이 빌드된다(debug 빌드는 영향 없음)
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kakao.map)
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    // 안드로이드 org.json은 로컬 단위 테스트에서 빈 껍데기라 실제 구현을 테스트에만 넣는다
    testImplementation(libs.org.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
