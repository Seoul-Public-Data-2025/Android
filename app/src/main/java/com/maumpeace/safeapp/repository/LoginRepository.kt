package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchLoginData
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

/**
 * ✅ LoginRepository
 * - API 호출을 담당하는 계층
 * - ViewModel에 순수한 데이터만 전달함
 */
class LoginRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * 🔐 카카오 소셜 로그인 요청
     * @param email 카카오 사용자 이메일
     * @param accessToken 카카오 accessToken
     * @return 서버에서 발급한 JWT 포함 응답
     */
    suspend fun loginWithKakao(email: String, accessToken: String): LoginData {
        return apiService.loginWithKakao(FetchLoginData(email, accessToken))
    }

    /**
     * 🔁 accessToken 재발급 요청 (refreshToken 사용)
     * @param refreshToken 저장된 refreshToken
     * @return 서버에서 새로 발급한 access/refreshToken
     */
    suspend fun refreshToken(refreshToken: String): LoginData {
        return apiService.refreshAccessTokenAsync(mapOf("refreshToken" to refreshToken))
    }
}