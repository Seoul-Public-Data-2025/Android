package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchLoginData
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

/**
 * LoginRepository
 *
 * 인증 관련 API 호출을 담당하는 계층으로, ViewModel에 비즈니스 로직이 아닌 순수 데이터만 전달합니다.
 */
class LoginRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * 카카오 소셜 로그인 요청
     *
     * @param kakaoAccessToken 카카오 accessToken
     * @param email 사용자 이메일
     * @param hashedPhoneNumber 해시 처리된 전화번호
     * @param profile 프로필 이미지 URL
     * @param nickname 닉네임
     * @param fcmToken FCM 토큰
     * @return 서버에서 발급한 JWT가 포함된 응답
     */
    suspend fun loginWithKakao(
        kakaoAccessToken: String,
        email: String,
        hashedPhoneNumber: String,
        profile: String,
        nickname: String,
        fcmToken: String
    ): LoginData {
        val request = FetchLoginData(
            kakaoAccessToken = kakaoAccessToken,
            email = email,
            hashedPhoneNumber = hashedPhoneNumber,
            profile = profile,
            nickname = nickname,
            fcmToken = fcmToken
        )
        return apiService.loginWithKakao(request)
    }
}