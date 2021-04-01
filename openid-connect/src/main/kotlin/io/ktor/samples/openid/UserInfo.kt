package io.ktor.samples.openid

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfoResponse(
    val sub: String,

    @SerialName("preferred_username")
    val username: String,

    val name: String = "",
    val email: String = ""
)

suspend fun HttpClient.userinfo(url: String, accessToken: String): UserInfoResponse {
    return get(url) {
        header("Authorization", "Bearer $accessToken")
    }
}