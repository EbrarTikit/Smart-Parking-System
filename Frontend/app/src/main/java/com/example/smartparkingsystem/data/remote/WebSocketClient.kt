package com.example.smartparkingsystem.data.remote

import android.util.Log
import com.example.smartparkingsystem.data.model.SensorUpdateDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class ParkingWebSocketClient(
    private val onMessageReceived: (SensorUpdateDto) -> Unit
) : WebSocketClient(URI("ws://localhost:8081/ws")) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(SensorUpdateDto::class.java)

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WebSocket", "Connection opened")
        // Subscribe to the parking spots topic
        send("SUBSCRIBE /topic/parking-spots")
    }

    override fun onMessage(message: String?) {
        Log.d("WebSocket", "Message received: $message")
        message?.let {
            try {
                val update = adapter.fromJson(it)
                update?.let { sensorUpdate ->
                    onMessageReceived(sensorUpdate)
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error parsing message: ${e.message}")
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("WebSocket", "Connection closed: $reason")
    }

    override fun onError(ex: Exception?) {
        Log.e("WebSocket", "WebSocket error: ${ex?.message}")
    }
}