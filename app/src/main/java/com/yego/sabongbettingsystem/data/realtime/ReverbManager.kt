package com.yego.sabongbettingsystem.data.realtime

import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import org.json.JSONObject

object ReverbManager {

    private const val HOST    = "192.168.1.10"
    private const val PORT    = 8080
    private const val APP_KEY = "lt6ejfvgbim9vntnqxms"

    private var pusher  : Pusher?  = null
    private var channel : Channel? = null

    var onFightUpdated   : ((JSONObject) -> Unit)? = null
    var onBetPlaced      : ((JSONObject) -> Unit)? = null
    var onWinnerDeclared : ((JSONObject) -> Unit)? = null
    var onTellerCashUpdated : ((JSONObject) -> Unit)? = null
    var onConnected      : (() -> Unit)?           = null
    var onDisconnected   : (() -> Unit)?           = null

    fun isConnected(): Boolean =
        pusher?.connection?.state == ConnectionState.CONNECTED

    fun connect() {
        if (isConnected()) {
            onConnected?.invoke()
            return
        }

        pusher?.disconnect()
        pusher  = null
        channel = null

        val options = PusherOptions().apply {
            setHost(HOST)
            setWsPort(PORT)
            isUseTLS = false
        }

        pusher = Pusher(APP_KEY, options)

        pusher!!.connection.bind(ConnectionState.ALL, object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                android.util.Log.d(
                    "Reverb",
                    "WS: ${change.previousState} -> ${change.currentState}"
                )
                when (change.currentState) {
                    ConnectionState.CONNECTED -> {
                        subscribeToChannel()
                        onConnected?.invoke()
                    }
                    ConnectionState.DISCONNECTED -> {
                        // only fire disconnected if we were previously connected
                        if (change.previousState == ConnectionState.CONNECTED) {
                            onDisconnected?.invoke()
                        }
                    }
                    // CONNECTING, RECONNECTING — don't change badge
                    else -> {}
                }
            }

            override fun onError(
                message: String?,
                code: String?,
                e: Exception?
            ) {
                android.util.Log.e("Reverb", "WS Error: $message")
                onDisconnected?.invoke()
            }
        })

        pusher!!.connect()
    }

    private fun subscribeToChannel() {
        if (channel != null) return

        channel = pusher!!.subscribe("fights", object : ChannelEventListener {
            override fun onSubscriptionSucceeded(channelName: String) {
                android.util.Log.d("Reverb", "Subscribed to: $channelName ✅")
            }
            override fun onEvent(event: PusherEvent) {
                android.util.Log.d("Reverb", "Event: ${event.eventName}")
            }
        })

        channel!!.bind("fight.updated") { event ->
            android.util.Log.d("Reverb", "fight.updated: ${event.data}")
            try {
                val raw     = JSONObject(event.data)
                val payload = if (raw.has("data")) raw.getJSONObject("data") else raw
                android.util.Log.d("Reverb", "meron_status: ${payload.optString("meron_status")}")
                android.util.Log.d("Reverb", "wala_status: ${payload.optString("wala_status")}")
                onFightUpdated?.invoke(payload)
            } catch (e: Exception) {
                android.util.Log.e("Reverb", "Parse error: ${e.message}")
            }
        }

        channel!!.bind("bet.placed") { event ->
            android.util.Log.d("Reverb", "bet.placed: ${event.data}")
            try {
                val raw     = JSONObject(event.data)
                val payload = if (raw.has("data")) raw.getJSONObject("data") else raw
                onBetPlaced?.invoke(payload)
            } catch (e: Exception) {
                android.util.Log.e("Reverb", "Parse error: ${e.message}")
            }
        }

        channel!!.bind("winner.declared") { event ->
            android.util.Log.d("Reverb", "winner.declared: ${event.data}")
            try {
                val raw     = JSONObject(event.data)
                val payload = if (raw.has("data")) raw.getJSONObject("data") else raw
                onWinnerDeclared?.invoke(payload)
            } catch (e: Exception) {
                android.util.Log.e("Reverb", "Parse error: ${e.message}")
            }
        }

        // Subscribe to cash-status channel for real-time teller updates
        val cashStatusChannel = pusher!!.subscribe("cash-status", object : ChannelEventListener {
            override fun onSubscriptionSucceeded(channelName: String) {
                android.util.Log.d("Reverb", "Subscribed to: $channelName ✅")
            }
            override fun onEvent(event: PusherEvent) {
                android.util.Log.d("Reverb", "Event: ${event.eventName}")
            }
        })

        cashStatusChannel.bind("teller.cash-updated") { event ->
            android.util.Log.d("Reverb", "teller.cash-updated: ${event.data}")
            try {
                val raw     = JSONObject(event.data)
                val payload = if (raw.has("data")) raw.getJSONObject("data") else raw
                onTellerCashUpdated?.invoke(payload)
            } catch (e: Exception) {
                android.util.Log.e("Reverb", "Parse error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        channel = null
        pusher?.disconnect()
        pusher  = null
    }
}