package com.maumpeace.safeapp.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Base64
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.databinding.ActivityLoginBinding
import com.maumpeace.safeapp.ui.main.MainActivity
import com.maumpeace.safeapp.util.TokenManager
import com.maumpeace.safeapp.viewModel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.security.MessageDigest

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
        logHashedPhoneNumberWithPermissionCheck() // 최초 앱 진입 시 권한 확인 및 로그

    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun logHashedPhoneNumberWithPermissionCheck() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_PHONE_STATE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val deniedPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isNotEmpty()) {
            // 설명이 필요한 권한이 있으면 사용자에게 Toast로 알림
            if (deniedPermissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
            ) {
                Toast.makeText(this, "로그인을 위해 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }

            ActivityCompat.requestPermissions(this, deniedPermissions.toTypedArray(), 1234)
            return
        }

        // 모든 권한 허용 상태일 경우 로직 실행
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val phoneNumber = telephonyManager.line1Number

        if (!phoneNumber.isNullOrBlank()) {
            val sha256 = MessageDigest.getInstance("SHA-256")
            val hash = sha256.digest(phoneNumber.toByteArray())
            val encoded = Base64.encodeToString(hash, Base64.NO_WRAP)
            Timber.tag("PHONE_HASH").d("전화번호 해시: $encoded")

            TokenManager.saveHashedPhoneNumber(this, encoded)
        } else {
            Timber.tag("PHONE_HASH").w("전화번호를 불러오지 못했습니다.")
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun collectLoginInfoAndLogin(
        accessToken: String,
        email: String,
        profileImage: String,
        nickname: String
    ) {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val phoneNumber = telephonyManager.line1Number

        val hashedPhoneNumber = phoneNumber?.let {
            val sha256 = MessageDigest.getInstance("SHA-256")
            val hash = sha256.digest(it.toByteArray())
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } ?: ""

        // FCM 토큰 발급
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.tag("FCM").w("FCM 토큰 가져오기 실패: ${task.exception}")
                return@addOnCompleteListener
            }

            val fcmToken = task.result ?: ""
            Timber.tag("FCM").d("FCM 토큰: $fcmToken")

            // ✅ 저장: 최신 토큰 캐싱
            TokenManager.saveFcmToken(this, fcmToken)

            // ✅ ViewModel로 전달
            loginViewModel.loginWithKakao(
                kakaoAccessToken = accessToken,
                email = email,
                hashedPhoneNumber = hashedPhoneNumber,
                profile = profileImage,
                nickname = nickname,
                fcmToken = fcmToken
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1234) {
            val deniedPermanently = permissions.indices.any { i ->
                grantResults[i] == PackageManager.PERMISSION_DENIED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])
            }

            if (deniedPermanently) {
                Toast.makeText(this, "권한이 거부되었습니다. 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
                // 설정 화면으로 이동 유도
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                return
            }

            // 알림 권한 저장
            val isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.indexOf(Manifest.permission.POST_NOTIFICATIONS).takeIf { it != -1 }
                    ?.let { grantResults[it] == PackageManager.PERMISSION_GRANTED } ?: false
            } else {
                true
            }
            TokenManager.saveAlarmPermission(this, isNotificationGranted)

            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                logHashedPhoneNumberWithPermissionCheck()
            } else {
                Timber.tag("PHONE_HASH").w("권한이 거부되어 전화번호를 수집할 수 없습니다.")
            }
        }
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
                logHashedPhoneNumberWithPermissionCheck()
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

                // ⬇️ 로그인 처리 (accessToken 포함)
                collectLoginInfoAndLogin(
                    accessToken = kakaoAccessToken,
                    email = email,
                    profileImage = profile,
                    nickname = nickname
                )
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