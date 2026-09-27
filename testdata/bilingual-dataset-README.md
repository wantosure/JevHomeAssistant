# 中英文语音助手测试集 v1

本地整理日期：2026-09-27。打开 `试听与标注.html` 即可在浏览器搜索、试听并查看原始语义标注；页面完全在本地运行。中文、英文各 500 条，共 1,000 条真人录音。

## 已交付内容

- `audio/`：1,000 条原始音频，中文 WAV、英文 FLAC，保持源文件字节。播放器不自动播放或上传录音。
- `samples.jsonl`：精选集清单、转写、原始意图/语义槽位、音频路径、时长和 SHA-256。
- `full-source-inventory.jsonl`：完整来源测试分区的 19,633 条记录（CATSLU 6,555 + SLURP 13,078）。未被精选的记录 `audio_path=null`，实际音频仍在本机保留的原始压缩包/Parquet 中；不表示精选 ZIP 内包含全部音频。
- `stats.json`：实测数量、领域分布与校验状态。

精选英文：家居控制 220 条、闹钟 96 条、音量 62 条、一般助手交互 60 条、其他助手业务 62 条。中文来源领域各 125 条。精选录音总长约 47.84 分钟，1,000 个音频 SHA-256 均不同。中文原库 6,555 条领域记录对应 6,368 个不同的音频 ID；同一音频可能出现在不同领域，使用 `recording_group` 分组，勿按领域行号随机划分训练和测试。

## 来源与获取方式

### CATSLU：中文真实人机对话

官方项目：https://sites.google.com/view/catslu/home/

任务说明：https://sites.google.com/view/catslu/challenge-details

官方测试包：https://drive.google.com/file/d/1DO2lYYXk7lEMoFQeY2XdK1irZHDhiDEA/view

来自官方 `catslu_test.tar.gz`，完整包已下载。包含导航、音乐、天气和视频四个领域的原始语音、人工转写、ASR 输出和语义标注。部分录音有机器人串音、未知语音或多轮上下文。精选主要选取首轮、转写清晰且有语义标签的不同表达，另收录 6 条原天气语料中出现的闹钟和音量请求；这 6 条原始语义标签为空，`suggested_task_for_review` 仅为整理者的待复核建议，不能当作原始标准答案。

在本次核实的官方页面及测试压缩包中未找到明确的完整再分发许可。此份整理用于用户已说明的本地非商业研究评测，不声称 CATSLU 可自由商用或公开再分发。外发整包之前需确认作者许可。

### SLURP：英文助手指令与参数

作者仓库：https://github.com/pswietojanski/slurp

原始音频发布：https://zenodo.org/records/4274930

本次测试音频镜像：https://huggingface.co/datasets/qmeeus/slurp

为避免下载训练语料，下载镜像的两个 test Parquet 分片。逐条核对了 13,078 条记录的 `slurp_id`、音频文件名、转写和意图，与作者仓库 `dataset/slurp/test.jsonl` 完全对应。完整镜像测试分片已保留在项目 `testdata/corpora/assistant-bilingual/`。精选优先覆盖家居、闹钟、音量，再补充一般助手交互及其他业务。同一句的不同麦克风/说话人版本只取一个，保留 `recording_group`，避免把同一句的多次录音当作独立语义题。

文本 CC BY 4.0；音频 CC BY-NC 4.0，见随包 `SLURP-LICENSE.txt`。引用：Emanuele Bastianelli, Andrea Vanzo, Pawel Swietojanski and Verena Rieser. “SLURP: A Spoken Language Understanding Resource Package.” EMNLP 2020.

## 如何用于当前 Jev 静默助手

1. 先用 `reference_text` 测原始业务/意图，再用实际 VAD/ASR 识别音频后做同样判断；二者差异用于区分语义判断与语音识别问题。
2. `source_labels` 是原语料标准；`product_expected=null` 表示尚未把这些标签映射到你家设备、电脑和闹钟规则。原库“用户向助手发请求”不等于本产品“允许执行”。例如天气请求在支持天气的助手里有效，在当前静默助手里可能属于未支持业务。
3. 英文 `audio_control` 不自动等于控制你的电脑；必须依据你定义的默认目标和澄清规则处理。闹钟需补固定日期/时区，设备控制需绑定测试设备目录。测试时保持观察模式。
4. CATSLU 中的音乐/视频指令不能直接标成闲聊或无效请求；它们首先是明确的助手请求，只是可能不在本产品能力范围。
5. `source_split=test` 保留原公开测试分区。精选已经可供查看，适合开发回归，不能再声称是未见过的封存验收集。新验收集须独立录制/留出，并按人、语句组或对话分组避免泄漏。
6. 原始转写/意图核对完成不等于逐条回听确认。当前 `human_listening_reviewed=0`，应在正式评分前回听所用样本，尤其留意远场、串音、停顿和转写误差。

## 仍需补齐的覆盖面

- 中文真实家居控制正例、中文和英文针对当前电脑代理的指令。
- 同一句话对助手说、对家人说、电视播放的来源对照及持续环境音。只有最终 ASR 文本时，来源相同文字无法保证可辨别；单列此类歧义样本。
- 否定、复述、撤回、条件句、ASR 尚未说完及“开/关”“十五/五十”等关键误识别。
- 中英混说是第三种测试条件，不包含在“两种语言各 500 条”的数量声明中。

## 其他值得接入的助手数据

- Snips / Sonos SmartLights：6 类灯光意图，含房间、亮度、颜色和 2 米远场录音。官方要求提交申请表，目前未代用户提交或获得音频。https://github.com/sonos/spoken-language-understanding-research-datasets
- Fluent Speech Commands：英文家居命令，30,043 条、31 个动作/对象/位置组合；本次未下载。官方另有仅限学术研究条款。https://fluent.ai/fluent-speech-commands-a-dataset-for-spoken-language-understanding-research/
- Alexa MASSIVE：包含中英文的意图与槽位**文本**，适合补中文家居命令并自行录音或单列 TTS 合成集；它本身不是中英文音频包。https://github.com/alexa/massive
- 美的团队 Reject or Not?：论文报告 11,913 条家庭场景文本/语音样本，语音由 TTS 合成。任务贴近是否应该介入，但本次未核实可下载数据包，不计入已交付数量。https://arxiv.org/abs/2512.10257
- 小米：本次未找到可核实下载的“小爱内部家居指令测试集”。小米公开音频理解研究集不可自动等同于小爱产品测试集。
