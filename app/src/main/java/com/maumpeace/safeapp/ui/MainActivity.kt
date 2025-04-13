package com.maumpeace.safeapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.databinding.ActivityMainBinding
import com.maumpeace.safeapp.viewModel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * ✅ MainActivity
 * - 로그인 이후 진입하는 메인 화면
 * - 로그아웃 기능 포함
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    /**
     * 🔓 로그아웃 처리
     */
    private fun performLogout() {
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Toast.makeText(this, "카카오 연결 해제 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT)
                    .show()
                return@unlink
            }

            // 서버 로그아웃 + 토큰 제거
            loginViewModel.logout { success ->
                if (success) {
                    getSharedPreferences("auth", MODE_PRIVATE).edit {
                        putBoolean("isLoginSuccess", false)
                    }
                    Toast.makeText(this, "로그아웃 완료", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    Toast.makeText(this, "서버 로그아웃 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 🔁 로그인 화면으로 이동
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}