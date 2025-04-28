package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.SecessionData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class SecessionRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun secession(): SecessionData {
        return apiService.secession()
    }
}