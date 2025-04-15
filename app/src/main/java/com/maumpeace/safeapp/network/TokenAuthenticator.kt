package com.maumpeace.safeapp.network

import android.content.Context
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.util.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * ✅ TokenAuthenticator
 * - 모든 API 요청에서 accessToken이 만료(401)되면 자동으로 refreshToken으로 갱신
 */
class TokenAuthenticator(
    private val context: Context
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = TokenManager.getRefreshToken(context) ?: return null

        // 무한 루프 방지: 이미 한 번 재시도했다면 null 반환
        if (responseCount(response) >= 2) return null

        // Retrofit 인스턴스 구성 (직접 생성, DI 사용 안함)
        val retrofit = Retrofit.Builder().baseUrl("http://43.201.36.238:8000/api/")
            .addConverterFactory(GsonConverterFactory.create()).build()

        val apiService = retrofit.create(ApiService::class.java)

        return try {
            // 동기 방식으로 refresh 요청
            val result = apiService.refreshAccessTokenSync(
                mapOf("refreshToken" to refreshToken)
            ).execute()

            if (result.isSuccessful) {
                val body: LoginData? = result.body()
                val accessToken = body?.result?.accessToken
                val newRefreshToken = body?.result?.refreshToken

                if (accessToken.isNullOrEmpty()) return null

                // 새로운 토큰 저장
                TokenManager.saveTokens(context, accessToken, newRefreshToken)

                // 원래 요청 재시도 (accessToken만 교체)
                response.request.newBuilder().header("Authorization", "Bearer $accessToken").build()
            } else {
                null // refreshToken도 만료되었거나 오류
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * ❗ 요청 재시도 횟수 확인 (무한 루프 방지용)
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