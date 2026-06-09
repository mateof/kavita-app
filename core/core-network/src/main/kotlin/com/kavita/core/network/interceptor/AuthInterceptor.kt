package com.kavita.core.network.interceptor

import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.ServerTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Añade el token JWT del servidor activo a cada petición. La renovación del token al recibir
 * un 401 la gestiona [com.kavita.core.network.interceptor.SessionAuthenticator].
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val activeServerProvider: ActiveServerProvider,
    private val tokenStore: ServerTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val serverId = activeServerProvider.activeServerId.value ?: return chain.proceed(original)

        val token = tokenStore.getAccessToken(serverId) ?: return chain.proceed(original)

        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
