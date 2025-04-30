package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.DirectionsResponse
import com.maumpeace.safeapp.network.NaverDirectionsService
import javax.inject.Inject

/**
 * NaverDirectionsRepository
 *
 * Naver Directions API를 호출하여 자동차 경로 정보를 가져오는 역할을 수행합니다.
 * ViewModel에 실제 데이터만 전달하며, API 호출 세부사항은 숨깁니다.
 */
class NaverDirectionsRepository @Inject constructor(
    private val directionsService: NaverDirectionsService
) {

    /**
     * 자동차 경로 정보 요청
     *
     * @param start 출발지 좌표 (예: "127.1054328,37.3595963")
     * @param goal 도착지 좌표 (예: "127.1234567,37.5432109")
     * @param clientId Naver API Client ID
     * @param clientSecret Naver API Secret Key
     * @return DirectionsResponse - 경로 응답 데이터
     */
    suspend fun getDrivingRoute(
        start: String,
        goal: String,
        clientId: String,
        clientSecret: String
    ): DirectionsResponse {
        return directionsService.getRoutePath(
            start = start,
            goal = goal,
            clientId = clientId,
            clientSecret = clientSecret
        )
    }
}