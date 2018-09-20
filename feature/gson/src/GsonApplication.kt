package io.ktor.samples.gson

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import java.text.*
import java.time.*

data class Model(val name: String, val items: List<Item>, val date: LocalDate = LocalDate.of(2018, 4, 13))
data class Item(val key: String, val value: String)

val model = Model("root", listOf(Item("A", "Apache"), Item("B", "Bing")))

/**
 * Entry Point of the application. This function is referenced in the
 * resources/application.conf file inside the ktor.application.modules.
 *
 * For more information about this file: https://ktor.io/servers/configuration.html#hocon-file
 */
fun Application.main() {
    // This adds automatically Date and Server headers to each response, and would allow you to configure
    // additional headers served to each response.
    install(DefaultHeaders)
    // This feature enables compression automatically when accepted by the client.
    install(Compression)
    // This uses use the logger to log every call (request/response)
    install(CallLogging)
    // Based on the Accept header, allows to reply with arbitrary objects converting them into JSON
    // when the client accepts it.
    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    // Register all the routes available to this application.
    routing {
        // Registers a route for `/v1` handling GET requests:
        get("/v1") {
            call.respond(model)
        }
        // Registers a route for `/v1/item/{key}` handling GET requests:
        get("/v1/item/{key}") {
            // Tries to get a Item with the specified key.
            // To get a parameter from the URL, `call.parameters` is used.
            val item = model.items.firstOrNull { it.key == call.parameters["key"] }

            // If the item is not found (null), it replies with a 404 NotFound.
            // Otherwise it returns an arbitrary instance of [Item].
            // Since the ContentNegotiation feature is installed,
            // it will be processed and sent as JSON.
            if (item == null)
                call.respond(HttpStatusCode.NotFound)
            else
                call.respond(item)
        }
    }
}
