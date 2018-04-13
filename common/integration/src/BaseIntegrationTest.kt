package io.ktor.integration.tests

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.experimental.*
import org.intellij.lang.annotations.*
import org.junit.*
import java.util.concurrent.*
import kotlin.test.*

abstract class BaseIntegrationTest {
    open val port get(): Int = 8082
    abstract val module: Application.() -> Unit

    lateinit var appEngine: ApplicationEngine

    @Before
    fun before() {
        appEngine = embeddedServer(Netty, port, "127.0.0.1") {
            module()
        }
        appEngine.start(false)
        //Thread.sleep(50000L)
    }

    @After
    fun after() {

        appEngine.stop(1L, 1L, TimeUnit.SECONDS)
    }

    fun getRouteContent(route: String): String {
        return runBlocking {
            val client = HttpClient(Apache)
            client.call("http://127.0.0.1:$port/${route.trimStart('/')}").receive<String>()
        }
    }

    fun assertRouteEquals(route: String, expectedContent: String) {
        assertEquals(expectedContent, getRouteContent(route))
    }

    fun assertRouteMatchPattern(route: String, @Language("regex") expectedPattern: String) {
        val content = getRouteContent(route)
        assertTrue(Regex(expectedPattern).matches(content), "Route $route do not match $expectedPattern")

    }
}