package com.jev.assistant.miot

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 设备目录的本地快照。
 *
 * 缓存的是**接口原始数据**（家庭与设备 DTO），而不是加工后的设备模型。
 * 这样离线启动时可以用与在线同步完全相同的构建逻辑重建设备，
 * 避免出现"缓存里的字段和在线时不一致"这类难查的问题。
 *
 * 能力信息不在这里缓存——它由 [MiotSpecRepository] 按 URN 单独缓存，
 * 那份缓存没有过期时间，离线时同样可用。
 */
class MiotDeviceCache(private val cacheDir: File) {

    private val gson = Gson()

    private data class Snapshot(
        val homes: List<MiotHomeDto> = emptyList(),
        val devices: List<MiotDeviceDto> = emptyList(),
        val savedAtSec: Long = 0,
    )

    suspend fun save(homes: List<MiotHomeDto>, devices: List<MiotDeviceDto>) = withContext(Dispatchers.IO) {
        runCatching {
            cacheDir.mkdirs()
            val snapshot = Snapshot(homes, devices, System.currentTimeMillis() / 1000)
            val target = File(cacheDir, FILE_NAME)
            val temp = File(cacheDir, "$FILE_NAME.tmp")
            temp.writeText(gson.toJson(snapshot))
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        }.onFailure { Log.w(TAG, "写入设备缓存失败", it) }
    }

    suspend fun load(): Pair<List<MiotHomeDto>, List<MiotDeviceDto>>? = withContext(Dispatchers.IO) {
        val file = File(cacheDir, FILE_NAME)
        if (!file.isFile) return@withContext null

        runCatching {
            val type = object : TypeToken<Snapshot>() {}.type
            val snapshot = gson.fromJson<Snapshot>(file.readText(), type)
            if (snapshot == null || snapshot.devices.isEmpty()) {
                null
            } else {
                snapshot.homes to snapshot.devices
            }
        }.onFailure { Log.w(TAG, "读取设备缓存失败", it) }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { File(cacheDir, FILE_NAME).delete() }
        Unit
    }

    private companion object {
        const val TAG = "MiotDeviceCache"
        const val FILE_NAME = "devices.json"
    }
}
