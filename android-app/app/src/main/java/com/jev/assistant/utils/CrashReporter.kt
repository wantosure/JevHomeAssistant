package com.jev.assistant.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val TAG = "CrashReporter"
    private const val CRASH_FILE_NAME = "last_crash_log.txt"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(context, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "记录崩溃日志失败: ${e.message}", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun recordCrash(context: Context, thread: Thread?, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        val stackTrace = sw.toString()

        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("===== Jev 智能助手 异常崩溃报告 =====\n")
        sb.append("时间: $timeStr\n")
        sb.append("设备品牌: ${Build.BRAND} (${Build.MANUFACTURER})\n")
        sb.append("设备型号: ${Build.MODEL} (${Build.PRODUCT})\n")
        sb.append("系统版本: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("CPU 架构: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")
        sb.append("发生线程: ${thread?.name ?: "Unknown"}\n")
        sb.append("异常类型: ${throwable.javaClass.name}\n")
        sb.append("异常信息: ${throwable.message}\n")
        sb.append("---------------- 堆栈详情 ----------------\n")
        sb.append(stackTrace)
        sb.append("\n=========================================\n")

        val crashFile = File(context.filesDir, CRASH_FILE_NAME)
        crashFile.writeText(sb.toString(), Charsets.UTF_8)
        Log.e(TAG, "已成功写入崩溃日志至: ${crashFile.absolutePath}")
    }

    fun getCrashLog(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists() && file.length() > 0) {
            file.readText(Charsets.UTF_8)
        } else {
            null
        }
    }

    fun clearCrashLog(context: Context) {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    fun generateDiagnosticReport(context: Context, asrInfo: String): String {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("===== Jev 智能助手 运行与诊断报告 =====\n")
        sb.append("生成时间: $timeStr\n")
        sb.append("设备品牌: ${Build.BRAND} (${Build.MANUFACTURER})\n")
        sb.append("设备型号: ${Build.MODEL} (${Build.PRODUCT})\n")
        sb.append("系统版本: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("CPU 架构: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")
        sb.append("当前 ASR 状态: $asrInfo\n")
        val crash = getCrashLog(context)
        if (crash != null) {
            sb.append("\n【抓取到的上次崩溃堆栈】:\n")
            sb.append(crash)
        } else {
            sb.append("\n【上次崩溃堆栈】: 暂无崩溃记录 (无未捕获致命异常)\n")
        }
        sb.append("\n=========================================\n")
        return sb.toString()
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("JevReport", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            Log.e(TAG, "复制到剪贴板失败: ${e.message}", e)
            false
        }
    }
}
