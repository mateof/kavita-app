package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfoDto(
    val os: String = "",
    val dotnetVersion: String = "",
    val runTimeVersion: String = "",
    val kavitaVersion: String = "",
    val numOfCores: Int = 0,
    val isDocker: Boolean = false,
    val installId: String = "",
)
