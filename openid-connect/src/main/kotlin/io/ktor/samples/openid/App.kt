package io.ktor.samples.openid

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.auth0.jwt.JWT


suspend fun main() {
    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
    }

    val discoveryResponse = runBlocking {
        discoverSettings(httpClient, "myrealm")
    }

    val provider = getProviderSettings(discoveryResponse)

    embeddedServer(Netty, port = 7070) {
        install(Authentication) {
            oauth("keycloak") {
                client = httpClient
                providerLookup = { provider }
                urlProvider = { "http://localhost:7070/callback" }
            }
        }

        routing {
            authenticate("keycloak") {
                get("/login") {}

                get("/callback") {
                    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                    val idToken = principal?.extraParameters?.get("id_token") ?: error("id_token not found in the response")
                    validateIdToken(idToken, discoveryResponse, provider)
                }
            }
        }
    }.start(wait = true)
}

@Serializable
data class DiscoveryResponse(
    @SerialName("authorization_endpoint")
    val authUrl: String,
    @SerialName("token_endpoint")
    val tokenUrl: String,
    @SerialName("userinfo_endpoint")
    val userinfoUrl: String,

    val issuer: String
)

suspend fun discoverSettings(client: HttpClient, realm: String): DiscoveryResponse {
    val url = "http://localhost:8080/auth/realms/$realm/.well-known/openid-configuration"
    return client.get<HttpStatement>(url).receive()
}

fun getProviderSettings(discoveryResponse: DiscoveryResponse): OAuthServerSettings.OAuth2ServerSettings {
    val clientId = System.getenv("CLIENT_ID") ?: error("Expected env variable CLIENT_ID to be set")
    val clientSecret = System.getenv("CLIENT_SECRET") ?: error("Expected env variable CLIENT_SECRET to be set")

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

class IdTokenValidationException(message: String): Exception(message)

fun validateIdToken(token: String, discoveryResponse: DiscoveryResponse, provider: OAuthServerSettings.OAuth2ServerSettings) {
    val jwt = JWT.decode(token)

    if (!jwt.issuer.equals(discoveryResponse.issuer)) {
        throw IdTokenValidationException(
            "The Issuer ID ${discoveryResponse.issuer} obtained from discovery doesn't match iss Claim ${jwt.issuer}"
        )
    }

    if (!jwt.audience.contains(provider.clientId)) {
        throw IdTokenValidationException(
            "The aud Claim ${jwt.audience} doesn't contain client_id ${provider.clientId} value"
        )
    }

    println(jwt.audience)


    /*
     Clients MUST validate the ID Token in the Token Response in the following manner:

    If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
    If an azp (authorized party) Claim is present, the Client SHOULD verify that its client_id is the Claim Value.
    If the ID Token is received via direct communication between the Client and the Token Endpoint (which it is in this flow), the TLS server validation MAY be used to validate the issuer in place of checking the token signature. The Client MUST validate the signature of all other ID Tokens according to JWS [JWS] using the algorithm specified in the JWT alg Header Parameter. The Client MUST use the keys provided by the Issuer.
    The alg value SHOULD be the default of RS256 or the algorithm sent by the Client in the id_token_signed_response_alg parameter during Registration.
    If the JWT alg Header Parameter uses a MAC based algorithm such as HS256, HS384, or HS512, the octets of the UTF-8 representation of the client_secret corresponding to the client_id contained in the aud (audience) Claim are used as the key to validate the signature. For MAC based algorithms, the behavior is unspecified if the aud is multi-valued or if an azp value is present that is different than the aud value.
    The current time MUST be before the time represented by the exp Claim.
    The iat Claim can be used to reject tokens that were issued too far away from the current time, limiting the amount of time that nonces need to be stored to prevent attacks. The acceptable range is Client specific.
    If a nonce value was sent in the Authentication Request, a nonce Claim MUST be present and its value checked to verify that it is the same value as the one that was sent in the Authentication Request. The Client SHOULD check the nonce value for replay attacks. The precise method for detecting replay attacks is Client specific.
    If the acr Claim was requested, the Client SHOULD check that the asserted Claim Value is appropriate. The meaning and processing of acr Claim Values is out of scope for this specification.
    If the auth_time Claim was requested, either through a specific request for this Claim or by using the max_age parameter, the Client SHOULD check the auth_time Claim value and request re-authentication if it determines too much time has elapsed since the last End-User authentication.
     */
}