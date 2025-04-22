package com.maumpeace.safeapp.network

import com.maumpeace.safeapp.model.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Naver Directions API를 사용하여 경로 정보를 가져오는 Retrofit 서비스 인터페이스
 */
interface NaverDirectionsService {

    /**
     * 출발지와 도착지를 기반으로 운전 경로를 요청합니다.
     *
     * @param start 출발지 좌표 (예: "127.1054328,37.3595963")
     * @param goal 도착지 좌표 (예: "127.1234567,37.5432109")
     * @param clientId Naver API 클라이언트 ID
     * @param clientSecret Naver API 클라이언트 시크릿 키
     * @return DirectionsResponse - 경로 정보
     */
    @GET("v1/driving")
    suspend fun getRoutePath(
        @Query("start") start: String,
        @Query("goal") goal: String,
        @Header("X-NCP-APIGW-API-KEY-ID") clientId: String,
        @Header("X-NCP-APIGW-API-KEY") clientSecret: String
    ): DirectionsResponse
}