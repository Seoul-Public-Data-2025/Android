package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.ChildLocationData
import com.maumpeace.safeapp.model.FetchChildLocation
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class ChildLocationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun childLocation(time: String, lat: String, lot: String): ChildLocationData {
        return apiService.childLocation(
            FetchChildLocation(
                time = time, lat = lat, lot = lot
            )
        )
    }
}