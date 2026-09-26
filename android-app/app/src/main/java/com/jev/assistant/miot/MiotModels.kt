package com.jev.assistant.miot

import com.google.gson.annotations.SerializedName

/**
 * 米家云接口的数据传输对象。
 *
 * 字段名与 JSON 键严格对应（`@SerializedName`），字段本身来自协议事实。
 * 服务端只会返回下列字段的子集，未出现的字段保持默认值。
 */

/** 单台设备。 */
data class MiotDeviceDto(
    @SerializedName("did") val did: String = "",
    @SerializedName("name") val name: String = "",
    /** 设备规范 URN；接口字段名是 `spec_type`，不是 `urn`。 */
    @SerializedName("spec_type") val specType: String? = null,
    @SerializedName("model") val model: String = "",
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("pid") val pid: Int? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean = false,
    @SerializedName("local_ip") val localIp: String? = null,
    @SerializedName("parent_id") val parentId: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("rssi") val rssi: Int? = null,
) {
    /** 是否为子设备（did 形如 `123456.s1`）。 */
    val subDeviceKey: String?
        get() = SUB_DEVICE_SUFFIX.find(did)?.groupValues?.get(1)

    /** 去掉子设备后缀的父设备 did；非子设备时为 null。 */
    val parentDidFromSuffix: String?
        get() = SUB_DEVICE_SUFFIX.find(did)?.let { did.substring(0, it.range.first) }

    companion object {
        /**
         * 子设备 did 的后缀形态，如 `.s1`。
         *
         * 捕获组含 `s` 前缀（得到 `s1` 而非 `1`），与协议中通道号的书写一致。
         */
        private val SUB_DEVICE_SUFFIX = Regex("\\.(s\\d+)$")
    }
}

/** `POST /app/v2/home/device_list_page` 的响应体。 */
data class MiotDeviceListPageDto(
    @SerializedName("list") val list: List<MiotDeviceDto> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("next_start_did") val nextStartDid: String? = null,
)

/** 家庭下的一个房间。 */
data class MiotRoomDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("dids") val dids: List<String> = emptyList(),
)

/** 一个家庭。 */
data class MiotHomeDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("roomlist") val roomList: List<MiotRoomDto> = emptyList(),
    /** 不属于任何房间、直接挂在家下的设备。 */
    @SerializedName("dids") val dids: List<String> = emptyList(),
    @SerializedName("shareflag") val shareFlag: Int = 0,
)

/** `POST /app/v2/homeroom/gethome` 的响应体。 */
data class MiotHomeListDto(
    @SerializedName("homelist") val homeList: List<MiotHomeDto> = emptyList(),
    @SerializedName("share_home_list") val shareHomeList: List<MiotHomeDto> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("max_id") val maxId: String? = null,
)

/**
 * `POST /app/v2/homeroom/get_dev_room_page` 的响应体。
 *
 * 注意其列表字段是 `info`，与 `gethome` 的 `homelist` 不同。
 */
data class MiotDevRoomPageDto(
    @SerializedName("info") val info: List<MiotHomeDto> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("max_id") val maxId: String? = null,
)

/** 读属性请求中的单项。 */
data class MiotPropQuery(
    @SerializedName("did") val did: String,
    @SerializedName("siid") val siid: Int,
    @SerializedName("piid") val piid: Int,
)

/** 写属性请求中的单项。 */
data class MiotPropWrite(
    @SerializedName("did") val did: String,
    @SerializedName("siid") val siid: Int,
    @SerializedName("piid") val piid: Int,
    @SerializedName("value") val value: Any?,
)

/** 读属性的单条结果。 */
data class MiotPropValueDto(
    @SerializedName("did") val did: String = "",
    @SerializedName("siid") val siid: Int = 0,
    @SerializedName("piid") val piid: Int = 0,
    @SerializedName("value") val value: Any? = null,
    @SerializedName("code") val code: Int = 0,
    @SerializedName("updateTime") val updateTime: Long? = null,
)

/** 写属性的单条结果（**没有 value 字段**）。 */
data class MiotPropWriteResultDto(
    @SerializedName("did") val did: String = "",
    @SerializedName("siid") val siid: Int = 0,
    @SerializedName("piid") val piid: Int = 0,
    @SerializedName("code") val code: Int = 0,
)

/** 换令牌响应中的结果体。 */
data class MiotTokenResultDto(
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("refresh_token") val refreshToken: String = "",
    @SerializedName("expires_in") val expiresIn: Long = 0,
)

/** 服务端统一外层信封（已解密的明文形态）。 */
data class MiotEnvelopeDto<T>(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("result") val result: T? = null,
)

/** 换令牌接口的外层信封（明文返回，不走 AES 信封）。 */
data class MiotTokenEnvelopeDto(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("result") val result: MiotTokenResultDto? = null,
)

/** 按型号反查 URN 的响应。 */
data class MiotUrnLookupDto(
    @SerializedName("urn") val urn: String? = null,
)
