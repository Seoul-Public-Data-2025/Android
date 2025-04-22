package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.MapMarkerData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

/**
 * MapMarkerRepository
 *
 * 지도 마커 데이터를 서버에서 가져오는 역할을 담당하는 리포지토리입니다.
 * ViewModel에서 호출하여 UI에 필요한 마커 정보를 제공합니다.
 */
class MapMarkerRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * 서버로부터 지도 마커 데이터 요청
     *
     * @return 마커 정보가 포함된 MapMarkerData 객체
     */
    suspend fun mapMarker(): MapMarkerData {
        return apiService.mapMarker()
    }
}