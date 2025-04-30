package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.CreateRelationData
import com.maumpeace.safeapp.model.FetchCreateRelationData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class CreateRelationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun createRelation(parentPhoneNumber: String, parentName: String): CreateRelationData {
        return apiService.createRelation(
            FetchCreateRelationData(
                parentPhoneNumber = parentPhoneNumber, parentName = parentName
            )
        )
    }
}