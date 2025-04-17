package com.maumpeace.safeapp.model

import com.google.gson.annotations.SerializedName

/**
 * ✅ 지도 마커 관련 서버 응답을 담는 데이터 클래스
 *
 * 서버 응답 예시:
 * {
 *   "success": true,
 *   "result": {
 *      "facility_type": "004",
 *      "lat": "37.51082200",
 *      "lot": "127.04471600",
 *      "addr": "서울특별시 강남구 봉은사로 409",
 *      "office_name": "CU 선정릉역점"
 *   }
 * }
 */
data class MapMarkerData(
    val success: Boolean, val result: List<MapMarkerInfoData>
)

/**
 * ✅ 지도 마커 데이터 모델
 * - facility_type: 001(결찰서), 002(CCTV), 003(안전시설물), 004(안전지킴이집)
 * - lat, lot: accessToken 만료 시 재발급에 사용하는 토큰
 * - addr: accessToken 만료 시 재발급에 사용하는 토큰
 * - office_name: accessToken 만료 시 재발급에 사용하는 토큰
 */
data class MapMarkerInfoData(
    @SerializedName("facility_type") val type: String?,

    @SerializedName("lat") val lat: String?,

    @SerializedName("lot") val lot: String?,

    @SerializedName("addr") val address: String?,

    @SerializedName("office_name") val name: String?,
)