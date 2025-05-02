package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.FetchRelationChildApproveData
import com.maumpeace.safeapp.model.RelationChildApproveData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class RelationChildApproveRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun childApprove(childId: Int): RelationChildApproveData {
        return apiService.relationChildApprove(FetchRelationChildApproveData(childId))
    }
}