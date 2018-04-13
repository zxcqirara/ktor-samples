package io.ktor.samples.testable

import io.ktor.application.*
import io.ktor.integration.tests.*
import org.junit.*

class IntegrationTest : BaseIntegrationTest() {
    override val module = Application::main

    @Test fun root() = assertRouteEquals("/", "Hello from Ktor Testable sample application")
}