package com.maumpeace.safeapp.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.databinding.ActivityLoginBinding
import com.maumpeace.safeapp.ui.main.MainActivity
import com.maumpeace.safeapp.viewModel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * ✅ LoginActivity
 * - 카카오 로그인 → 서버에 accessToken 전달 → JWT 수신 후 Main 진입
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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

        setupObservers()
        setupListeners()
    }

    /**
     * 🔄 ViewModel의 상태 관찰
     */
    private fun setupObservers() {
        loginViewModel.loginData.observe(this) { loginData ->
            // 로그인 성공 처리
            loginData?.let {
                getSharedPreferences("auth", MODE_PRIVATE).edit {
                    putBoolean(
                        "isLoginSuccess", true
                    )
                }
                enableLoginButton() // 버튼 다시 활성화
                navigateToMain()
            }
        }

        loginViewModel.errorMessage.observe(this) { error ->
            error?.let {
                Timber.tag("error: ").e(it)
//                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                enableLoginButton() // 에러 나도 버튼 다시 활성화
            }
        }
    }

    /**
     * 🧑 사용자 입력 리스너 설정
     */
    private fun setupListeners() {
        binding.btnKakaoLogin.setOnClickListener {
            performKakaoLogin()
        }
    }

    /**
     * 🔐 카카오 로그인 시도 → accessToken 획득
     */
    private fun performKakaoLogin() {
        disableLoginButton() // 버튼 비활성화

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Toast.makeText(this, "카카오 로그인 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT)
                    .show()
                enableLoginButton()
            } else if (token != null) {
                getKakaoUserInfo(token.accessToken)
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    /**
     * 👤 카카오 유저 정보(email) 획득 후 서버 로그인 시도
     */
    private fun getKakaoUserInfo(kakaoAccessToken: String) {
        UserApiClient.instance.me { user, error ->
            if (error != null || user?.kakaoAccount?.email == null) {
                Toast.makeText(this, "카카오 사용자 정보 요청 실패", Toast.LENGTH_SHORT).show()
                enableLoginButton()
            } else {
                val email = user.kakaoAccount?.email.toString()
                val profile = user.kakaoAccount?.profile?.profileImageUrl.toString()
                val nickname = user.kakaoAccount?.profile?.nickname.toString()


                loginViewModel.loginWithKakao(profile, nickname, email, kakaoAccessToken)
            }
        }
    }

    private fun disableLoginButton() {
        binding.btnKakaoLogin.isEnabled = false
        binding.btnKakaoLogin.alpha = 0.5f
    }

    private fun enableLoginButton() {
        binding.btnKakaoLogin.isEnabled = true
        binding.btnKakaoLogin.alpha = 1f
    }

    /**
     * 🏠 MainActivity로 전환
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}