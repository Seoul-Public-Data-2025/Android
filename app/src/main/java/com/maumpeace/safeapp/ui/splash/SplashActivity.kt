package com.maumpeace.safeapp.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.R
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

        val nextIntent = if (isLoginSuccess && !accessToken.isNullOrBlank() && AuthApiClient.instance.hasToken()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        // 🎯 반절 올라가면서 서서히 사라지기
        binding.root.animate()
            .translationY(-binding.root.height * 0.25f) // 반절만 위로 이동
            .alpha(0f) // 동시에 투명해지기
            .setInterpolator(AccelerateInterpolator()) // 점점 빨라지는 느낌
            .setDuration(700) // 0.7초 정도로 자연스럽게
            .withEndAction {
                startActivity(nextIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .start()
    }
}