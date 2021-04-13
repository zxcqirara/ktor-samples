package io.ktor.samples.openid

import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryResponse(
    @SerialName("authorization_endpoint")
    val authUrl: String,
    @SerialName("token_endpoint")
    val tokenUrl: String,
    @SerialName("userinfo_endpoint")
    val userinfoUrl: String,
    @SerialName("registration_endpoint")
    val registerUrl: String,

    val issuer: String
)

suspend fun HttpClient.discoverSettings(url: String): DiscoveryResponse {
    try {
        return get<HttpStatement>(url).receive()
    } catch (cause: ClientRequestException) {
        throw Exception("Unable to dynamically discover information about OpenID Provider by URL $url", cause)
    }
}

fun getProvider(discoveryResponse: DiscoveryResponse, clientId: String, clientSecret: String): OAuthServerSettings.OAuth2ServerSettings {
    return OAuthServerSettings.OAuth2ServerSettings(
        name = "keycloak",
        authorizeUrl = discoveryResponse.authUrl,
        accessTokenUrl = discoveryResponse.tokenUrl,
        clientId = clientId,
        clientSecret = clientSecret,
        requestMethod = HttpMethod.Post,
        defaultScopes = listOf("openid", "profile")
    )
}