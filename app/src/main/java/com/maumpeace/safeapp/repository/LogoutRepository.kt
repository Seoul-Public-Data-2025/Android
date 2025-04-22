package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchLogoutData
import com.maumpeace.safeapp.model.LogoutData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class LogoutRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * 🔓 서버 로그아웃 요청
     * @return 성공 여부 (true/false)
     */
    suspend fun logout(refreshToken: String): LogoutData {
        return apiService.logout(FetchLogoutData(refreshToken))
    }
}