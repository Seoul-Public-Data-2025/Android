package com.maumpeace.safeapp.model

data class DirectionsResponse(
    val code: Int,
    val message: String,
    val route: Route
)

data class Route(
    val traoptimal: List<Path>
)

data class Path(
    val summary: Summary,
    val path: List<List<Double>>
)

data class Summary(
    val distance: Int,
    val duration: Int
)

