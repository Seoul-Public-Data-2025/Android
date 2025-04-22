package com.maumpeace.safeapp.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.databinding.ActivitySplashBinding
import com.maumpeace.safeapp.ui.login.LoginActivity
import com.maumpeace.safeapp.ui.main.MainActivity
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

        // API 30 이상에서 전체화면 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION") window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // 2초 후 실행
        Handler(Looper.getMainLooper()).postDelayed({
            proceedToNextScreen()
        }, 2000)
    }

    private fun proceedToNextScreen() {
        val isLoginSuccess =
            getSharedPreferences("auth", MODE_PRIVATE).getBoolean("isLoginSuccess", false)
        val accessToken = TokenManager.getAccessToken(this)

        if (isLoginSuccess && !accessToken.isNullOrBlank() && AuthApiClient.instance.hasToken()) {
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
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}