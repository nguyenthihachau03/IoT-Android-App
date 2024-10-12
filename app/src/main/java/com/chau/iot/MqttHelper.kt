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
    private val serverUri = "ssl://368cf7d2de0e4603b0f7058e3cc51252.s1.eu.hivemq.cloud:8883"
    private val clientId = MqttClient.generateClientId()
    private val username = "arduino"
    private val password = "Chau2003"
    private val subscriptionTopic = "arduino/topic"
    private var isConnecting = false

    var mqttAndroidClient = MqttAndroidClient(context!!, serverUri, clientId, Ack.AUTO_ACK)

    init {
        mqttAndroidClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                Log.d("MQTT", "Connection lost, attempting to reconnect")
                reconnect()
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                Log.d("MQTT", "Message arrived: $message")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                Log.d("MQTT", "Delivery complete")
            }
        })
        connect() // gọi connect trong init
    }

    // Thay đổi phạm vi của connect từ private thành internal hoặc public
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
            Log.d("MQTT", "Attempting to connect to $serverUri")
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("MQTT", "Successfully connected")
                    isConnecting = false
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTT", "Connection failed: ${exception.message}")
                    isConnecting = false
                    reconnect()
                }
            })
        } catch (ex: MqttException) {
            ex.printStackTrace()
            isConnecting = false
        }
    }

    private fun reconnect() {
        if (!mqttAndroidClient.isConnected) {
            Log.d("MQTT", "Reconnecting in 5 seconds...")
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            connect()
        }
    }

    private fun subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("MQTT", "Subscribed to $subscriptionTopic")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTT", "Failed to subscribe: ${exception.message}")
                }
            })
        } catch (ex: MqttException) {
            ex.printStackTrace()
        }
    }

    fun publishMessage(message: String) {
        try {
            val mqttMessage = MqttMessage().apply {
                payload = message.toByteArray()
            }
            Log.d("MQTT", "Publishing message: $message to topic: $subscriptionTopic")
            mqttAndroidClient.publish(subscriptionTopic, mqttMessage)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttAndroidClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("MQTT", "Successfully disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTT", "Failed to disconnect: ${exception.message}")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}
