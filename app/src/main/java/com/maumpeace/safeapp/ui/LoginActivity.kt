package com.maumpeace.safeapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.ActivityLoginBinding
import com.maumpeace.safeapp.viewModel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKakaoLogin.setOnClickListener {
            kakaoLogin()
        }
    }

    private fun fetchLogin(email: String, code: String) {
        loginViewModel.fetchLoginData(email, code)
        loginViewModel.loginData.observe(this) { loginData ->
            if (loginData != null && loginData.success) {
                Toast.makeText(this, "${email}님 환영합니다!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }
        loginViewModel.errorLiveData.observe(this) { error ->
            error?.let {
                if (it.contains("401") || it.contains("500") || it.contains("502") || error.contains(
                        "JSON"
                    )
                ) {
                    Toast.makeText(this, "점검중", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun kakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                val accessToken = token.accessToken
                getUserInfo(accessToken)
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, prompts = null, callback = callback)
        }
    }

    private fun getUserInfo(accessToken: String) {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Toast.makeText(this, "사용자 정보 요청 실패", Toast.LENGTH_SHORT).show()
            } else if (user != null) {
                fetchLogin(user.kakaoAccount?.email.toString(), accessToken)
            }
        }
    }
}