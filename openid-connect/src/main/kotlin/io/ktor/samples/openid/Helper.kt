package io.ktor.samples.openid

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.engine.*

fun ApplicationCall.urlWithPath(path: String): Url {
    val env = (application.environment as ApplicationEngineEnvironment)

    if (env.connectors.isEmpty()) {
        throw IllegalStateException("No server connectors found but at least one expected")
    }

    val connector = env.connectors.first()

    return URLBuilder(
        host = connector.host,
        port = connector.port,
        encodedPath = path
    ).build()
}