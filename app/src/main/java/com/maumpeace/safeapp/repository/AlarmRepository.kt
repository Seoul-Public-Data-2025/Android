package com.maumpeace.safeapp.repository

import com.maumpeace.safeapp.model.AlarmData
import com.maumpeace.safeapp.model.FetchAlarmData
import com.maumpeace.safeapp.network.ApiService
import javax.inject.Inject

class AlarmRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun alarm(notification: Boolean, hashedPhoneNumber: String): AlarmData {
        return apiService.alarm(
            FetchAlarmData(
                notification = notification, hashedPhoneNumber = hashedPhoneNumber
            )
        )
    }
}