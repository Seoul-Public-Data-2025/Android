package com.maumpeace.safeapp.network

import com.maumpeace.safeapp.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * ✅ SafeApp의 모든 REST API 엔드포인트를 정의하는 인터페이스
 * - ViewModel/Repository에서는 suspend 사용
 * - TokenAuthenticator 등에서는 동기 Call 사용
 */
interface ApiService {

    /**
     * 🔐 카카오 소셜 로그인 요청
     * @param fetchLoginData 카카오 액세스 토큰 및 이메일
     * @return 서버 JWT 토큰 포함 응답
     */
    @POST("auth/kakao-login/")
    suspend fun loginWithKakao(
        @Body fetchLoginData: FetchLoginData
    ): LoginData

    /**
     * 🔁 accessToken 갱신 (비동기)
     * @param body refreshToken을 담은 맵
     * @return 새 accessToken 포함 LoginData
     */
    @POST("auth/refresh/")
    suspend fun refreshAccessTokenAsync(
        @Body body: Map<String, String>
    ): LoginData

    /**
     * 🔁 accessToken 갱신 (동기)
     * TokenAuthenticator에서 사용
     */
    @POST("auth/refresh/")
    fun refreshAccessTokenSync(
        @Body body: Map<String, String>
    ): Call<LoginData>

    /**
     * 🔓 로그아웃 요청
     * @param fetchLogoutData 사용자 식별 정보
     * @return 로그아웃 처리 결과
     */
    @POST("auth/logout/")
    suspend fun logout(
        @Body fetchLogoutData: FetchLogoutData
    ): LogoutData

    /**
     * 🗺️ 지도 마커 요청
     * @return 서버에서 받은 마커 데이터
     */
    @GET("display-icon/")
    suspend fun mapMarker(): MapMarkerData
}