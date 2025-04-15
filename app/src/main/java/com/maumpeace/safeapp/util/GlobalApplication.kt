package com.maumpeace.safeapp.util

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "56d45462db421a0576a8bc4710c16560")
    }

    init {
        INSTANCE = this
    }

    companion object {
        lateinit var INSTANCE: GlobalApplication
    }
}