package com.kavita.core.network.interceptor

import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.AuthEvent
import com.kavita.core.network.AuthEventBus
import com.kavita.core.network.ServerTokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val activeServerProvider: ActiveServerProvider,
    private val tokenStore: ServerTokenStore,
    private val authEventBus: AuthEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val serverId = activeServerProvider.activeServerId.value

        val request = if (serverId != null) {
            val token = tokenStore.getAccessToken(serverId)
            if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
        } else {
            original
        }

        val response = chain.proceed(request)

        if (response.code == 401 && serverId != null) {
            runBlocking {
                authEventBus.emit(AuthEvent.Unauthorized(serverId))
            }
        }

        return response
    }
}
