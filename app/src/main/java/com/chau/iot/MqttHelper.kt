package com.chau.iot

import android.content.Context
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.Ack
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

internal class MqttHelper(context: Context?) {
    private val serverUri = "ssl://71fe86b9428f4263bfa754229cd53053.s1.eu.hivemq.cloud:8883"
    private val clientId = MqttClient.generateClientId()
    private val username = "arduino"
    private val password = "Chau2003"

    // Topics
    private val subscriptionTopic = "arduino/topic"
    private val subscriptionTopics = listOf("nhietdo/topic", "doam/topic")

    private var isConnecting = false

    var mqttAndroidClient = MqttAndroidClient(context!!, serverUri, clientId, Ack.AUTO_ACK)

    // Callbacks
    var onTemperatureReceived: ((String) -> Unit)? = null
    var onHumidityReceived: ((String) -> Unit)? = null
    var onArduinoMessageReceived: ((String) -> Unit)? = null

    init {
        mqttAndroidClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                Log.d("MQTT", "Connection lost, attempting to reconnect")
                reconnect()
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = message.toString()
                Log.d("MQTT", "Message arrived from $topic: $payload")
                when (topic) {
                    "nhietdo/topic" -> onTemperatureReceived?.invoke(payload)
                    "doam/topic" -> onHumidityReceived?.invoke(payload)
                    "arduino/topic" -> onArduinoMessageReceived?.invoke(payload)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                Log.d("MQTT", "Delivery complete")
            }
        })
        connect()
    }

    internal fun connect() {
        if (isConnecting || mqttAndroidClient.isConnected) return
        val options = MqttConnectOptions().apply {
            userName = username
            password = this@MqttHelper.password.toCharArray()
            isCleanSession = true
            connectionTimeout = 10
        }

        isConnecting = true
        try {
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("MQTT", "Successfully connected")
                    isConnecting = false
                    subscribeToTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTT", "Connection failed: ${exception.message}", exception)
                    isConnecting = false
                    reconnect()
                }
            })
        } catch (ex: MqttException) {
            Log.e("MQTT", "Exception during connect: ${ex.message}", ex)
            isConnecting = false
        }
    }

    private fun subscribeToTopic(topic: String) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("MQTT", "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTT", "Failed to subscribe to $topic: ${exception.message}")
                }
            })
        } catch (ex: MqttException) {
            Log.e("MQTT", "Exception during subscription to $topic: ${ex.message}", ex)
        }
    }

    private fun subscribeToTopics() {
        (listOf(subscriptionTopic) + subscriptionTopics).forEach { topic ->
            subscribeToTopic(topic)
        }
    }

    fun publishMessage(message: String) {
        try {
            if (!mqttAndroidClient.isConnected) {
                Log.e("MQTT", "Client not connected, cannot publish message.")
                return
            }
            val mqttMessage = MqttMessage().apply {
                payload = message.toByteArray()
            }
            Log.d("MQTT", "Publishing message: $message to topic: $subscriptionTopic")
            mqttAndroidClient.publish(subscriptionTopic, mqttMessage)
        } catch (e: MqttException) {
            Log.e("MQTT", "Publish failed: ${e.message}", e)
        }
    }

    private fun reconnect() {
        if (!mqttAndroidClient.isConnected) {
            Log.d("MQTT", "Reconnecting in 5 seconds...")
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                Log.e("MQTT", "Reconnect interrupted: ${e.message}", e)
            }
            connect()
        }
    }
}
