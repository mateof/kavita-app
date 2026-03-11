package com.kavita.core.model.repository

import com.kavita.core.model.Library

interface AdminRepository {
    suspend fun getServerInfo(): ServerAdminInfo
    suspend fun getLibraries(): List<Library>
    suspend fun createLibrary(name: String, type: Int, folders: List<String>): Library
    suspend fun deleteLibrary(libraryId: Int)
    suspend fun scanLibrary(libraryId: Int)
    suspend fun refreshMetadata(libraryId: Int)
    suspend fun getUsers(): List<UserInfo>
    suspend fun inviteUser(email: String, roles: List<String>)
    suspend fun deleteUser(userId: Int)
    fun isAdmin(): Boolean
}

data class ServerAdminInfo(
    val version: String,
    val os: String,
    val isDocker: Boolean,
    val numOfCores: Int,
)

data class UserInfo(
    val id: Int,
    val username: String,
    val email: String?,
    val roles: List<String>,
    val lastActiveUtc: String?,
)
