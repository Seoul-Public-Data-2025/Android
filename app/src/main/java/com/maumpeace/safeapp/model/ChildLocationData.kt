package com.maumpeace.safeapp.model

data class ChildLocationData(
    val success: Boolean,
)

data class FetchChildLocation(
    val time: String,
    val lat: String,
    val lot: String,
)
