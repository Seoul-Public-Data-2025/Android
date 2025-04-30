package com.maumpeace.safeapp.model

data class RelationChildListData(
    val success: Boolean, val result: RelationChildListInfoData
)

data class RelationChildListInfoData(
    val relations: List<RelationChildInfoData>,
)

data class RelationChildInfoData(
    val id: Int,
    val name: String,
    val phone: String,
    val role: String,
    val isApproved: Boolean,
)