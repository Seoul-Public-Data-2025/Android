import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
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
        versionCode = 6
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "KAKAO_NATIVE_KEY", "\"${localProps["KAKAO_NATIVE_KEY"]}\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${localProps["NAVER_CLIENT_ID"]}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${localProps["NAVER_CLIENT_SECRET"]}\"")
        buildConfigField("String", "BASE_URL", "\"${localProps["BASE_URL"]}\"")

        manifestPlaceholders.putAll(
            mapOf(
                "kakao_app_key" to requireNotNull(localProps["KAKAO_NATIVE_KEY"]) { "KAKAO_NATIVE_KEY 누락됨" },
                "naver_client_id" to requireNotNull(localProps["NAVER_CLIENT_ID"]) { "NAVER_CLIENT_ID 누락됨" }
            )
        )
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
        buildConfig = true
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