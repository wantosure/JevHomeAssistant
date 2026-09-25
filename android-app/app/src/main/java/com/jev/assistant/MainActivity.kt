package com.jev.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.jev.assistant.service.VoiceAssistantService
import com.jev.assistant.ui.screens.MainScreen
import com.jev.assistant.ui.theme.JevAssistantTheme
import com.jev.assistant.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            startAssistantService()
            viewModel.audioPipeline.startListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 保持屏幕常亮选项（用于作为桌面插电 7×24H 放置设备时可选）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            JevAssistantTheme {
                MainScreen(viewModel = viewModel)
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            startAssistantService()
            try {
                viewModel.audioPipeline.startListening()
            } catch (t: Throwable) {
                android.util.Log.e("MainActivity", "Failed to start listening: ${t.message}", t)
            }
        } else {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun startAssistantService() {
        try {
            VoiceAssistantService.start(this)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity 销毁时不强杀前台服务，实现后台持续监听目标
    }
}
