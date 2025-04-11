package com.maumpeace.safeapp.network

import com.maumpeace.safeapp.model.FetchLoginData
import com.maumpeace.safeapp.model.LoginData
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    //로그인
    @POST("auth/kakao/login/")
    suspend fun getKioskLogin(@Body fetchLoginData: FetchLoginData): LoginData
}