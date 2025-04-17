package com.maumpeace.safeapp.network

import com.maumpeace.safeapp.model.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NaverDirectionsService {
    @GET("map-direction/v1/driving")
    suspend fun getRoutePath(
        @Query("start") start: String,
        @Query("goal") goal: String,
        @Header("X-NCP-APIGW-API-KEY-ID") clientId: String,
        @Header("X-NCP-APIGW-API-KEY") clientSecret: String
    ): DirectionsResponse
}