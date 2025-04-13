package com.maumpeace.safeapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.databinding.ActivitySplashBinding
import com.maumpeace.safeapp.util.TokenManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * ✅ SplashActivity
 * - 앱 시작 시 진입
 * - accessToken 및 로그인 플래그를 확인하여 화면 이동 결정
 */
@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences에서 로그인 성공 여부 확인
        val isLoginSuccess = getSharedPreferences("auth", MODE_PRIVATE)
            .getBoolean("isLoginSuccess", false)

        val accessToken = TokenManager.getAccessToken(this)

        if (isLoginSuccess && !accessToken.isNullOrBlank() && AuthApiClient.instance.hasToken()) {
            // 카카오 accessToken 유효성 확인
            UserApiClient.instance.accessTokenInfo { _, error ->
                val intent = if (error != null) {
                    Intent(this, LoginActivity::class.java)
                } else {
                    Intent(this, MainActivity::class.java)
                }
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        } else {
            // 로그인 필요
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}