package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchLogoutData
import com.maumpeace.safeapp.model.LogoutData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

/**
 * LogoutRepository
 *
 * 서버에 로그아웃 요청을 보내는 기능을 담당하는 리포지토리 계층.
 * ViewModel로부터 전달받은 refreshToken을 기반으로 로그아웃 처리 요청을 수행합니다.
 */
class LogoutRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * 서버 로그아웃 요청
     *
     * @param refreshToken 현재 로그인 사용자의 refreshToken
     * @return 로그아웃 처리 결과 데이터
     */
    suspend fun logout(refreshToken: String): LogoutData {
        val request = FetchLogoutData(refreshToken)
        return apiService.logout(request)
    }
}