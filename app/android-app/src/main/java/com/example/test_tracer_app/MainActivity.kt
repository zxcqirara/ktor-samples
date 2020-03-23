package com.example.test_tracer_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.ktor.client.features.tracing.Stetho
import io.ktor.client.request.get

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch {
            val client = HttpClient(Stetho(OkHttp))
            while (true) {
                client.get<String>("http://www.ya.ru/")
                delay(1000)
            }
        }
    }
}