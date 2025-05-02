package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchRelationGuardianDeleteData
import com.maumpeace.safeapp.model.RelationGuardianDeleteData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class RelationGuardianDeleteRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun guardianDelete(guardianId: Int): RelationGuardianDeleteData {
        return apiService.relationGuardianDelete(FetchRelationGuardianDeleteData(guardianId))
    }
}