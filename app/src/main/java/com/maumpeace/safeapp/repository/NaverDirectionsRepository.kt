package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.DirectionsResponse
import com.maumpeace.safeapp.network.NaverDirectionsService
import javax.inject.Inject

class NaverDirectionsRepository @Inject constructor(
    private val directionsService: NaverDirectionsService
) {
    suspend fun getDrivingRoute(
        start: String, goal: String, clientId: String, clientSecret: String
    ): DirectionsResponse {
        return directionsService.getRoutePath(
            start, goal, clientId = clientId, clientSecret = clientSecret
        )
    }
}
