package com.maumpeace.safeapp.model

import com.google.gson.annotations.SerializedName

/**
 * ✅ 로그인 및 토큰 관련 서버 응답을 담는 데이터 클래스
 *
 * 서버 응답 예시:
 * {
 *   "success": true,
 *   "result": {
 *     "accessToken": "abc.def.ghi",
 *     "refreshToken": "jkl.mno.pqr"
 *   }
 * }
 */
data class LoginData(
    val success: Boolean, val result: LoginInfoData
)

/**
 * ✅ JWT 토큰 데이터 모델
 * - accessToken: 서버에서 인증 시 사용할 JWT
 * - refreshToken: accessToken 만료 시 재발급에 사용하는 토큰
 */
data class LoginInfoData(
    @SerializedName("accessToken") val accessToken: String?,

    @SerializedName("refreshToken") val refreshToken: String?
)

/**
 * ✅ 로그인 요청 시 서버에 보낼 데이터
 * - email: 카카오에서 받아온 사용자 이메일
 * - accessToken: 카카오 로그인 accessToken
 */
data class FetchLoginData(
    val email: String?, val accessToken: String?
)