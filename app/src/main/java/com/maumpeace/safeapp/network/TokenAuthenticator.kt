package com.maumpeace.safeapp.network

import android.content.Context
import android.content.Intent
import com.maumpeace.safeapp.BuildConfig
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.ui.login.LoginActivity
import com.maumpeace.safeapp.util.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * TokenAuthenticator
 *
 * accessToken이 만료되었을 때(HTTP 401) 자동으로 refreshToken을 이용해 재발급을 시도하고,
 * 성공 시 새로운 accessToken으로 요청을 재시도합니다.
 * refreshToken까지 만료된 경우 로그인 화면으로 이동시킵니다.
 */
class TokenAuthenticator(
    private val context: Context
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = TokenManager.getRefreshToken(context) ?: return null

        // 무한 루프 방지: 이미 재시도한 요청은 중단
        if (responseCount(response) >= 2) return null

        // 임시 Retrofit 인스턴스 생성 (DI 비사용)
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        return try {
            val result = apiService.refreshAccessTokenSync(
                mapOf("refreshToken" to refreshToken)
            ).execute()

            if (result.isSuccessful) {
                val loginData: LoginData? = result.body()
                val accessToken = loginData?.result?.accessToken
                val newRefreshToken = loginData?.result?.refreshToken

                if (accessToken.isNullOrEmpty()) return null

                // 새 토큰 저장
                TokenManager.saveTokens(context, accessToken, newRefreshToken)

                // 원래 요청에 새 토큰 추가 후 재시도
                response.request.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            } else {
                redirectToLogin()
                null
            }
        } catch (e: IOException) {
            redirectToLogin()
            null
        }
    }

    /**
     * 로그인 화면으로 이동
     */
    private fun redirectToLogin() {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    /**
     * 재시도 횟수 계산 (무한 루프 방지)
     */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}