plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.maumpeace.safeapp"
    compileSdk = 35

    kapt {
        correctErrorTypes = true
    }

    defaultConfig {
        applicationId = "com.maumpeace.safeapp"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // 카카오 로그인 (유저 정보 등)
    implementation(libs.v2.user)

    // 공통 모듈 (KakaoSdk.init 등)
    implementation(libs.v2.common)

    implementation(libs.hilt.android)
    implementation(libs.google.material)
    kapt(libs.hilt.compiler)

    // hiltViewModel 사용하기 위해 필요
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)

    // Timber 로그
    implementation(libs.timber)

    // Chucker 로그 (디버그용 HTTP 로깅)
    debugImplementation(libs.chucker.debug)
    releaseImplementation(libs.chucker.release)

    //네이버 지도
    implementation (libs.map.sdk)
    implementation(libs.play.services.location)

    implementation (libs.circleimageview)

    implementation (libs.glide)
    annotationProcessor (libs.compiler)

    // ✅ Fragment에서 by viewModels() 사용 가능하게 해줌
    implementation (libs.androidx.fragment.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}