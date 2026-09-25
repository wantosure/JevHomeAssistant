package com.jev.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.jev.assistant.data.LocalStorage
import com.jev.assistant.device.DeviceRegistry

class JevApp : Application() {

    companion object {
        const val CHANNEL_SERVICE_ID = "jev_assistant_service"
        const val CHANNEL_ALARM_ID = "jev_assistant_alarm"
        lateinit var instance: JevApp
            private set

        var isNativeSupported = false
            private set
    }

    lateinit var storage: LocalStorage
        private set
    lateinit var deviceRegistry: DeviceRegistry
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 全局崩溃守护：持久化记录异常堆栈至本地文件，方便重启后一键复制
        com.jev.assistant.utils.CrashReporter.install(this)

        // 安全加载 Sherpa 与 ONNX 原生依赖库
        try {
            System.loadLibrary("onnxruntime")
            System.loadLibrary("sherpa-onnx-jni")
            isNativeSupported = true
            android.util.Log.i("JevApp", "Native libraries (onnxruntime + sherpa-onnx-jni) loaded successfully!")
        } catch (e: Throwable) {
            isNativeSupported = false
            android.util.Log.e("JevApp", "Native libraries load failed: ${e.message}. Will fallback to system ASR gracefully.", e)
        }

        storage = LocalStorage(this)
        deviceRegistry = DeviceRegistry(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Jev 持续语音监听前台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持 24 小时后台麦克风监听并显示当前状态"
                setShowBadge(false)
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Jev 本地闹钟提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "本地设定的闹钟到点响铃提醒"
                enableVibration(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alarmChannel)
        }
    }
}
