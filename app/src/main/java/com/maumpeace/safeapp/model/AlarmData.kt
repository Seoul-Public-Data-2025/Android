package com.maumpeace.safeapp.model

import com.google.gson.annotations.SerializedName

data class AlarmData(
    val success: Boolean, val result: AlarmInfoData
)

data class AlarmInfoData(
    @SerializedName("notification") val notification: Boolean?,
)

data class FetchAlarmData(
    val notification: Boolean?,
    val hashedPhoneNumber: String?,
)