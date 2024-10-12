package com.chau.iot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.Ack
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action != null && action == "ALARM_RECEIVER") {
            Log.d("AlarmReceiver", "Alarm broadcast received")

            context?.let {
                // Tạo lại kết nối MQTT hoặc thực hiện hành động ping MQTT
                val mqttHelper = MqttHelper(it)
                mqttHelper.connect() // Kết nối lại hoặc ping server
            }
        }
    }
}

class AlarmPingSender(val context: Context, val alarmManager: AlarmManager) {

    private var pendingIntent: PendingIntent? = null

    fun start() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "ALARM_RECEIVER"
        }
        pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val interval = 60 * 1000L // 60 giây

        pendingIntent?.let {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + interval,
                interval,
                it
            )
            Log.d("AlarmPingSender", "Ping alarm set up")
        } ?: Log.e("AlarmPingSender", "PendingIntent is null, cannot set up alarm")
    }

    fun stop() {
        pendingIntent?.let {
            alarmManager.cancel(it)
            Log.d("AlarmPingSender", "Ping alarm canceled")
        } ?: Log.e("AlarmPingSender", "PendingIntent is null, cannot cancel alarm")
    }
}
