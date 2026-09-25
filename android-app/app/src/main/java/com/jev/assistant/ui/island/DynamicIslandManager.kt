package com.jev.assistant.ui.island

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.jev.assistant.JevApp
import com.jev.assistant.MainActivity
import com.jev.assistant.R

/**
 * 小米 HyperOS 焦点通知与全局悬浮灵动岛控制器 (需求 3)
 * 当应用在后台运行且执行智能家居操作时调用
 */
object DynamicIslandManager {
    private const val TAG = "DynamicIsland"
    private const val ISLAND_NOTIFICATION_ID = 2002
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeIslandView: View? = null
    private var windowManager: WindowManager? = null

    /**
     * 触发灵动岛展示
     * @param room 房间名称 (如 "主卧")
     * @param deviceName 设备名称 (如 "主卧主灯")
     * @param actionDesc 动作说明 (如 "已开启 · 亮度80%")
     * @param cost 本次会话计费 (如 0.0020)
     */
    fun showIsland(
        context: Context,
        room: String,
        deviceName: String,
        actionDesc: String,
        cost: Double = 0.0020
    ) {
        mainHandler.post {
            try {
                // 1. 发射小米 HyperOS 官方焦点通知 (HyperOS Focus Notification)
                postHyperOsFocusNotice(context, room, deviceName, actionDesc, cost)

                // 2. 如果具备悬浮窗权限，弹出屏幕顶部沉浸式灵动岛药丸
                if (Settings.canDrawOverlays(context)) {
                    showOverlayIsland(context, room, deviceName, actionDesc, cost)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "触发灵动岛异常: ${t.message}", t)
            }
        }
    }

    /**
     * 小米 HyperOS 官方焦点通知 (状态栏胶囊)
     */
    private fun postHyperOsFocusNotice(
        context: Context,
        room: String,
        deviceName: String,
        actionDesc: String,
        cost: Double
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ISLAND_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 适配小米 HyperOS 焦点通知 Extra 协议
        val extra = Bundle().apply {
            putBoolean("miui.focusNotice", true)
            putString("miui.focusState", "active")
            putString("miui.focusType", "smart_home")
            putString("miui.focusTitle", "💡 [$room] $deviceName")
            putString("miui.focusContent", "$actionDesc (¥${String.format("%.4f", cost)})")
        }

        val notification = NotificationCompat.Builder(context, JevApp.CHANNEL_SERVICE_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("💡 [$room] $deviceName")
            .setContentText("$actionDesc · 消耗 ¥${String.format("%.4f", cost)}")
            .setSubText("Jev 智能家居")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addExtras(extra)
            .build()

        manager.notify(ISLAND_NOTIFICATION_ID, notification)

        // 5秒后清除焦点通知
        mainHandler.postDelayed({
            try {
                manager.cancel(ISLAND_NOTIFICATION_ID)
            } catch (_: Exception) {}
        }, 5000)
    }

    /**
     * 屏幕顶部居中纯黑磨砂小米灵动岛悬浮药丸
     */
    private fun showOverlayIsland(
        context: Context,
        room: String,
        deviceName: String,
        actionDesc: String,
        cost: Double
    ) {
        dismissCurrentOverlay()

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 36 // 避开前置摄像头挖孔与顶部边距
        }

        // 构建原生仿小米灵动岛药丸容器
        val islandLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(36, 18, 36, 18)

            // 灵动岛纯黑药丸圆角背景 + 青绿流光边框
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
                setColor(Color.parseColor("#121214"))
                setStroke(3, Color.parseColor("#06B6D4"))
            }
            background = bg

            setOnClickListener {
                dismissCurrentOverlay()
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
        }

        // 设备图标
        val iconView = TextView(context).apply {
            text = "💡"
            textSize = 15f
            setPadding(0, 0, 16, 0)
        }

        // 设备与房间文本
        val titleView = TextView(context).apply {
            text = "[$room] $deviceName"
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 14, 0)
        }

        // 状态说明文本
        val descView = TextView(context).apply {
            text = actionDesc
            setTextColor(Color.parseColor("#10B981")) // 翡翠绿
            textSize = 12f
            setPadding(0, 0, 16, 0)
        }

        // 价格标签 (需求 4)
        val costView = TextView(context).apply {
            text = "¥${String.format("%.4f", cost)}"
            setTextColor(Color.parseColor("#F59E0B")) // 琥珀黄
            textSize = 11f
            val costBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor("#262010"))
            }
            background = costBg
            setPadding(12, 4, 12, 4)
        }

        islandLayout.addView(iconView)
        islandLayout.addView(titleView)
        islandLayout.addView(descView)
        islandLayout.addView(costView)

        try {
            wm.addView(islandLayout, layoutParams)
            activeIslandView = islandLayout

            // 3.5秒后平滑缩回收起
            mainHandler.postDelayed({
                dismissCurrentOverlay()
            }, 3500)
        } catch (e: Exception) {
            Log.e(TAG, "添加灵动岛悬浮窗失败: ${e.message}", e)
        }
    }

    private fun dismissCurrentOverlay() {
        try {
            activeIslandView?.let { view ->
                windowManager?.removeViewImmediate(view)
            }
        } catch (_: Exception) {}
        activeIslandView = null
        windowManager = null
    }
}
