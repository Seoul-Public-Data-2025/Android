package com.maumpeace.safeapp.model

data class LoginData (
    val success: Boolean,
    val result: LoginInfoData,
)

data class LoginInfoData(
    val accessToken: String?,
    val refreshToken: String?,
)

data class FetchLoginData(
    val email: String?,
    val code: String?
)