package com.kavita.core.network

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Singleton
class ServerTokenStore @Inject constructor() {

    private val tokens = ConcurrentHashMap<Long, TokenPair>()

    fun setTokens(serverId: Long, accessToken: String, refreshToken: String) {
        tokens[serverId] = TokenPair(accessToken, refreshToken)
    }

    fun getTokens(serverId: Long): TokenPair? = tokens[serverId]

    fun getAccessToken(serverId: Long): String? = tokens[serverId]?.accessToken

    fun getRefreshToken(serverId: Long): String? = tokens[serverId]?.refreshToken

    fun removeTokens(serverId: Long) {
        tokens.remove(serverId)
    }

    fun clear() {
        tokens.clear()
    }
}
