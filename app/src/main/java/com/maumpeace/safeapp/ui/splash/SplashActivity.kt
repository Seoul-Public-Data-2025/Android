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
import com.maumpeace.safeapp.databinding.ActivitySplashBinding
import com.maumpeace.safeapp.ui.login.LoginActivity
import com.maumpeace.safeapp.ui.main.MainActivity
import com.maumpeace.safeapp.util.PushConstants
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

    private fun createNextIntent(): Intent {
        val isLoginSuccess = getSharedPreferences("auth", MODE_PRIVATE)
            .getBoolean("isLoginSuccess", false)
        val accessToken = TokenManager.getAccessToken(this)

        val intent = if (isLoginSuccess && !accessToken.isNullOrBlank()
            && AuthApiClient.instance.hasToken()) {
            val type = intent?.getStringExtra(PushConstants.KEY_TYPE)
            val id = intent?.getStringExtra(PushConstants.KEY_ID)

            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(PushConstants.KEY_TYPE, type)
                putExtra(PushConstants.KEY_ID, id)
                putExtra("push_handled", false)
            }
        } else {
            Intent(this, LoginActivity::class.java)
        }

        intent.putExtras(getIntent().extras ?: Bundle())
        return intent
    }

    private fun proceedToNextScreen() {
        val nextIntent = createNextIntent()

        binding.root.animate()
            .translationY(-binding.root.height * 0.25f)
            .alpha(0f)
            .setInterpolator(AccelerateInterpolator())
            .setDuration(700)
            .withEndAction {
                startActivity(nextIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .start()
    }
}