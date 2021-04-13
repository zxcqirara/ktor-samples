package io.ktor.samples.openid

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.thymeleaf.*
import kotlinx.coroutines.runBlocking
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

val clientId = System.getenv("CLIENT_ID") ?: error("Expected env variable CLIENT_ID")
val clientSecret = System.getenv("CLIENT_SECRET") ?: error("Expected env variable CLIENT_SECRET") // TODO: Process 401 response
val discoveryURL = System.getenv("DISCOVERY_URL") ?: error("Expected env variable DISCOVERY_URL")
const val callbackPath = "/callback" // TODO: Print URL before starting the server

suspend fun HttpClient.providerInfo() = getProvider(discoverSettings(discoveryURL), clientId, clientSecret)
fun ApplicationCall.callbackURL(): String = urlWithPath(callbackPath).toString()

fun main() {
    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
        }
    }

    embeddedServer(CIO, host = "127.0.0.1", port = 7070) {
        install(Authentication) {
            oauth("keycloak") {
                client = httpClient
                providerLookup = {
                    runBlocking {
                        httpClient.providerInfo()
                    }
                }
                urlProvider = {
                    callbackURL()
                }
            }
        }

        install(Thymeleaf) {
            setTemplateResolver(ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
                suffix = ".html"
                characterEncoding = "utf-8"
            })
        }

        // TODO: validate ID token
        routing {
            get("/") {
                val provider = httpClient.providerInfo()
                // TODO Refactor templates
                call.respond(ThymeleafContent("index", mapOf("inputs" to listOf(
                    Input(title = "Client ID", value = provider.clientId),
                    Input(title = "Client Secret", value = provider.clientSecret),
                    Input(title = "Authorization URL", value = provider.authorizeUrl),
                    Input(title = "Token Request URL", value = provider.accessTokenUrl),
                    Input(title = "Callback URL", value = call.callbackURL()),
                    Input(title = "Scopes", value = provider.defaultScopes.joinToString(separator = ", ")),
                ))))
            }

            post("/userinfo") {
                val accessToken = call.receive<Parameters>()["access_token"]
                val userinfoUrl = httpClient.discoverSettings(discoveryURL).userinfoUrl

                if (accessToken != null) {
                    val userinfo = httpClient.userinfo(userinfoUrl, accessToken)

                    call.respond(ThymeleafContent("userinfo", mapOf(
                        "inputs" to listOf(
                            Input(title = "Username", value = userinfo.username),
                            Input(title = "Sub", value = userinfo.sub),
                            Input(title = "Name", value = userinfo.name),
                            Input(title = "Email", value = userinfo.email),
                        )
                    )))
                }
            }

            authenticate("keycloak") {
                post("/login") {}

                get(callbackPath) {
                    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                    val idToken = principal?.extraParameters?.get("id_token") ?: error("id_token not found in the response")

                    call.respond(ThymeleafContent("callback", mapOf("inputs" to listOf(
                        Input(title = "Access Token", name = "access_token", value = principal.accessToken),
                        Input(title = "Token Type", value = principal.tokenType),
                        Input(title = "ID Token", value = idToken),
                    ))))
                }
            }

            static("") {
                resources("css")
            }
        }
    }.start(wait = true)
}

data class Input(val title: String, val name: String = "", val value: String = "", val readonly: Boolean = true)