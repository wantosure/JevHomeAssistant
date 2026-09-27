# 开发交接入口（客户端直连 Jev 版）

这是当前有效版本。手机直接调用 Jev，本 Demo **无独立云端业务服务**。此前 Sites 和甲骨文云 `bitcoin001.cn` 方案暂不使用。

按顺序阅读：

1. [需求文档](requirements.md)：产品行为和验收。
2. [技术设计](technical-design.md)：安卓本地架构、Jev 直连、任务/设备/闹钟/电脑。
3. [设备协议](device-control-contract.md)：自定义逻辑 ID、标准动作及参数。
4. [米家极客版设备截图](assets/mijia-geek-devices.png)：真实名称和房间转录依据。
5. [米家云接入说明](miot-integration.md)：Android 应用实际使用的真实设备同步、能力映射和安全执行链路。
6. [语音测试集指南](voice-evaluation-guide.md)与[初始标注样例](../testdata/voice-cases-starter.jsonl)：判断准确率和误触发的评测起点。
7. [测试音频来源与录制规则](audio-source-plan.md)、[18 句录音清单](../testdata/recording-manifest.csv)和[12 条真实人声负例候选](../testdata/public-stcmds-candidates.csv)：音频获取、命名和导入依据。
8. [中英文助手语音测试集说明](../testdata/bilingual-dataset-README.md)：当前主语料入口。完整测试来源已下载，精选中英文各 500 条；本机 [试听页面](../testdata/assistant-bilingual-v1/试听与标注.html) 和 [音频包](../testdata/assistant-bilingual-v1.zip)。
9. [家庭模拟设备、分组与语音指令全集](simulated-home-device-catalog.md)：87 个截图条目的离线模拟评测夹具，包含唯一 ID、房间、能力参数、分组成员和逐设备语音例句；它不代表 Android 应用的真实设备注册表或已验证硬件能力。
10. [静默助手无响应语音文本集](no-response-speech-cases.md)：500 条设备误触发负例与 500 条日常闲聊；含完整可导入的 JSONL 和预期静默判定。
11. [全量测试指令中英表](all-voice-test-cases.md)：把设备动作、610 条逐设备口语变体、分组指令、设备专属负例、500 条误触发负例和 500 条闲聊统一到 ID 空间；提供 [TTS CSV](../testdata/all-voice-test-cases-v1.csv) 和含预期标签的 [设备用例 JSONL](../testdata/smart-home-command-cases-v1.jsonl)。
12. [ASR 误识别测试集](asr-misrecognition-cases.md)：覆盖全部 1,837 条可执行中文设备/分组命令，每条关联一个同音或近音 ASR 错误文本，含 CSV 与结构化 JSONL 标准答案。

旧项目 `jev_server.py`、`numeric_values.py`、`group_control.py`、`worker/index.js` 可作为算法参考，但不要传 `.key`、数据库或其他凭据。

> **⚠️ 这些是上一代方案，已不再被 Android 应用使用。**
> 目前应用的真实设备来自米家云接入，见 [米家接入说明](miot-integration.md)。
>
> 注意：`mijia-100-devices.json`、`device-adapters/mijia_adapter.py` 以及 `scripts/` 下的
> 若干脚本仍被上面这些**原型脚本**引用，因此暂未删除。它们与 Android 应用已无关系，
> 其中的设备 ID 与映射表是当初为演示编造的，**不含任何真实米家标识**，不要据此判断应用行为。

发给开发 AI 的提示词：

> 请按随附需求、技术设计、设备协议和米家极客版截图，开发独立安卓静默语音助手。闲置手机本地持续 VAD/ASR，App 直接调用 Jev 做两阶段判断，不为 Jev 建云端服务。无唤醒词、不主动接话；灰色日志显示识别与分类，执行卡片显示真实进度。设备逻辑 ID 和标准参数由你按协议制定，先做观察模式，再接入真正可验证的米家执行接口。闹钟由手机本地调度；电脑控制首版通过同一局域网的白名单代理。交付可安装 APK、源码、设备配置、模型来源、代理和真机验证记录。缺少实际物理 ID 或接口时明确标未接入，不把模拟计划说成真实成功。

性能和全天指标仍是待验收目标。上述语音语料和模拟设备目录用于离线评测，不能证明真实设备运行表现；真实设备接入见[米家云接入说明](miot-integration.md)。
