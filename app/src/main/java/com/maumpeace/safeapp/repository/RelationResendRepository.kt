package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchRelationResendData
import com.maumpeace.safeapp.model.RelationResendData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class RelationResendRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun guardianDelete(id: Int): RelationResendData {
        return apiService.relationResend(FetchRelationResendData(id))
    }
}