package com.kavita.core.network.api

import com.kavita.core.network.dto.InviteUserRequest
import com.kavita.core.network.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface KavitaUserApi {

    @GET("api/Users")
    suspend fun getUsers(): List<UserDto>

    @POST("api/Account/invite")
    suspend fun inviteUser(@Body request: InviteUserRequest)

    @DELETE("api/Users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: Int)

    @GET("api/Users/{userId}/roles")
    suspend fun getUserRoles(@Path("userId") userId: Int): List<String>
}
