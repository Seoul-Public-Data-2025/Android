package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.MapMarkerData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class MapMarkerRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun mapMarker(): MapMarkerData {
        return apiService.mapMarker()
    }
}