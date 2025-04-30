package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.RelationChildListData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class RelationChildListRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun relationChildList(): RelationChildListData {
        return apiService.relationChildList()
    }
}