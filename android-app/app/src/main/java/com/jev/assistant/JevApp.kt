package com.jev.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.jev.assistant.data.LocalStorage
import com.jev.assistant.device.AdapterStatus
import com.jev.assistant.device.DeviceRegistry
import com.jev.assistant.miot.MiotCloudClient
import com.jev.assistant.miot.MiotCredentials
import com.jev.assistant.miot.MiotDeviceCache
import com.jev.assistant.miot.MiotDeviceRepository
import com.jev.assistant.miot.MiotSpecRepository
import com.jev.assistant.miot.auth.MiotAccountManager
import com.jev.assistant.miot.auth.MiotAuthStore
import com.jev.assistant.miot.auth.MiotBindStatus
import com.jev.assistant.miot.auth.MiotOAuthClient
import com.jev.assistant.miot.auth.MiotTokenStore
import java.io.File

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

    lateinit var miotAccountManager: MiotAccountManager
        private set
    lateinit var miotCloudClient: MiotCloudClient
        private set
    lateinit var miotSpecRepository: MiotSpecRepository
        private set
    lateinit var miotDeviceRepository: MiotDeviceRepository
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
        deviceRegistry = DeviceRegistry()

        setUpMiot()

        createNotificationChannels()
    }

    /**
     * 装配米家云接入。
     *
     * 依赖关系存在环（云客户端需要令牌续期，账号管理需要云客户端换令牌），
     * 因此令牌来源以 lambda 注入，在真正发起请求时才求值。
     */
    private fun setUpMiot() {
        // 凭据目前取默认值；用户自定义 client_id 与区域在设置页接入后由此读取
        val credentials: () -> MiotCredentials = { MiotCredentials() }

        miotCloudClient = MiotCloudClient(
            credentialsProvider = credentials,
            accessTokenProvider = { miotAccountManager.currentAccessToken() },
            onUnauthorized = {
                miotAccountManager.ensureFresh(force = true).getOrNull()?.accessToken
            },
        )

        val store: MiotAuthStore = MiotTokenStore(this)
        miotAccountManager = MiotAccountManager(
            store = store,
            oauth = MiotOAuthClient(credentials, miotCloudClient),
            credentialsProvider = credentials,
            // 绑定状态一旦变化就切回观察模式：换了账号环境后，
            // 不允许沿用上一轮可能已是 LIVE 的模式直接对真实设备下发指令。
            onBindingChanged = { storage.setLiveMode(false) },
        )

        val cacheRoot = File(filesDir, "miot")
        miotSpecRepository = MiotSpecRepository.create(File(cacheRoot, "specs"), miotCloudClient)
        miotDeviceRepository = MiotDeviceRepository(
            cloud = miotCloudClient,
            specRepository = miotSpecRepository,
            cache = MiotDeviceCache(File(cacheRoot, "devices")),
        )

        miotAccountManager.initialize()
        deviceRegistry.updateAdapterStatus(
            when (miotAccountManager.status.value) {
                is MiotBindStatus.Bound -> AdapterStatus.Syncing
                is MiotBindStatus.Expired ->
                    AdapterStatus.AuthExpired("授权已失效，请重新绑定米家账号")

                MiotBindStatus.Unbound -> AdapterStatus.Unbound
            },
        )
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
