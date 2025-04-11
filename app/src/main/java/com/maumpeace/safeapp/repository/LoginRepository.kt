package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchLoginData
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class LoginRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun fetchLogin(email: String, code: String): LoginData {
        return apiService.getKioskLogin(FetchLoginData(email, code))
    }
}