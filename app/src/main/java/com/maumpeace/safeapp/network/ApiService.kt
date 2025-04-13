package com.maumpeace.safeapp.network

import com.maumpeace.safeapp.model.FetchLoginData
import com.maumpeace.safeapp.model.LoginData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 모든 API 요청을 정의하는 인터페이스
 * suspend 함수는 ViewModel/Repository에서,
 * 동기(Call) 함수는 TokenAuthenticator 등에서 사용
 */
interface ApiService {

    /**
     * 🔐 카카오 소셜 로그인 요청
     * - 서버에 카카오 accessToken, email을 전달
     * - 응답으로 서버 자체 JWT access/refresh 토큰 반환
     */
    @POST("auth/kakao-login/")
    suspend fun loginWithKakao(
        @Body fetchLoginData: FetchLoginData
    ): LoginData

    /**
     * 🔁 accessToken 갱신 요청 (suspend: Repository/VM 용)
     * - refreshToken을 서버에 보내어 accessToken 재발급
     */
    @POST("auth/refresh/")
    suspend fun refreshAccessTokenAsync(
        @Body body: Map<String, String>
    ): LoginData

    /**
     * 🔁 accessToken 갱신 요청 (동기: Authenticator 전용)
     * - 같은 기능이지만 Authenticator는 suspend 불가
     */
    @POST("auth/refresh/")
    fun refreshAccessTokenSync(
        @Body body: Map<String, String>
    ): Call<LoginData>

    /**
     * 🔓 로그아웃 요청
     * - 서버에서 refreshToken을 폐기
     */
    @POST("auth/logout/")
    suspend fun logout()
}