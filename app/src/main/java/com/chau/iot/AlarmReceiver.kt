package com.chau.iot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.eclipse.paho.android.service.MqttService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action != null && action == "ALARM_RECEIVER") {
            // Đây là nơi xử lý nếu nhận được broadcast từ MQTT Alarm Ping Sender
            val mqttServiceIntent = Intent(context, MqttService::class.java)
            context?.startService(mqttServiceIntent)
        }
    }
}

class AlarmPingSender(val context: Context, val alarmManager: AlarmManager) {

    private var pendingIntent: PendingIntent? = null

    fun start() {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.action = "ALARM_RECEIVER"
        pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Sử dụng FLAG_IMMUTABLE nếu cần cho các phiên bản Android mới hơn
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
        try {
            // Kiểm tra nếu pendingIntent không null trước khi hủy
            pendingIntent?.let {
                alarmManager.cancel(it)
                Log.d("AlarmPingSender", "Ping alarm canceled")
            } ?: Log.e("AlarmPingSender", "PendingIntent is null, cannot cancel alarm")
        } catch (e: Exception) {
            Log.e("AlarmPingSender", "Error while canceling alarm: ${e.message}")
        }
    }
}
