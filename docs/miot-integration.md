# 米家（MIoT）云接入说明

版本：1.0 · 2026-09-26

本文说明 Android 客户端如何接入米家云：授权流程、加密信封、设备同步、能力解析与安全门控。
它是 `android-app/app/src/main/java/com/jev/assistant/miot/` 下代码的设计依据。

---

## 1. 许可证边界（重要）

本项目是 MIT 许可的开源项目。米家的接入实现**完全由本项目独立编写**，
仅依据公开的接口行为与公开的设备规范，不包含任何第三方 SDK 的代码。

### 1.1 采信的内容（协议事实，不受版权保护）

- HTTP 端点路径、请求头名称、字段名
- 加密方案本身：RSA 包裹会话密钥、AES-128-CBC、IV 等于密钥、PKCS7 填充
- 服务端返回的错误码数值及其含义
- `miot-spec.org` 公开规范的字段结构（`iid` / `type` / `access` / `format` / `value-range` / `value-list`）
- MIoT 服务端公钥：这是公开的**互操作密钥材料**（公钥本身不保密），多个开源米家实现通用

### 1.2 未采信的内容

- 任何 SDK 的类划分、方法命名与内部实现结构
- 其 YAML 配置文件（`bool_trans` / `spec_filter` / `spec_modify`）
- 其缓存策略、并发聚合窗口、键匹配算法等具体实现选择
- 其文档文字与提示语

### 1.3 关于默认 client_id

`MiotCredentials.DEFAULT_CLIENT_ID` 的默认值是公开实现中通用的互操作标识，
**不是本项目所有的凭据**，也非本项目注册。它：

- 可在「设置 → 米家账号」中替换为你自己的小米开放平台应用凭据（见 `MiotCredentials`）；
- 使用他人注册的应用凭据存在被限流或停用的风险；
- 若你计划商业使用，请自行到小米开放平台申请凭据，并自行确认 `miot-spec.org` 的数据使用条款。

---

## 2. 分层结构

```
miot/
├── MiotConfig.kt            端点、区域、限额、可配置凭据
├── MiotCrypto.kt            加密信封（纯函数，可离线测试）
├── MiotModels.kt            接口 DTO
├── MiotErrorMapper.kt       错误码 → 设备层错误归类
├── MiotSpec.kt              能力模型 + iid 转换
├── MiotSpecParser.kt        规范 JSON → 能力模型
├── MiotCapabilityMapper.kt  规范属性 → 本项目动作词表
├── MiotTypeTable.kt         URN 类别 → 中文类型/图标/分组类别
├── MiotSpecRepository.kt    规范获取与磁盘缓存
├── MiotCloudClient.kt       唯一 HTTP 出口
├── MiotActionTranslator.kt  动作 → 属性写入（含参数校验）
├── MiotDeviceRepository.kt  家庭/房间/设备同步
├── MiotDeviceCache.kt       设备目录本地快照
├── auth/                    授权：OAuth、令牌存储、状态机
└── adapter/                 DeviceAdapter 实现
```

**依赖方向**：`miot` 依赖 `device`（使用其 `DeviceErrorKind` 与 `DeviceAdapter` 契约），
反之不成立。这样执行链路不依赖米家这一具体接入方式。

---

## 3. 协议要点

### 3.1 主机与区域

| 用途 | 地址 |
|---|---|
| 用户登录授权 | `https://account.xiaomi.com/oauth2/authorize` |
| 回调展示页（固定） | `https://mico.api.mijia.tech/login_redirect` |
| 业务 API（中国大陆） | `https://mico.api.mijia.tech` |
| 业务 API（海外） | `https://{de\|us\|sg\|ru\|i2}.mico.api.mijia.tech` |

### 3.2 加密信封

业务接口（设备、家庭、属性）的请求体与响应体都经过加密：

1. 每次会话随机生成 16 字节 AES 密钥（进程内固定，不落盘）；
2. 用 MIoT 公钥以 `RSA/ECB/PKCS1Padding` 包裹该密钥，base64 后放入 `X-Client-Secret`；
3. 请求体 = `base64(AES-128-CBC(JSON, key, IV = key) + PKCS7)`；
4. 响应体同样加密，需用同一密钥解密。

> **IV 等于密钥**是协议既定行为，因此同一密钥下相同明文恒定产生相同密文。
> 这不是缺陷，`MiotCryptoTest` 中有一条测试专门固定该行为。

**例外**：换令牌接口与 `miot-spec.org` 返回**明文 JSON**，不带加密头。
客户端通过「响应是否以 `{` 或 `[` 开头」判断，无需调用方关心。

### 3.3 请求头

```
Content-Type: text/plain
User-Agent: mico/docker
X-Client-BizId: micoapi
X-Encrypt-Type: 1
X-Client-AppId: {client_id}
X-Client-Secret: {base64(RSA(会话密钥))}
Host: {业务主机}
Authorization: Bearer{access_token}
```

> `Bearer` 与令牌之间**没有空格**，这是最容易写错的一处，测试中有专门断言。

### 3.4 授权流程

`state` 由本地 uuid 派生：`SHA1("d=mico." + uuid)`。它的作用是确认回调确实由本机发起。

```
生成 uuid（持久化）
   → 打开授权页（携带 device_id = "mico.{uuid}" 与 state）
   → 用户在小米页面登录并同意
   → 页面展示可复制的回执（base64 的 code + state）
   → 用户在 App 内粘贴
   → 校验 state 一致
   → GET /app/v2/mico/oauth/get_token?data={...} 换令牌
```

换令牌的两个形态（**没有 `grant_type` 字段**）：

- 换码：`{client_id, redirect_uri, code, device_id}`
- 续期：`{client_id, redirect_uri, refresh_token}`

响应为明文 `{"code":0,"result":{"access_token","refresh_token","expires_in"}}`。
本地到期时间按 `now + expires_in × 0.7` 折算，并在到期前 60 秒自动续期。

**回执需要用户手动复制粘贴**：小米只注册了固定的回调展示页，
App 无法用 `intent-filter` 拦截，这是协议约束。

### 3.5 设备与家庭

- 设备列表按 did 分批（每批 150）请求，批内按 `next_start_did` 翻页；
- **家庭与房间信息来自家庭接口，不来自设备接口**，两者按 did 关联；
- 设备的 `spec_type` 字段即其规范 URN；缺失时按型号反查（非公开路径，失败不应阻塞同步）；
- 不属于任何房间的设备，其 `room_name` 会退化为家庭名——这是协议行为，
  实现中用 `roomAssigned` 显式标记，不靠字符串比较去猜；
- did 形如 `123.s1` 的是子设备，通常不在房间列表中，由实现继承父设备的房间。

### 3.6 属性读写

```jsonc
// 读：params 是数组，响应也是数组
POST /app/v2/miotspec/prop/get  {"datasource":1,"params":[{"did","siid","piid"}]}

// 写：params 是数组，响应无 value，逐项 code
POST /app/v2/miotspec/prop/set  {"params":[{"did","siid","piid","value"}]}
```

成功码：`0`、`-702000000`、`-702010000`。

### 3.7 常见错误码

| 错误码 | 含义 | 归类 |
|---|---|---|
| `-704042011` | 设备离线 | 可重试 |
| `-704042001` / `-704090001` | 设备未找到 | — |
| `-704040003` / `-704040005` | 属性不存在 | — |
| `-704030023` | 属性不可写 | — |
| `-704220043` | 属性值越界 | — |
| `-704012906` | 认证失败 | 需重新绑定 |
| `-704083036` | 操作超时 | 可重试 |

---

## 4. 设备规范解析

规范来自**公开的** `https://miot-spec.org/miot-spec-v2/instance?type={urn}`，无需小米鉴权。

```jsonc
{
  "type": "urn:miot-spec-v2:device:light:0000A001:yeelink-ceiling1:2",
  "services": [{
    "iid": 2,
    "type": "urn:miot-spec-v2:service:light:00007802:...",
    "properties": [{
      "iid": 2,
      "type": "...:brightness:...",
      "format": "uint8",
      "access": ["read", "write", "notify"],
      "unit": "percentage",
      "value-range": [1, 100, 1]        // ← 三元素数组 [min, max, step]
    }]
  }]
}
```

**需要注意的几点：**

- `value-range` 是**数组** `[min, max, step]`，不是 `{min,max,step}` 对象；
- URN 按 `:` 切分后的第 4 段是类别名（`light` / `air-conditioner` / …），第 5 段是属性名（`on` / `brightness`）；
- `access` 数组决定可读/可写；`unit` 缺失时为 null；
- `device-information` 服务是设备元数据（厂商、序列号等），解析时跳过；
- lite iid 形如 `prop.0.{siid}.{piid}`，转 API 形式为 `prop.{siid}.{piid}`。

**刻意不做类型目录（type level）过滤。** 该过滤需要额外拉取数百个请求，
且类型查不到时会把整个服务丢弃导致能力解析失败——属于设计包袱而非功能。

规范按 URN 永久缓存（存**原始 JSON**，改进解析器时无需重新联网）。
缓存结构带 `cacheVersion`，格式变更时旧缓存自动失效。

---

## 5. 能力映射

规范里的属性名（`on` / `brightness` / `color-temperature`）与本项目的动作词表
（`set_power` / `set_brightness` / …）不同，由 `MiotCapabilityMapper` 对齐。

**同名属性跨服务重复**是这里最容易出错的点。典型例子是插座：

```
service 2  switch           on   ← 真正控制通断电的是这个
service 3  indicator-light  on   ← 这个只管指示灯
```

因此映射按两条规则取舍：

1. 主服务优先于辅助服务（`switch` / `light` / `air-conditioner` … > `*-extension` / `indicator-light` / `remote`）；
2. 优先级相同时**可写的胜出**——窗帘的 `target-position`（可写）与 `current-position`（只读）
   都映射成 `position`，若保留只读的那个，开合指令就只能退化成动作枚举。

**方向与单位一律以规范为准，不猜。** 窗帘开合优先使用幂等的
「设为 0 / 100」，只有「停止」没有幂等表达时才退回动作枚举，
且枚举取值按规范的描述文字匹配——不同型号的方向可能相反。

---

## 6. 安全门控

### 6.1 单一门控点

`HomeExecutor` 是**唯一**决定是否产生真实副作用的地方。`isLive` 为 false 时：

- 绝不调用适配器的写接口；
- 只调用 `preview()` 展示将要下发的属性与取值；
- 终态为 `RunStatus.PLANNED`（不是 `SUCCEEDED`）。

这样设计是为了让门控只有一处，便于审查是否存在旁路。
**设备卡片上的手动开关也走执行器**，不直接改本地状态。

### 6.2 三层前置条件

真实下发需要同时满足：

1. 已绑定小米账号（否则执行层直接拒绝并提示）；
2. 处于 LIVE 模式；
3. 目标设备的 `mappingStatus` 为 `MAPPED` 或 `VERIFIED`，**且确实存在可写能力**。

`DRAFT`（规范未解析成功）的设备一律拒绝下发——能力未知就不该假装能控制。

### 6.3 绑定即重置

建立或解除绑定后，**模式强制回到观察模式**。
换账号环境后不允许沿用上一轮可能已是 LIVE 的设置直接操作真实设备。

### 6.4 令牌存储

`access_token` / `refresh_token` 使用 Android Keystore 支持的加密存储
（`EncryptedSharedPreferences`）。**加密存储不可用时直接报错并拒绝绑定，不降级为明文**。

> 注意：项目既有的 Jev API Key 是明文存于普通 SharedPreferences 的，
> 米家这一层刻意没有沿用那套做法。

客户端直连意味着持有该手机、能访问其进程或设备的人仍可能提取令牌。
这一点无法通过客户端方案消除，不应宣称"不可提取"。

---

## 7. 多家庭语义

真实账号常有多个地理上分离的家庭（例如「家里」与「农场」）。

- **「全屋」指当前选中的家庭**，不是账号下的全部设备；
- 同一时刻只有一个激活家庭，切换后设备列表、房间、分组与 ASR 热词一并更新；
- 这样设计是因为用户说"把灯都关了"时，绝不应同时关掉另一个城市的房子的灯。

---

## 8. 已知限制

| 限制 | 说明 |
|---|---|
| 未实现动作调用 | 规范的 `action` 已解析保留，但执行层只做属性读写 |
| 未实现场景执行 | 手动场景列表与执行未接入 |
| 无实时状态推送 | 状态在同步时与下发回读时更新，不做 MQTT 订阅 |
| 接口非公开契约 | `/app/v2/*` 是 App 私有接口，随时可能变更；`MiotCloudClient` 是唯一的替换点 |
| 协议路径 | 部分路径（如按型号反查 URN）位于非公开位置，失败时降级而非阻塞 |

---

## 9. 相关文档

- [设备控制协议](device-control-contract.md)：本项目的动作词表与设备标识规则
- [技术设计](technical-design.md)：客户端整体架构
- [需求文档](requirements.md)：产品行为与验收标准
