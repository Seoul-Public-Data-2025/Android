package com.maumpeace.safeapp.util

import com.naver.maps.geometry.LatLng

object UserStateData {
    var latLng: LatLng? = null

    fun setMyLatLng(latLng: LatLng) {
        this.latLng = latLng
    }

    fun getMyLatLng(): LatLng {
        return latLng ?: LatLng(0.0, 0.0)
    }
}