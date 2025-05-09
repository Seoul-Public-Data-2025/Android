package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.ChildLocationDisconnectData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class ChildLocationDisconnectRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun childLocationDisconnect(): ChildLocationDisconnectData {
        return apiService.childLocationDisconnect()
    }
}