package com.maumpeace.safeapp.model

data class RelationGuardianListData(
    val success: Boolean, val result: RelationGuardianListInfoData
)

data class RelationGuardianListInfoData(
    val relations: List<RelationGuardianInfoData>,
)

data class RelationGuardianInfoData(
    val id: Int,
    val name: String,
    val phone: String,
    val role: String,
    val isApproved: Boolean,
)