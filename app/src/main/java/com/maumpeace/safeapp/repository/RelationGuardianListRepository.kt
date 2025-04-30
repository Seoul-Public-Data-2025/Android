package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.RelationGuardianListData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class RelationGuardianListRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun relationGuardianList(): RelationGuardianListData {
        return apiService.relationGuardianList()
    }
}