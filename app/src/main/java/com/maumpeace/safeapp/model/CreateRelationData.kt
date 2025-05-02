package com.maumpeace.safeapp.model

data class CreateRelationData(
    val success: Boolean,
)

data class FetchCreateRelationData(
    val parentPhoneNumber: String, val parentName: String
)