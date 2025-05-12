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
) : WebSocketClient(URI("ws://10.0.2.2:8081/ws/websocket")) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(SensorUpdateDto::class.java)

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WebSocket", "Connection opened successfully")
        Log.d("WebSocket", "Server handshake status: ${handshakedata?.httpStatus}")
        Log.d("WebSocket", "Server handshake message: ${handshakedata?.httpStatusMessage}")

        // STOMP bağlantı mesajı
        val connectFrame = """
            CONNECT
            accept-version:1.1,1.0
            heart-beat:10000,10000

            
        """.trimIndent() + "\u0000"  // Fix null byte terminator

        send(connectFrame)
        Log.d("WebSocket", "Sent STOMP CONNECT frame")

        // Subscribe mesajı
        val subscribeFrame = """
            SUBSCRIBE
            id:sub-0
            destination:/topic/parking-spots

            
        """.trimIndent() + "\u0000"  // Fix null byte terminator

        send(subscribeFrame)
        Log.d("WebSocket", "Sent STOMP SUBSCRIBE frame")
    }

    override fun onMessage(message: String?) {
        Log.d("WebSocket", "Raw message received: $message")
        message?.let {
            try {
                // STOMP frame'ini parse et
                if (message.startsWith("MESSAGE")) {
                    // Split based on double newline to get the body section
                    val parts = message.split("\n\n", limit = 2)
                    if (parts.size > 1) {
                        val messageBody = parts[1].trim().replace("\u0000", "")
                        Log.d("WebSocket", "Message body: $messageBody")
                        
                        val update = adapter.fromJson(messageBody)
                        update?.let { sensorUpdate ->
                            Log.d("WebSocket", "Parsed update - Spot ID: ${sensorUpdate.id}, Occupied: ${sensorUpdate.occupied}")
                            onMessageReceived(sensorUpdate)
                        }
                    } else {
                        Log.e("WebSocket", "Failed to parse message body: No body part found")
                    }
                } else {
                    Log.d("WebSocket", "Received non-MESSAGE frame: $message")
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error parsing message: ${e.message}")
                Log.e("WebSocket", "Failed message content: $message")
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("WebSocket", "Connection closed - Code: $code, Reason: $reason, Remote: $remote")
        // Bağlantı kapandığında yeniden bağlanmayı dene
        Log.d("WebSocket", "Attempting to reconnect...")
        reconnect()
    }

    override fun onError(ex: Exception?) {
        Log.e("WebSocket", "WebSocket error occurred")
        Log.e("WebSocket", "Error message: ${ex?.message}")
        Log.e("WebSocket", "Error stack trace: ${ex?.stackTraceToString()}")
    }
}