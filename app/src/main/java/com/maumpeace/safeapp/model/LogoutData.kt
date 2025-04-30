package com.maumpeace.safeapp.model

/**
 * ✅ 로그아웃 관련 서버 응답을 담는 데이터 클래스
 *
 * 서버 응답 예시:
 * {
 *   "success": true,
 * }
 */
data class LogoutData(
    val success: Boolean,
)

/**
 * ✅ 로그아웃 요청 시 서버에 보낼 데이터
 * - refreshToken: JWT refreshToken
 */
data class FetchLogoutData(
    val refreshToken: String?
)