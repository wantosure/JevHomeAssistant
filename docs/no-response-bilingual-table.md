# 静默助手无响应测试集双语 TTS 表

共 500 条误触发负例和 500 条日常闲聊。`id` 是中英文共用的语义样本 ID，每行对应一组中英文文本；两种语言的预期判定均为忽略/不响应。英文句子按相同类别与语义生成，适合分别合成 TTS。

可直接批量导入的 CSV：[no-response-bilingual-v1.csv](../testdata/no-response-bilingual-v1.csv)，UTF-8 with BOM 编码，表头为 `id,中文,English`。建议音频文件名分别使用 `{id}_zh-CN.wav` 与 `{id}_en-US.wav`，便于把语言和音频回连到同一用例。

| id | 中文 | English |
| --- | --- | --- |
| `NRF-unknown_location-01-01` | 打开二楼的灯 | Turn on the lights on the second floor. |
| `NRF-unknown_location-01-02` | 打开三楼的灯 | Turn on the lights on the third floor. |
| `NRF-unknown_location-01-03` | 打开地下室的灯 | Turn on the lights in the basement. |
| `NRF-unknown_location-01-04` | 打开阁楼的灯 | Turn on the lights in the attic. |
| `NRF-unknown_location-01-05` | 打开书房的灯 | Turn on the lights in the study. |
| `NRF-unknown_location-01-06` | 打开儿童房的灯 | Turn on the lights in the kids’ room. |
| `NRF-unknown_location-01-07` | 打开客房的灯 | Turn on the lights in the guest room. |
| `NRF-unknown_location-01-08` | 打开储藏室的灯 | Turn on the lights in the storage room. |
| `NRF-unknown_location-01-09` | 打开走廊尽头的灯 | Turn on the lights at the far end of the hallway. |
| `NRF-unknown_location-01-10` | 打开车库的灯 | Turn on the lights in the garage. |
| `NRF-unknown_location-02-01` | 把二楼空调关了 | Switch off the air conditioner on the second floor. |
| `NRF-unknown_location-02-02` | 把楼上空调关了 | Switch off the air conditioner on the third floor. |
| `NRF-unknown_location-02-03` | 把楼下空调关了 | Switch off the air conditioner in the basement. |
| `NRF-unknown_location-02-04` | 把书房空调关了 | Switch off the air conditioner in the attic. |
| `NRF-unknown_location-02-05` | 把儿童房空调关了 | Switch off the air conditioner in the study. |
| `NRF-unknown_location-02-06` | 把客房空调关了 | Switch off the air conditioner in the kids’ room. |
| `NRF-unknown_location-02-07` | 把地下室空调关了 | Switch off the air conditioner in the guest room. |
| `NRF-unknown_location-02-08` | 把阁楼空调关了 | Switch off the air conditioner in the storage room. |
| `NRF-unknown_location-02-09` | 把车库空调关了 | Switch off the air conditioner at the far end of the hallway. |
| `NRF-unknown_location-02-10` | 把办公室空调关了 | Switch off the air conditioner in the garage. |
| `NRF-unknown_location-03-01` | 二楼窗帘拉开一半 | Open the curtains halfway on the second floor. |
| `NRF-unknown_location-03-02` | 三楼窗帘拉开一半 | Open the curtains halfway on the third floor. |
| `NRF-unknown_location-03-03` | 书房窗帘拉开一半 | Open the curtains halfway in the basement. |
| `NRF-unknown_location-03-04` | 儿童房窗帘拉开一半 | Open the curtains halfway in the attic. |
| `NRF-unknown_location-03-05` | 客房窗帘拉开一半 | Open the curtains halfway in the study. |
| `NRF-unknown_location-03-06` | 阳台外面窗帘拉开一半 | Open the curtains halfway in the kids’ room. |
| `NRF-unknown_location-03-07` | 储藏间窗帘拉开一半 | Open the curtains halfway in the guest room. |
| `NRF-unknown_location-03-08` | 楼梯口窗帘拉开一半 | Open the curtains halfway in the storage room. |
| `NRF-unknown_location-03-09` | 地下室窗帘拉开一半 | Open the curtains halfway at the far end of the hallway. |
| `NRF-unknown_location-03-10` | 车库窗帘拉开一半 | Open the curtains halfway in the garage. |
| `NRF-unknown_location-04-01` | 开一下二楼的风扇 | Turn on the fan on the second floor. |
| `NRF-unknown_location-04-02` | 开一下三楼的风扇 | Turn on the fan on the third floor. |
| `NRF-unknown_location-04-03` | 开一下客房的风扇 | Turn on the fan in the basement. |
| `NRF-unknown_location-04-04` | 开一下书房的风扇 | Turn on the fan in the attic. |
| `NRF-unknown_location-04-05` | 开一下儿童房的风扇 | Turn on the fan in the study. |
| `NRF-unknown_location-04-06` | 开一下楼道的风扇 | Turn on the fan in the kids’ room. |
| `NRF-unknown_location-04-07` | 开一下车库的风扇 | Turn on the fan in the guest room. |
| `NRF-unknown_location-04-08` | 开一下地下室的风扇 | Turn on the fan in the storage room. |
| `NRF-unknown_location-04-09` | 开一下阁楼的风扇 | Turn on the fan at the far end of the hallway. |
| `NRF-unknown_location-04-10` | 开一下办公室的风扇 | Turn on the fan in the garage. |
| `NRF-unknown_location-05-01` | 把二楼的灯都关上 | Turn off all the lights on the second floor. |
| `NRF-unknown_location-05-02` | 把三楼的灯都关上 | Turn off all the lights on the third floor. |
| `NRF-unknown_location-05-03` | 把书房的灯都关上 | Turn off all the lights in the basement. |
| `NRF-unknown_location-05-04` | 把儿童房的灯都关上 | Turn off all the lights in the attic. |
| `NRF-unknown_location-05-05` | 把客房的灯都关上 | Turn off all the lights in the study. |
| `NRF-unknown_location-05-06` | 把楼梯间的灯都关上 | Turn off all the lights in the kids’ room. |
| `NRF-unknown_location-05-07` | 把地下室的灯都关上 | Turn off all the lights in the guest room. |
| `NRF-unknown_location-05-08` | 把阁楼的灯都关上 | Turn off all the lights in the storage room. |
| `NRF-unknown_location-05-09` | 把车库的灯都关上 | Turn off all the lights at the far end of the hallway. |
| `NRF-unknown_location-05-10` | 把办公室的灯都关上 | Turn off all the lights in the garage. |
| `NRF-unsupported_media-01-01` | 电视播放CCTV1 | Play CCTV-1 on the TV. |
| `NRF-unsupported_media-01-02` | 电视播放新闻联播 | Play the evening news on the TV. |
| `NRF-unsupported_media-01-03` | 电视播放天气预报 | Play the weather forecast on the TV. |
| `NRF-unsupported_media-01-04` | 电视播放动物世界 | Play a nature program on the TV. |
| `NRF-unsupported_media-01-05` | 电视播放纪录片 | Play a documentary on the TV. |
| `NRF-unsupported_media-01-06` | 电视播放体育频道 | Play a sports channel on the TV. |
| `NRF-unsupported_media-01-07` | 电视播放电影频道 | Play a movie channel on the TV. |
| `NRF-unsupported_media-01-08` | 电视播放本地新闻 | Play local news on the TV. |
| `NRF-unsupported_media-01-09` | 电视播放动画片 | Play a cartoon on the TV. |
| `NRF-unsupported_media-01-10` | 电视播放综艺节目 | Play a variety show on the TV. |
| `NRF-unsupported_media-02-01` | 给我放CCTV1 | Put on CCTV-1 for me. |
| `NRF-unsupported_media-02-02` | 给我放新闻联播 | Put on the evening news for me. |
| `NRF-unsupported_media-02-03` | 给我放天气预报 | Put on the weather forecast for me. |
| `NRF-unsupported_media-02-04` | 给我放一部老电影 | Put on a nature program for me. |
| `NRF-unsupported_media-02-05` | 给我放昨晚那场比赛 | Put on a documentary for me. |
| `NRF-unsupported_media-02-06` | 给我放动画片 | Put on a sports channel for me. |
| `NRF-unsupported_media-02-07` | 给我放本地新闻 | Put on a movie channel for me. |
| `NRF-unsupported_media-02-08` | 给我放纪录片 | Put on local news for me. |
| `NRF-unsupported_media-02-09` | 给我放电视剧 | Put on a cartoon for me. |
| `NRF-unsupported_media-02-10` | 给我放综艺节目 | Put on a variety show for me. |
| `NRF-unsupported_media-03-01` | 把电视调到CCTV1 | Switch the TV to CCTV-1. |
| `NRF-unsupported_media-03-02` | 把电视调到CCTV5 | Switch the TV to the evening news. |
| `NRF-unsupported_media-03-03` | 把电视调到电影频道 | Switch the TV to the weather forecast. |
| `NRF-unsupported_media-03-04` | 把电视调到新闻频道 | Switch the TV to a nature program. |
| `NRF-unsupported_media-03-05` | 把电视调到地方台 | Switch the TV to a documentary. |
| `NRF-unsupported_media-03-06` | 把电视调到体育频道 | Switch the TV to a sports channel. |
| `NRF-unsupported_media-03-07` | 把电视调到财经频道 | Switch the TV to a movie channel. |
| `NRF-unsupported_media-03-08` | 把电视调到少儿频道 | Switch the TV to local news. |
| `NRF-unsupported_media-03-09` | 把电视调到纪录片频道 | Switch the TV to a cartoon. |
| `NRF-unsupported_media-03-10` | 把电视调到音乐频道 | Switch the TV to a variety show. |
| `NRF-unsupported_media-04-01` | 电视上搜一下一部喜剧片 | Search for CCTV-1 on the TV. |
| `NRF-unsupported_media-04-02` | 电视上搜一下昨晚的新闻 | Search for the evening news on the TV. |
| `NRF-unsupported_media-04-03` | 电视上搜一下最新电影 | Search for the weather forecast on the TV. |
| `NRF-unsupported_media-04-04` | 电视上搜一下那部老电视剧 | Search for a nature program on the TV. |
| `NRF-unsupported_media-04-05` | 电视上搜一下天气预报 | Search for a documentary on the TV. |
| `NRF-unsupported_media-04-06` | 电视上搜一下足球比赛 | Search for a sports channel on the TV. |
| `NRF-unsupported_media-04-07` | 电视上搜一下纪录片 | Search for a movie channel on the TV. |
| `NRF-unsupported_media-04-08` | 电视上搜一下动画电影 | Search for local news on the TV. |
| `NRF-unsupported_media-04-09` | 电视上搜一下美食节目 | Search for a cartoon on the TV. |
| `NRF-unsupported_media-04-10` | 电视上搜一下音乐会 | Search for a variety show on the TV. |
| `NRF-unsupported_media-05-01` | 让客厅电视继续播新闻联播 | Let the living-room TV keep playing CCTV-1. |
| `NRF-unsupported_media-05-02` | 让客厅电视继续播CCTV1 | Let the living-room TV keep playing the evening news. |
| `NRF-unsupported_media-05-03` | 让客厅电视继续播天气预报 | Let the living-room TV keep playing the weather forecast. |
| `NRF-unsupported_media-05-04` | 让客厅电视继续播昨晚的电影 | Let the living-room TV keep playing a nature program. |
| `NRF-unsupported_media-05-05` | 让客厅电视继续播体育比赛 | Let the living-room TV keep playing a documentary. |
| `NRF-unsupported_media-05-06` | 让客厅电视继续播纪录片 | Let the living-room TV keep playing a sports channel. |
| `NRF-unsupported_media-05-07` | 让客厅电视继续播电视剧 | Let the living-room TV keep playing a movie channel. |
| `NRF-unsupported_media-05-08` | 让客厅电视继续播综艺节目 | Let the living-room TV keep playing local news. |
| `NRF-unsupported_media-05-09` | 让客厅电视继续播动画片 | Let the living-room TV keep playing a cartoon. |
| `NRF-unsupported_media-05-10` | 让客厅电视继续播音乐节目 | Let the living-room TV keep playing a variety show. |
| `NRF-device_mentioned_only-01-01` | 今天我买了个灯，准备周末自己装 | I was just chatting about home stuff: I bought a lamp today and plan to install it this weekend. |
| `NRF-device_mentioned_only-01-02` | 朋友家最近也换了空调 | I was just chatting about home stuff: A friend recently got a new air conditioner. |
| `NRF-device_mentioned_only-01-03` | 昨天看到有人在装窗帘 | I was just chatting about home stuff: I saw someone putting up curtains yesterday. |
| `NRF-device_mentioned_only-01-04` | 我爸说老家那个风扇有点响 | I was just chatting about home stuff: Dad says the old fan at home has started making noise. |
| `NRF-device_mentioned_only-01-05` | 同事新买了一个空气净化器 | I was just chatting about home stuff: A coworker just bought an air purifier. |
| `NRF-device_mentioned_only-01-06` | 楼下邻居家的门铃声音挺大 | I was just chatting about home stuff: The doorbell at the downstairs neighbor’s place sounds loud. |
| `NRF-device_mentioned_only-01-07` | 网上有人推荐了一款扫地机器人 | I was just chatting about home stuff: Someone online recommended a robot vacuum. |
| `NRF-device_mentioned_only-01-08` | 刚才路过一家店在卖智能音箱 | I was just chatting about home stuff: I passed a shop selling smart speakers. |
| `NRF-device_mentioned_only-01-09` | 我以前用过一个特别旧的加湿器 | I was just chatting about home stuff: I used to have a very old humidifier. |
| `NRF-device_mentioned_only-01-10` | 小时候家里那盏灯总是一闪一闪的 | I was just chatting about home stuff: The lamp at home used to flicker when I was a kid. |
| `NRF-device_mentioned_only-02-01` | 新买的灯还放在纸箱里 | The conversation drifted to appliances: I bought a lamp today and plan to install it this weekend. |
| `NRF-device_mentioned_only-02-02` | 空调滤网该找时间清理了 | The conversation drifted to appliances: A friend recently got a new air conditioner. |
| `NRF-device_mentioned_only-02-03` | 窗帘颜色跟墙纸不太搭 | The conversation drifted to appliances: I saw someone putting up curtains yesterday. |
| `NRF-device_mentioned_only-02-04` | 风扇声音好像比去年大了一点 | The conversation drifted to appliances: Dad says the old fan at home has started making noise. |
| `NRF-device_mentioned_only-02-05` | 净化器的滤芯上个月才换 | The conversation drifted to appliances: A coworker just bought an air purifier. |
| `NRF-device_mentioned_only-02-06` | 门锁电池我得买一节备用 | The conversation drifted to appliances: The doorbell at the downstairs neighbor’s place sounds loud. |
| `NRF-device_mentioned_only-02-07` | 扫地机器人最近总卡在地毯上 | The conversation drifted to appliances: Someone online recommended a robot vacuum. |
| `NRF-device_mentioned_only-02-08` | 那个智能音箱已经很久没听歌了 | The conversation drifted to appliances: I passed a shop selling smart speakers. |
| `NRF-device_mentioned_only-02-09` | 加湿器冬天用得比较多 | The conversation drifted to appliances: I used to have a very old humidifier. |
| `NRF-device_mentioned_only-02-10` | 客厅那盏灯用了很多年了 | The conversation drifted to appliances: The lamp at home used to flicker when I was a kid. |
| `NRF-device_mentioned_only-03-01` | 我今天逛商场看见一排吊灯 | I suddenly remembered something: I bought a lamp today and plan to install it this weekend. |
| `NRF-device_mentioned_only-03-02` | 朋友发来一张新家装修的照片 | I suddenly remembered something: A friend recently got a new air conditioner. |
| `NRF-device_mentioned_only-03-03` | 网上那篇文章介绍了不同的空调 | I suddenly remembered something: I saw someone putting up curtains yesterday. |
| `NRF-device_mentioned_only-03-04` | 刚才电视里在讲窗帘怎么选 | I suddenly remembered something: Dad says the old fan at home has started making noise. |
| `NRF-device_mentioned_only-03-05` | 我同事正在比较几款风扇 | I suddenly remembered something: A coworker just bought an air purifier. |
| `NRF-device_mentioned_only-03-06` | 楼下有人讨论空气净化器的滤芯 | I suddenly remembered something: The doorbell at the downstairs neighbor’s place sounds loud. |
| `NRF-device_mentioned_only-03-07` | 家里人聊到门锁要不要换新 | I suddenly remembered something: Someone online recommended a robot vacuum. |
| `NRF-device_mentioned_only-03-08` | 最近扫地机器人出了几个新款 | I suddenly remembered something: I passed a shop selling smart speakers. |
| `NRF-device_mentioned_only-03-09` | 新闻里提到智能音箱市场 | I suddenly remembered something: I used to have a very old humidifier. |
| `NRF-device_mentioned_only-03-10` | 邻居说冬天加湿器挺实用 | I suddenly remembered something: The lamp at home used to flicker when I was a kid. |
| `NRF-device_mentioned_only-04-01` | 那盏灯是上个月才买的 | It came up while we were talking: I bought a lamp today and plan to install it this weekend. |
| `NRF-device_mentioned_only-04-02` | 空调去年夏天修过一次 | It came up while we were talking: A friend recently got a new air conditioner. |
| `NRF-device_mentioned_only-04-03` | 窗帘当时选了浅灰色 | It came up while we were talking: I saw someone putting up curtains yesterday. |
| `NRF-device_mentioned_only-04-04` | 风扇是搬家前买的 | It came up while we were talking: Dad says the old fan at home has started making noise. |
| `NRF-device_mentioned_only-04-05` | 净化器已经用了两三年 | It came up while we were talking: A coworker just bought an air purifier. |
| `NRF-device_mentioned_only-04-06` | 门锁是装修的时候装的 | It came up while we were talking: The doorbell at the downstairs neighbor’s place sounds loud. |
| `NRF-device_mentioned_only-04-07` | 扫地机器人是朋友送的 | It came up while we were talking: Someone online recommended a robot vacuum. |
| `NRF-device_mentioned_only-04-08` | 音箱放在柜子上好久了 | It came up while we were talking: I passed a shop selling smart speakers. |
| `NRF-device_mentioned_only-04-09` | 加湿器收在储物柜里 | It came up while we were talking: I used to have a very old humidifier. |
| `NRF-device_mentioned_only-04-10` | 阳台灯泡前阵子刚换过 | It came up while we were talking: The lamp at home used to flicker when I was a kid. |
| `NRF-device_mentioned_only-05-01` | 我突然想起小时候那盏台灯 | I was telling someone about this: I bought a lamp today and plan to install it this weekend. |
| `NRF-device_mentioned_only-05-02` | 说到空调我想起上周的维修单 | I was telling someone about this: A friend recently got a new air conditioner. |
| `NRF-device_mentioned_only-05-03` | 窗帘这个颜色看久了还挺舒服 | I was telling someone about this: I saw someone putting up curtains yesterday. |
| `NRF-device_mentioned_only-05-04` | 风扇转起来的声音有点催眠 | I was telling someone about this: Dad says the old fan at home has started making noise. |
| `NRF-device_mentioned_only-05-05` | 净化器运行时有一点轻微的风声 | I was telling someone about this: A coworker just bought an air purifier. |
| `NRF-device_mentioned_only-05-06` | 门铃响的时候我正好在厨房 | I was telling someone about this: The doorbell at the downstairs neighbor’s place sounds loud. |
| `NRF-device_mentioned_only-05-07` | 扫地机器人绕着椅子转了好几圈 | I was telling someone about this: Someone online recommended a robot vacuum. |
| `NRF-device_mentioned_only-05-08` | 音箱里那首歌我以前常听 | I was telling someone about this: I passed a shop selling smart speakers. |
| `NRF-device_mentioned_only-05-09` | 加湿器的水箱还没晾干 | I was telling someone about this: I used to have a very old humidifier. |
| `NRF-device_mentioned_only-05-10` | 客厅灯罩上好像落了一层灰 | I was telling someone about this: The lamp at home used to flicker when I was a kid. |
| `NRF-quoted_or_reported-01-01` | 他说“打开主卧的灯” | He said, “Turn on the bedroom light.” |
| `NRF-quoted_or_reported-01-02` | 她刚才提到“把客厅空调调到26度” | He said, “Set the living-room AC to 26 degrees.” |
| `NRF-quoted_or_reported-01-03` | 电视里有人说“请关闭所有灯光” | He said, “Please turn off all the lights.” |
| `NRF-quoted_or_reported-01-04` | 歌词里唱着“把灯打开” | He said, “Turn on the light.” |
| `NRF-quoted_or_reported-01-05` | 我看到评论写着“开一下空调” | He said, “Switch on the air conditioner.” |
| `NRF-quoted_or_reported-01-06` | 朋友发消息说“窗帘拉开吧” | He said, “Open the curtains.” |
| `NRF-quoted_or_reported-01-07` | 新闻字幕写着“智能家居控制” | He said, “Control the smart lights.” |
| `NRF-quoted_or_reported-01-08` | 那段视频里反复说“打开风扇” | He said, “Turn on the fan.” |
| `NRF-quoted_or_reported-01-09` | 小孩学着动画片说“开灯” | He said, “Turn on the lights.” |
| `NRF-quoted_or_reported-01-10` | 播客里提到了“关闭空调” | He said, “Turn off the AC.” |
| `NRF-quoted_or_reported-02-01` | 我只是在复述他说的开灯那句话 | She mentioned, “Turn on the bedroom light.” |
| `NRF-quoted_or_reported-02-02` | 她刚才问我能不能把空调调低一点 | She mentioned, “Set the living-room AC to 26 degrees.” |
| `NRF-quoted_or_reported-02-03` | 主持人念了一遍电视播放CCTV1 | She mentioned, “Please turn off all the lights.” |
| `NRF-quoted_or_reported-02-04` | 歌词里面正好有一句打开窗户 | She mentioned, “Turn on the light.” |
| `NRF-quoted_or_reported-02-05` | 群聊里有人提到把窗帘关上 | She mentioned, “Switch on the air conditioner.” |
| `NRF-quoted_or_reported-02-06` | 那篇文章举例说可以打开风扇 | She mentioned, “Open the curtains.” |
| `NRF-quoted_or_reported-02-07` | 视频旁白说灯光亮度调到一半 | She mentioned, “Control the smart lights.” |
| `NRF-quoted_or_reported-02-08` | 我是在讲昨天谁说要开加湿器 | She mentioned, “Turn on the fan.” |
| `NRF-quoted_or_reported-02-09` | 朋友刚刚讲到智能门锁的事 | She mentioned, “Turn on the lights.” |
| `NRF-quoted_or_reported-02-10` | 我转述一下邻居让人关灯的话 | She mentioned, “Turn off the AC.” |
| `NRF-quoted_or_reported-03-01` | “打开客厅吸顶灯”这句话有几个字 | The video included the line, “Turn on the bedroom light.” |
| `NRF-quoted_or_reported-03-02` | “次卧空调设为26度”一共有多少字 | The video included the line, “Set the living-room AC to 26 degrees.” |
| `NRF-quoted_or_reported-03-03` | 我说的只是“关闭所有空调”这个例子 | The video included the line, “Please turn off all the lights.” |
| `NRF-quoted_or_reported-03-04` | 刚才那句“把灯调亮”不是让我操作 | The video included the line, “Turn on the light.” |
| `NRF-quoted_or_reported-03-05` | 作文题目里写着“开灯以后” | The video included the line, “Switch on the air conditioner.” |
| `NRF-quoted_or_reported-03-06` | 聊天记录中出现过“窗帘打开” | The video included the line, “Open the curtains.” |
| `NRF-quoted_or_reported-03-07` | 歌词里有“月光照亮房间”这句 | The video included the line, “Control the smart lights.” |
| `NRF-quoted_or_reported-03-08` | 新闻标题提到了“家庭智能设备” | The video included the line, “Turn on the fan.” |
| `NRF-quoted_or_reported-03-09` | 说明书上写着按键可以控制灯光 | The video included the line, “Turn on the lights.” |
| `NRF-quoted_or_reported-03-10` | 电影台词里有人喊了一声“开门” | The video included the line, “Turn off the AC.” |
| `NRF-quoted_or_reported-04-01` | 我记得他说的是“把主卧灯关掉” | I am only quoting the phrase, “Turn on the bedroom light.” |
| `NRF-quoted_or_reported-04-02` | 她好像说过客厅空调有点冷 | I am only quoting the phrase, “Set the living-room AC to 26 degrees.” |
| `NRF-quoted_or_reported-04-03` | 他们刚才讨论要不要换一盏灯 | I am only quoting the phrase, “Please turn off all the lights.” |
| `NRF-quoted_or_reported-04-04` | 主持人刚刚报了CCTV1的节目单 | I am only quoting the phrase, “Turn on the light.” |
| `NRF-quoted_or_reported-04-05` | 我听见邻居说窗帘颜色不错 | I am only quoting the phrase, “Switch on the air conditioner.” |
| `NRF-quoted_or_reported-04-06` | 朋友说他家扫地机器人迷路了 | I am only quoting the phrase, “Open the curtains.” |
| `NRF-quoted_or_reported-04-07` | 老师举例讲到语音控制电器 | I am only quoting the phrase, “Control the smart lights.” |
| `NRF-quoted_or_reported-04-08` | 视频标题写的是智能灯怎么安装 | I am only quoting the phrase, “Turn on the fan.” |
| `NRF-quoted_or_reported-04-09` | 我爸提过楼上的风扇坏了 | I am only quoting the phrase, “Turn on the lights.” |
| `NRF-quoted_or_reported-04-10` | 播客主播正在聊家里的温度 | I am only quoting the phrase, “Turn off the AC.” |
| `NRF-quoted_or_reported-05-01` | 这句“开灯”是我从文章里抄下来的 | Someone else sent me a message saying, “Turn on the bedroom light.” |
| `NRF-quoted_or_reported-05-02` | 我唱的歌词里刚好有空调两个字 | Someone else sent me a message saying, “Set the living-room AC to 26 degrees.” |
| `NRF-quoted_or_reported-05-03` | 刚才那条语音是别人发来的 | Someone else sent me a message saying, “Please turn off all the lights.” |
| `NRF-quoted_or_reported-05-04` | 这只是一个关于智能家居的例句 | Someone else sent me a message saying, “Turn on the light.” |
| `NRF-quoted_or_reported-05-05` | 我在读小说里的那句对白 | Someone else sent me a message saying, “Switch on the air conditioner.” |
| `NRF-quoted_or_reported-05-06` | 电视字幕出现了“关闭电源”几个字 | Someone else sent me a message saying, “Open the curtains.” |
| `NRF-quoted_or_reported-05-07` | 朋友给我看了一段开箱视频 | Someone else sent me a message saying, “Control the smart lights.” |
| `NRF-quoted_or_reported-05-08` | 说明书举了一个调节温度的例子 | Someone else sent me a message saying, “Turn on the fan.” |
| `NRF-quoted_or_reported-05-09` | 这是昨天聊天内容的截图文字 | Someone else sent me a message saying, “Turn on the lights.” |
| `NRF-quoted_or_reported-05-10` | 我在练习怎么念这几个设备名称 | Someone else sent me a message saying, “Turn off the AC.” |
| `NRF-negated_or_cancelled-01-01` | 不要开客厅的灯 | Do not turn on the living-room light. |
| `NRF-negated_or_cancelled-01-02` | 先别打开主卧吸顶灯 | Do not switch on the bedroom ceiling light. |
| `NRF-negated_or_cancelled-01-03` | 不用把空调调到26度 | Do not set the AC to 26 degrees. |
| `NRF-negated_or_cancelled-01-04` | 别把窗帘拉开 | Do not open the curtains. |
| `NRF-negated_or_cancelled-01-05` | 风扇不用开了 | Do not turn on the fan. |
| `NRF-negated_or_cancelled-01-06` | 不用关闭空气净化器 | Do not switch off the air purifier. |
| `NRF-negated_or_cancelled-01-07` | 不要让加湿器运行 | Do not run the humidifier. |
| `NRF-negated_or_cancelled-01-08` | 扫地机器人先不要启动 | Do not start the robot vacuum. |
| `NRF-negated_or_cancelled-01-09` | 先别打开门厅中控屏 | Do not turn on the control-panel screen. |
| `NRF-negated_or_cancelled-01-10` | 今天不用播放任何音乐 | Do not play any music today. |
| `NRF-negated_or_cancelled-02-01` | 我不是让你开灯 | Please don’t turn on the living-room light yet. |
| `NRF-negated_or_cancelled-02-02` | 我并没有叫你关空调 | Please don’t switch on the bedroom ceiling light yet. |
| `NRF-negated_or_cancelled-02-03` | 我没说要把窗帘打开 | Please don’t set the AC to 26 degrees yet. |
| `NRF-negated_or_cancelled-02-04` | 不是要启动扫地机器人 | Please don’t open the curtains yet. |
| `NRF-negated_or_cancelled-02-05` | 别执行刚才那条开灯的话 | Please don’t turn on the fan yet. |
| `NRF-negated_or_cancelled-02-06` | 刚才只是提到风扇，不用开 | Please don’t switch off the air purifier yet. |
| `NRF-negated_or_cancelled-02-07` | 我不是想调空调温度 | Please don’t run the humidifier yet. |
| `NRF-negated_or_cancelled-02-08` | 不用按我刚刚说的去做 | Please don’t start the robot vacuum yet. |
| `NRF-negated_or_cancelled-02-09` | 不是让你打开净化器 | Please don’t turn on the control-panel screen yet. |
| `NRF-negated_or_cancelled-02-10` | 今天先不操作任何设备 | Please don’t play any music today yet. |
| `NRF-negated_or_cancelled-03-01` | 要是我说开灯你也先别开 | There is no need to turn on the living-room light. |
| `NRF-negated_or_cancelled-03-02` | 即使我提到空调也不用调整 | There is no need to switch on the bedroom ceiling light. |
| `NRF-negated_or_cancelled-03-03` | 听到窗帘两个字不用操作 | There is no need to set the AC to 26 degrees. |
| `NRF-negated_or_cancelled-03-04` | 我说关灯只是举个例子 | There is no need to open the curtains. |
| `NRF-negated_or_cancelled-03-05` | 就算电视里喊开门也别执行 | There is no need to turn on the fan. |
| `NRF-negated_or_cancelled-03-06` | 如果我聊到风扇不要自动启动 | There is no need to switch off the air purifier. |
| `NRF-negated_or_cancelled-03-07` | 别人叫你开灯时不要理会 | There is no need to run the humidifier. |
| `NRF-negated_or_cancelled-03-08` | 我读一句开空调你不要执行 | There is no need to start the robot vacuum. |
| `NRF-negated_or_cancelled-03-09` | 有人说播放音乐也不用响应 | There is no need to turn on the control-panel screen. |
| `NRF-negated_or_cancelled-03-10` | 后面听到设备名称都先忽略 | There is no need to play any music today. |
| `NRF-negated_or_cancelled-04-01` | 开灯……算了，别开了 | I am not asking you to turn on the living-room light. |
| `NRF-negated_or_cancelled-04-02` | 把空调调低……不，先别动 | I am not asking you to switch on the bedroom ceiling light. |
| `NRF-negated_or_cancelled-04-03` | 窗帘打开，不对，还是保持原样 | I am not asking you to set the AC to 26 degrees. |
| `NRF-negated_or_cancelled-04-04` | 刚才说关灯来着，算了不用关 | I am not asking you to open the curtains. |
| `NRF-negated_or_cancelled-04-05` | 扫地机器人启动……不用了 | I am not asking you to turn on the fan. |
| `NRF-negated_or_cancelled-04-06` | 风扇打开……等一下，别开 | I am not asking you to switch off the air purifier. |
| `NRF-negated_or_cancelled-04-07` | 把温度改成26……不改了 | I am not asking you to run the humidifier. |
| `NRF-negated_or_cancelled-04-08` | 打开净化器，不用了，取消 | I am not asking you to start the robot vacuum. |
| `NRF-negated_or_cancelled-04-09` | 加湿器开始运行……先取消 | I am not asking you to turn on the control-panel screen. |
| `NRF-negated_or_cancelled-04-10` | 音箱播放音乐……算了停一下 | I am not asking you to play any music today. |
| `NRF-negated_or_cancelled-05-01` | 别把客厅灯打开，我现在想睡觉 | I changed my mind. Don’t turn on the living-room light. |
| `NRF-negated_or_cancelled-05-02` | 暂时不要开主卧的空调 | I changed my mind. Don’t switch on the bedroom ceiling light. |
| `NRF-negated_or_cancelled-05-03` | 窗帘就保持现在这样吧 | I changed my mind. Don’t set the AC to 26 degrees. |
| `NRF-negated_or_cancelled-05-04` | 扫地机器人今天不用工作 | I changed my mind. Don’t open the curtains. |
| `NRF-negated_or_cancelled-05-05` | 风扇先不要启动 | I changed my mind. Don’t turn on the fan. |
| `NRF-negated_or_cancelled-05-06` | 空调保持原来的温度 | I changed my mind. Don’t switch off the air purifier. |
| `NRF-negated_or_cancelled-05-07` | 灯不用调亮，我不觉得暗 | I changed my mind. Don’t run the humidifier. |
| `NRF-negated_or_cancelled-05-08` | 不要开启加湿功能 | I changed my mind. Don’t start the robot vacuum. |
| `NRF-negated_or_cancelled-05-09` | 不要播放电视节目 | I changed my mind. Don’t turn on the control-panel screen. |
| `NRF-negated_or_cancelled-05-10` | 门锁保持锁上的状态 | I changed my mind. Don’t play any music today. |
| `NRF-conditional_or_hypothetical-01-01` | 如果回家晚了就把灯打开 | What if we were to turn on the light if I get home late? |
| `NRF-conditional_or_hypothetical-01-02` | 等天气热了再开空调 | What if we were to switch on the AC when it gets hot? |
| `NRF-conditional_or_hypothetical-01-03` | 要是下雨的话把窗帘关上 | What if we were to close the curtains if it rains? |
| `NRF-conditional_or_hypothetical-01-04` | 假如有人进门就打开走廊灯 | What if we were to turn on the hallway light if someone comes in? |
| `NRF-conditional_or_hypothetical-01-05` | 如果我觉得冷再调高温度 | What if we were to raise the temperature if I feel cold? |
| `NRF-conditional_or_hypothetical-01-06` | 等我出门以后让扫地机器人工作 | What if we were to start the robot vacuum after I leave? |
| `NRF-conditional_or_hypothetical-01-07` | 要是空气不好再开净化器 | What if we were to run the purifier if the air gets bad? |
| `NRF-conditional_or_hypothetical-01-08` | 如果有人按门铃就通知我 | What if we were to notify me if someone rings the bell? |
| `NRF-conditional_or_hypothetical-01-09` | 等屋里太干了再开加湿器 | What if we were to turn on the humidifier if the air gets dry? |
| `NRF-conditional_or_hypothetical-01-10` | 万一停电了这些设备会怎样 | What if we were to what would happen if the power went out? |
| `NRF-conditional_or_hypothetical-02-01` | 我在想要不要把客厅灯换掉 | I am thinking about whether to turn on the light if I get home late. |
| `NRF-conditional_or_hypothetical-02-02` | 不知道空调调到26会不会太冷 | I am thinking about whether to switch on the AC when it gets hot. |
| `NRF-conditional_or_hypothetical-02-03` | 如果把窗帘换成白色会好看吗 | I am thinking about whether to close the curtains if it rains. |
| `NRF-conditional_or_hypothetical-02-04` | 风扇放在床边会不会更凉快 | I am thinking about whether to turn on the hallway light if someone comes in. |
| `NRF-conditional_or_hypothetical-02-05` | 假设家里有两台净化器怎么安排 | I am thinking about whether to raise the temperature if I feel cold. |
| `NRF-conditional_or_hypothetical-02-06` | 要是门锁没电了应该怎么办 | I am thinking about whether to start the robot vacuum after I leave. |
| `NRF-conditional_or_hypothetical-02-07` | 扫地机器人能不能避开地毯 | I am thinking about whether to run the purifier if the air gets bad. |
| `NRF-conditional_or_hypothetical-02-08` | 我考虑买个加湿器放卧室 | I am thinking about whether to notify me if someone rings the bell. |
| `NRF-conditional_or_hypothetical-02-09` | 如果灯光再暖一点可能更舒服 | I am thinking about whether to turn on the humidifier if the air gets dry. |
| `NRF-conditional_or_hypothetical-02-10` | 空调开新风和开窗有什么区别 | I am thinking about whether to what would happen if the power went out. |
| `NRF-conditional_or_hypothetical-03-01` | 明天也许会想开客厅的灯 | Maybe I will turn on the light if I get home late later. |
| `NRF-conditional_or_hypothetical-03-02` | 晚上可能会把空调温度调低 | Maybe I will switch on the AC when it gets hot later. |
| `NRF-conditional_or_hypothetical-03-03` | 周末说不定会整理一下窗帘 | Maybe I will close the curtains if it rains later. |
| `NRF-conditional_or_hypothetical-03-04` | 过几天有空再看看风扇 | Maybe I will turn on the hallway light if someone comes in later. |
| `NRF-conditional_or_hypothetical-03-05` | 下个月也许买一台净化器 | Maybe I will raise the temperature if I feel cold later. |
| `NRF-conditional_or_hypothetical-03-06` | 哪天记得检查一下门锁电池 | Maybe I will start the robot vacuum after I leave later. |
| `NRF-conditional_or_hypothetical-03-07` | 以后想起来再清理扫地机器人 | Maybe I will run the purifier if the air gets bad later. |
| `NRF-conditional_or_hypothetical-03-08` | 天气干燥的时候再拿出加湿器 | Maybe I will notify me if someone rings the bell later. |
| `NRF-conditional_or_hypothetical-03-09` | 等装修完了再考虑灯的位置 | Maybe I will turn on the humidifier if the air gets dry later. |
| `NRF-conditional_or_hypothetical-03-10` | 忙完这阵子再看看家电 | Maybe I will what would happen if the power went out later. |
| `NRF-conditional_or_hypothetical-04-01` | 要是空调自己会调温就好了 | It would be nice if I could turn on the light if I get home late. |
| `NRF-conditional_or_hypothetical-04-02` | 如果灯能随着日落慢慢变暗就好了 | It would be nice if I could switch on the AC when it gets hot. |
| `NRF-conditional_or_hypothetical-04-03` | 假如窗帘可以自动避开花盆就好了 | It would be nice if I could close the curtains if it rains. |
| `NRF-conditional_or_hypothetical-04-04` | 风扇要是更安静一些就好了 | It would be nice if I could turn on the hallway light if someone comes in. |
| `NRF-conditional_or_hypothetical-04-05` | 希望净化器的滤芯能用久一点 | It would be nice if I could raise the temperature if I feel cold. |
| `NRF-conditional_or_hypothetical-04-06` | 门锁如果能提醒电量就方便了 | It would be nice if I could start the robot vacuum after I leave. |
| `NRF-conditional_or_hypothetical-04-07` | 扫地机器人要是少卡几次就好了 | It would be nice if I could run the purifier if the air gets bad. |
| `NRF-conditional_or_hypothetical-04-08` | 加湿器如果好清洗就更实用 | It would be nice if I could notify me if someone rings the bell. |
| `NRF-conditional_or_hypothetical-04-09` | 要是每个房间都有合适的照明就好了 | It would be nice if I could turn on the humidifier if the air gets dry. |
| `NRF-conditional_or_hypothetical-04-10` | 假如音箱能记住上次播放的位置 | It would be nice if I could what would happen if the power went out. |
| `NRF-conditional_or_hypothetical-05-01` | 可能我回家以后会打开客厅灯 | I have not decided whether to turn on the light if I get home late. |
| `NRF-conditional_or_hypothetical-05-02` | 说不定晚上会把窗帘拉上 | I have not decided whether to switch on the AC when it gets hot. |
| `NRF-conditional_or_hypothetical-05-03` | 也许等会儿会开一会儿风扇 | I have not decided whether to close the curtains if it rains. |
| `NRF-conditional_or_hypothetical-05-04` | 我还没决定要不要调空调 | I have not decided whether to turn on the hallway light if someone comes in. |
| `NRF-conditional_or_hypothetical-05-05` | 过几天看看是否需要加湿器 | I have not decided whether to raise the temperature if I feel cold. |
| `NRF-conditional_or_hypothetical-05-06` | 下次再考虑怎么摆放净化器 | I have not decided whether to start the robot vacuum after I leave. |
| `NRF-conditional_or_hypothetical-05-07` | 要不要换灯我还在犹豫 | I have not decided whether to run the purifier if the air gets bad. |
| `NRF-conditional_or_hypothetical-05-08` | 晚点可能会整理扫地机器人的地图 | I have not decided whether to notify me if someone rings the bell. |
| `NRF-conditional_or_hypothetical-05-09` | 不确定要不要换门锁 | I have not decided whether to turn on the humidifier if the air gets dry. |
| `NRF-conditional_or_hypothetical-05-10` | 之后有空再想想家里的照明 | I have not decided whether to what would happen if the power went out. |
| `NRF-questions_not_requests-01-01` | 客厅空调设成26度会不会太冷 | I wonder about this: Would setting the living-room AC to 26 degrees feel too cold? |
| `NRF-questions_not_requests-01-02` | 主卧开吸顶灯是不是很亮 | I wonder about this: Is the bedroom ceiling light too bright? |
| `NRF-questions_not_requests-01-03` | 窗帘每天都拉开好吗 | I wonder about this: Is it a good idea to open the curtains every day? |
| `NRF-questions_not_requests-01-04` | 风扇开一晚上会不会不舒服 | I wonder about this: Would running a fan all night be uncomfortable? |
| `NRF-questions_not_requests-01-05` | 净化器滤芯多久换一次 | I wonder about this: How often should an air-purifier filter be changed? |
| `NRF-questions_not_requests-01-06` | 门锁电池一般能用多久 | I wonder about this: How long does a smart-lock battery usually last? |
| `NRF-questions_not_requests-01-07` | 扫地机器人应该每天清扫吗 | I wonder about this: Should a robot vacuum clean every day? |
| `NRF-questions_not_requests-01-08` | 加湿器放床头合适吗 | I wonder about this: Is it okay to put a humidifier by the bed? |
| `NRF-questions_not_requests-01-09` | 暖色灯光是不是更适合晚上 | I wonder about this: Is warm lighting better in the evening? |
| `NRF-questions_not_requests-01-10` | 电视的CCTV1今天播什么 | I wonder about this: What is on CCTV-1 today? |
| `NRF-questions_not_requests-02-01` | 这盏灯是什么型号 | This question came to mind: Would setting the living-room AC to 26 degrees feel too cold? |
| `NRF-questions_not_requests-02-02` | 空调的除湿模式和制冷有什么区别 | This question came to mind: Is the bedroom ceiling light too bright? |
| `NRF-questions_not_requests-02-03` | 窗帘布料要怎么清洗 | This question came to mind: Is it a good idea to open the curtains every day? |
| `NRF-questions_not_requests-02-04` | 风扇的叶片怎么拆下来 | This question came to mind: Would running a fan all night be uncomfortable? |
| `NRF-questions_not_requests-02-05` | 净化器放在哪个位置比较好 | This question came to mind: How often should an air-purifier filter be changed? |
| `NRF-questions_not_requests-02-06` | 智能门锁没电会有什么提示 | This question came to mind: How long does a smart-lock battery usually last? |
| `NRF-questions_not_requests-02-07` | 扫地机器人怎么清理滚刷 | This question came to mind: Should a robot vacuum clean every day? |
| `NRF-questions_not_requests-02-08` | 加湿器用自来水可以吗 | This question came to mind: Is it okay to put a humidifier by the bed? |
| `NRF-questions_not_requests-02-09` | 吸顶灯的色温是什么意思 | This question came to mind: Is warm lighting better in the evening? |
| `NRF-questions_not_requests-02-10` | 电视遥控器上的按键怎么用 | This question came to mind: What is on CCTV-1 today? |
| `NRF-questions_not_requests-03-01` | 有人知道灯泡一般能用几年吗 | I was just wondering: Would setting the living-room AC to 26 degrees feel too cold? |
| `NRF-questions_not_requests-03-02` | 为什么有些空调声音比较大 | I was just wondering: Is the bedroom ceiling light too bright? |
| `NRF-questions_not_requests-03-03` | 窗帘装在窗框里面好看吗 | I was just wondering: Is it a good idea to open the curtains every day? |
| `NRF-questions_not_requests-03-04` | 风扇对着墙吹是不是更舒服 | I was just wondering: Would running a fan all night be uncomfortable? |
| `NRF-questions_not_requests-03-05` | 空气净化器开窗时还有用吗 | I was just wondering: How often should an air-purifier filter be changed? |
| `NRF-questions_not_requests-03-06` | 门铃和猫眼有什么区别 | I was just wondering: How long does a smart-lock battery usually last? |
| `NRF-questions_not_requests-03-07` | 扫地机器人能过多高的门槛 | I was just wondering: Should a robot vacuum clean every day? |
| `NRF-questions_not_requests-03-08` | 加湿器开久了墙面会不会潮 | I was just wondering: Is it okay to put a humidifier by the bed? |
| `NRF-questions_not_requests-03-09` | 主灯和筒灯怎么搭配 | I was just wondering: Is warm lighting better in the evening? |
| `NRF-questions_not_requests-03-10` | CCTV1现在正在播什么节目 | I was just wondering: What is on CCTV-1 today? |
| `NRF-questions_not_requests-04-01` | 你们觉得客厅灯用暖光还是白光 | Someone asked me, “Would setting the living-room AC to 26 degrees feel too cold?” |
| `NRF-questions_not_requests-04-02` | 夏天空调开多少度比较舒服 | Someone asked me, “Is the bedroom ceiling light too bright?” |
| `NRF-questions_not_requests-04-03` | 窗帘选遮光还是半遮光的 | Someone asked me, “Is it a good idea to open the curtains every day?” |
| `NRF-questions_not_requests-04-04` | 循环扇和普通风扇有什么不同 | Someone asked me, “Would running a fan all night be uncomfortable?” |
| `NRF-questions_not_requests-04-05` | 净化器需要一直开着吗 | Someone asked me, “How often should an air-purifier filter be changed?” |
| `NRF-questions_not_requests-04-06` | 指纹锁和密码锁哪个更方便 | Someone asked me, “How long does a smart-lock battery usually last?” |
| `NRF-questions_not_requests-04-07` | 扫地前要不要把椅子搬开 | Someone asked me, “Should a robot vacuum clean every day?” |
| `NRF-questions_not_requests-04-08` | 加湿器的湿度设多少比较合适 | Someone asked me, “Is it okay to put a humidifier by the bed?” |
| `NRF-questions_not_requests-04-09` | 厨房筒灯装几个比较亮 | Someone asked me, “Is warm lighting better in the evening?” |
| `NRF-questions_not_requests-04-10` | 老电视还能不能收到CCTV1 | Someone asked me, “What is on CCTV-1 today?” |
| `NRF-questions_not_requests-05-01` | 昨晚那盏灯是不是有点闪 | I have a question about this: Would setting the living-room AC to 26 degrees feel too cold? |
| `NRF-questions_not_requests-05-02` | 空调制冷的时候为什么会滴水 | I have a question about this: Is the bedroom ceiling light too bright? |
| `NRF-questions_not_requests-05-03` | 窗帘被太阳晒褪色怎么办 | I have a question about this: Is it a good idea to open the curtains every day? |
| `NRF-questions_not_requests-05-04` | 风扇摆头的时候有点异响正常吗 | I have a question about this: Would running a fan all night be uncomfortable? |
| `NRF-questions_not_requests-05-05` | 净化器显示的数字代表什么 | I have a question about this: How often should an air-purifier filter be changed? |
| `NRF-questions_not_requests-05-06` | 门锁把手有点松要怎么处理 | I have a question about this: How long does a smart-lock battery usually last? |
| `NRF-questions_not_requests-05-07` | 扫地机器人老是绕着桌腿转 | I have a question about this: Should a robot vacuum clean every day? |
| `NRF-questions_not_requests-05-08` | 加湿器底部的水垢怎么清理 | I have a question about this: Is it okay to put a humidifier by the bed? |
| `NRF-questions_not_requests-05-09` | 灯罩积灰多久擦一次 | I have a question about this: Is warm lighting better in the evening? |
| `NRF-questions_not_requests-05-10` | 电视画面有点暗是什么原因 | I have a question about this: What is on CCTV-1 today? |
| `NRF-background_media_or_transcription-01-01` | 电视里正在播放CCTV1的新闻 | In the background, the news is on CCTV-1. |
| `NRF-background_media_or_transcription-01-02` | 收音机里讲到今天的天气 | In the background, someone on the radio is talking about the weather. |
| `NRF-background_media_or_transcription-01-03` | 手机视频里有人说打开客厅的灯 | In the background, a phone video says “turn on the living-room light”. |
| `NRF-background_media_or_transcription-01-04` | 旁边的人正在讨论空调温度 | In the background, people nearby are discussing the AC temperature. |
| `NRF-background_media_or_transcription-01-05` | 隔壁传来一声把窗帘拉开的说话声 | In the background, someone next door said to open the curtains. |
| `NRF-background_media_or_transcription-01-06` | 短视频里介绍扫地机器人 | In the background, a short video is reviewing robot vacuums. |
| `NRF-background_media_or_transcription-01-07` | 广播里提到了空气净化器 | In the background, the broadcast mentioned air purifiers. |
| `NRF-background_media_or_transcription-01-08` | 电视节目正在讲智能门锁 | In the background, a TV show is talking about smart locks. |
| `NRF-background_media_or_transcription-01-09` | 有人在看加湿器测评视频 | In the background, someone is watching a humidifier review. |
| `NRF-background_media_or_transcription-01-10` | 客厅里传来一段风扇广告 | In the background, there is an ad for a fan playing in the living room. |
| `NRF-background_media_or_transcription-02-01` | 新闻主播正在介绍新款智能灯 | The TV is on; the news is on CCTV-1. |
| `NRF-background_media_or_transcription-02-02` | 电视节目字幕出现了空调两个字 | The TV is on; someone on the radio is talking about the weather. |
| `NRF-background_media_or_transcription-02-03` | 视频博主演示如何拆洗风扇 | The TV is on; a phone video says “turn on the living-room light”. |
| `NRF-background_media_or_transcription-02-04` | 广告里反复说打开净化器 | The TV is on; people nearby are discussing the AC temperature. |
| `NRF-background_media_or_transcription-02-05` | 综艺节目里有人提到窗帘 | The TV is on; someone next door said to open the curtains. |
| `NRF-background_media_or_transcription-02-06` | 纪录片拍到了扫地机器人 | The TV is on; a short video is reviewing robot vacuums. |
| `NRF-background_media_or_transcription-02-07` | 电视剧里角色正在讨论门锁 | The TV is on; the broadcast mentioned air purifiers. |
| `NRF-background_media_or_transcription-02-08` | 手机外放着加湿器评测 | The TV is on; a TV show is talking about smart locks. |
| `NRF-background_media_or_transcription-02-09` | 旁边音箱正在播天气预报 | The TV is on; someone is watching a humidifier review. |
| `NRF-background_media_or_transcription-02-10` | 新闻正在报道智能家居展会 | The TV is on; there is an ad for a fan playing in the living room. |
| `NRF-background_media_or_transcription-03-01` | 我刚才在看CCTV1的节目回放 | I can hear that the news is on CCTV-1. |
| `NRF-background_media_or_transcription-03-02` | 电视背景音里一直有人说话 | I can hear that someone on the radio is talking about the weather. |
| `NRF-background_media_or_transcription-03-03` | 手机里那段视频还没播完 | I can hear that a phone video says “turn on the living-room light”. |
| `NRF-background_media_or_transcription-03-04` | 隔壁房间的节目声音传过来了 | I can hear that people nearby are discussing the AC temperature. |
| `NRF-background_media_or_transcription-03-05` | 收音机正在放一段采访 | I can hear that someone next door said to open the curtains. |
| `NRF-background_media_or_transcription-03-06` | 短视频自动播放到下一条了 | I can hear that a short video is reviewing robot vacuums. |
| `NRF-background_media_or_transcription-03-07` | 家里人正在看新闻联播 | I can hear that the broadcast mentioned air purifiers. |
| `NRF-background_media_or_transcription-03-08` | 朋友发来的语音还在播放 | I can hear that a TV show is talking about smart locks. |
| `NRF-background_media_or_transcription-03-09` | 电脑网页上有一段产品介绍 | I can hear that someone is watching a humidifier review. |
| `NRF-background_media_or_transcription-03-10` | 广告音乐从电视里传出来 | I can hear that there is an ad for a fan playing in the living room. |
| `NRF-background_media_or_transcription-04-01` | 画面上写着客厅灯光设计案例 | A video is playing where the news is on CCTV-1. |
| `NRF-background_media_or_transcription-04-02` | 电视里正在讲空调省电技巧 | A video is playing where someone on the radio is talking about the weather. |
| `NRF-background_media_or_transcription-04-03` | 视频标题是窗帘安装全过程 | A video is playing where a phone video says “turn on the living-room light”. |
| `NRF-background_media_or_transcription-04-04` | 节目里展示了几种循环扇 | A video is playing where people nearby are discussing the AC temperature. |
| `NRF-background_media_or_transcription-04-05` | 广告介绍空气净化器的新功能 | A video is playing where someone next door said to open the curtains. |
| `NRF-background_media_or_transcription-04-06` | 新闻画面带过一把智能门锁 | A video is playing where a short video is reviewing robot vacuums. |
| `NRF-background_media_or_transcription-04-07` | 视频里扫地机器人撞到了鞋子 | A video is playing where the broadcast mentioned air purifiers. |
| `NRF-background_media_or_transcription-04-08` | 主播拿着加湿器介绍水箱 | A video is playing where a TV show is talking about smart locks. |
| `NRF-background_media_or_transcription-04-09` | 字幕正在滚动显示CCTV1节目单 | A video is playing where someone is watching a humidifier review. |
| `NRF-background_media_or_transcription-04-10` | 纪录片里出现了老式吊灯 | A video is playing where there is an ad for a fan playing in the living room. |
| `NRF-background_media_or_transcription-05-01` | 有人在电视里问空调为什么漏水 | There is some background audio: the news is on CCTV-1. |
| `NRF-background_media_or_transcription-05-02` | 视频旁白提到了厨房吸顶灯 | There is some background audio: someone on the radio is talking about the weather. |
| `NRF-background_media_or_transcription-05-03` | 收音机主持人在聊夏天用风扇 | There is some background audio: a phone video says “turn on the living-room light”. |
| `NRF-background_media_or_transcription-05-04` | 新闻画面里看到一排窗帘 | There is some background audio: people nearby are discussing the AC temperature. |
| `NRF-background_media_or_transcription-05-05` | 广告演员说空气质量很重要 | There is some background audio: someone next door said to open the curtains. |
| `NRF-background_media_or_transcription-05-06` | 节目展示如何清理扫地机滚刷 | There is some background audio: a short video is reviewing robot vacuums. |
| `NRF-background_media_or_transcription-05-07` | 门外有人在讨论门铃的声音 | There is some background audio: the broadcast mentioned air purifiers. |
| `NRF-background_media_or_transcription-05-08` | 视频里比较了不同加湿器 | There is some background audio: a TV show is talking about smart locks. |
| `NRF-background_media_or_transcription-05-09` | 电视正在播一段家电广告 | There is some background audio: someone is watching a humidifier review. |
| `NRF-background_media_or_transcription-05-10` | 背景音里听见了“打开灯”三个字 | There is some background audio: there is an ad for a fan playing in the living room. |
| `NRF-fragment_or_ambiguous-01-01` | 那个灯啊 | I was thinking about that lamp. |
| `NRF-fragment_or_ambiguous-01-02` | 客厅那个东西 | I was thinking about that thing in the living room. |
| `NRF-fragment_or_ambiguous-01-03` | 温度嘛，差不多就行 | I was thinking about the temperature, more or less. |
| `NRF-fragment_or_ambiguous-01-04` | 上次说的那个设备 | I was thinking about the device we talked about last time. |
| `NRF-fragment_or_ambiguous-01-05` | 窗边那个，怎么说呢 | I was thinking about that thing by the window. |
| `NRF-fragment_or_ambiguous-01-06` | 楼上那间房 | I was thinking about the room upstairs. |
| `NRF-fragment_or_ambiguous-01-07` | 昨天那个家电 | I was thinking about that appliance from yesterday. |
| `NRF-fragment_or_ambiguous-01-08` | 灯光这块儿 | I was thinking about the lighting situation. |
| `NRF-fragment_or_ambiguous-01-09` | 空调那件事 | I was thinking about the AC thing. |
| `NRF-fragment_or_ambiguous-01-10` | 等下再说那个 | I was thinking about that thing we mentioned later. |
| `NRF-fragment_or_ambiguous-02-01` | 把那个弄一下吧 | Maybe it was that lamp; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-02` | 房间里那个东西呢 | Maybe it was that thing in the living room; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-03` | 你知道我说的那个 | Maybe it was the temperature, more or less; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-04` | 差不多就那个意思 | Maybe it was the device we talked about last time; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-05` | 好像在客厅那边 | Maybe it was that thing by the window; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-06` | 那个开关在哪里来着 | Maybe it was the room upstairs; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-07` | 前两天买的那一个 | Maybe it was that appliance from yesterday; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-08` | 再稍微一点点 | Maybe it was the lighting situation; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-09` | 好像不是这个颜色 | Maybe it was the AC thing; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-02-10` | 具体名字我想不起来了 | Maybe it was that thing we mentioned later; I cannot quite remember. |
| `NRF-fragment_or_ambiguous-03-01` | 灯和空调的事情我还没想好 | Something about that lamp… |
| `NRF-fragment_or_ambiguous-03-02` | 那个设备可能放在卧室 | Something about that thing in the living room… |
| `NRF-fragment_or_ambiguous-03-03` | 开还是不开我有点纠结 | Something about the temperature, more or less… |
| `NRF-fragment_or_ambiguous-03-04` | 客厅和主卧都差不多吧 | Something about the device we talked about last time… |
| `NRF-fragment_or_ambiguous-03-05` | 亮一点还是暗一点呢 | Something about that thing by the window… |
| `NRF-fragment_or_ambiguous-03-06` | 是不是上次说的那个型号 | Something about the room upstairs… |
| `NRF-fragment_or_ambiguous-03-07` | 好像可以调又好像不行 | Something about that appliance from yesterday… |
| `NRF-fragment_or_ambiguous-03-08` | 这几个按钮看起来都一样 | Something about the lighting situation… |
| `NRF-fragment_or_ambiguous-03-09` | 还有那个东西也要看看 | Something about the AC thing… |
| `NRF-fragment_or_ambiguous-03-10` | 东西放哪儿忘记了 | Something about that thing we mentioned later… |
| `NRF-fragment_or_ambiguous-04-01` | 打开……算了我忘了要开什么 | I am not sure what to do about that lamp. |
| `NRF-fragment_or_ambiguous-04-02` | 客厅那个……不是，先不说了 | I am not sure what to do about that thing in the living room. |
| `NRF-fragment_or_ambiguous-04-03` | 把温度调……等会儿再弄 | I am not sure what to do about the temperature, more or less. |
| `NRF-fragment_or_ambiguous-04-04` | 灯那个事情回头再想 | I am not sure what to do about the device we talked about last time. |
| `NRF-fragment_or_ambiguous-04-05` | 说到空调我突然忘了 | I am not sure what to do about that thing by the window. |
| `NRF-fragment_or_ambiguous-04-06` | 刚才想说什么来着 | I am not sure what to do about the room upstairs. |
| `NRF-fragment_or_ambiguous-04-07` | 先看一下再决定吧 | I am not sure what to do about that appliance from yesterday. |
| `NRF-fragment_or_ambiguous-04-08` | 可能是窗边的那个东西 | I am not sure what to do about the lighting situation. |
| `NRF-fragment_or_ambiguous-04-09` | 大概就这样吧我也不确定 | I am not sure what to do about the AC thing. |
| `NRF-fragment_or_ambiguous-04-10` | 等我找到了再说 | I am not sure what to do about that thing we mentioned later. |
| `NRF-fragment_or_ambiguous-05-01` | 那个灯是不是挺不错的 | What was I saying about that lamp? |
| `NRF-fragment_or_ambiguous-05-02` | 空调好像还可以吧 | What was I saying about that thing in the living room? |
| `NRF-fragment_or_ambiguous-05-03` | 窗帘我觉得也还行 | What was I saying about the temperature, more or less? |
| `NRF-fragment_or_ambiguous-05-04` | 风扇可能放这里比较好 | What was I saying about the device we talked about last time? |
| `NRF-fragment_or_ambiguous-05-05` | 净化器那个我不太懂 | What was I saying about that thing by the window? |
| `NRF-fragment_or_ambiguous-05-06` | 门锁好像有好几种 | What was I saying about the room upstairs? |
| `NRF-fragment_or_ambiguous-05-07` | 扫地机应该差不多吧 | What was I saying about that appliance from yesterday? |
| `NRF-fragment_or_ambiguous-05-08` | 加湿器这个先放一边 | What was I saying about the lighting situation? |
| `NRF-fragment_or_ambiguous-05-09` | 屏幕上的图标看不清 | What was I saying about the AC thing? |
| `NRF-fragment_or_ambiguous-05-10` | 刚才那个声音是什么来着 | What was I saying about that thing we mentioned later? |
| `NRF-third_party_or_directed_elsewhere-01-01` | 妈，麻烦你把客厅灯打开 | I told someone else: Mom, could you turn on the living-room light? |
| `NRF-third_party_or_directed_elsewhere-01-02` | 小王，帮忙看看空调怎么了 | I told someone else: Xiao Wang, can you check what is wrong with the AC? |
| `NRF-third_party_or_directed_elsewhere-01-03` | 爸，你回家记得拉上窗帘 | I told someone else: Dad, remember to close the curtains when you get home. |
| `NRF-third_party_or_directed_elsewhere-01-04` | 师傅，风扇好像不转了 | I told someone else: The repair person says the fan may be broken. |
| `NRF-third_party_or_directed_elsewhere-01-05` | 同事说净化器滤芯要换 | I told someone else: A coworker said the purifier filter needs replacing. |
| `NRF-third_party_or_directed_elsewhere-01-06` | 朋友让我帮他挑一把门锁 | I told someone else: A friend asked me to help pick a door lock. |
| `NRF-third_party_or_directed_elsewhere-01-07` | 孩子说扫地机器人又卡住了 | I told someone else: The kid said the robot vacuum got stuck again. |
| `NRF-third_party_or_directed_elsewhere-01-08` | 邻居问加湿器是不是漏水 | I told someone else: The neighbor asked if the humidifier is leaking. |
| `NRF-third_party_or_directed_elsewhere-01-09` | 店员介绍了一款新吊灯 | I told someone else: The shop assistant showed me a new ceiling lamp. |
| `NRF-third_party_or_directed_elsewhere-01-10` | 医生让我记得早点休息 | I told someone else: The doctor told me to get more rest. |
| `NRF-third_party_or_directed_elsewhere-02-01` | 我跟家里人说晚上记得关灯 | Someone asked me to pass this along: Mom, could you turn on the living-room light? |
| `NRF-third_party_or_directed_elsewhere-02-02` | 同事让我把那份文件发过去 | Someone asked me to pass this along: Xiao Wang, can you check what is wrong with the AC? |
| `NRF-third_party_or_directed_elsewhere-02-03` | 朋友说周末一起去吃饭 | Someone asked me to pass this along: Dad, remember to close the curtains when you get home. |
| `NRF-third_party_or_directed_elsewhere-02-04` | 爸妈问我什么时候回家 | Someone asked me to pass this along: The repair person says the fan may be broken. |
| `NRF-third_party_or_directed_elsewhere-02-05` | 邻居拜托我代收一个快递 | Someone asked me to pass this along: A coworker said the purifier filter needs replacing. |
| `NRF-third_party_or_directed_elsewhere-02-06` | 店员说这个型号正在打折 | Someone asked me to pass this along: A friend asked me to help pick a door lock. |
| `NRF-third_party_or_directed_elsewhere-02-07` | 师傅建议先检查一下电源 | Someone asked me to pass this along: The kid said the robot vacuum got stuck again. |
| `NRF-third_party_or_directed_elsewhere-02-08` | 孩子让我陪他看动画片 | Someone asked me to pass this along: The neighbor asked if the humidifier is leaking. |
| `NRF-third_party_or_directed_elsewhere-02-09` | 室友说窗户记得关一下 | Someone asked me to pass this along: The shop assistant showed me a new ceiling lamp. |
| `NRF-third_party_or_directed_elsewhere-02-10` | 朋友约我下班去散步 | Someone asked me to pass this along: The doctor told me to get more rest. |
| `NRF-third_party_or_directed_elsewhere-03-01` | 等会儿我自己去按一下开关 | We were talking about it; Mom, could you turn on the living-room light? |
| `NRF-third_party_or_directed_elsewhere-03-02` | 等维修师傅来了再检查空调 | We were talking about it; Xiao Wang, can you check what is wrong with the AC? |
| `NRF-third_party_or_directed_elsewhere-03-03` | 我打算问问朋友怎么选窗帘 | We were talking about it; Dad, remember to close the curtains when you get home. |
| `NRF-third_party_or_directed_elsewhere-03-04` | 周末和家人一起收拾房间 | We were talking about it; The repair person says the fan may be broken. |
| `NRF-third_party_or_directed_elsewhere-03-05` | 下班路上顺便买点水果 | We were talking about it; A coworker said the purifier filter needs replacing. |
| `NRF-third_party_or_directed_elsewhere-03-06` | 回头把产品说明书找出来 | We were talking about it; A friend asked me to help pick a door lock. |
| `NRF-third_party_or_directed_elsewhere-03-07` | 我先给售后打个电话问问 | We were talking about it; The kid said the robot vacuum got stuck again. |
| `NRF-third_party_or_directed_elsewhere-03-08` | 这件事还是和室友商量一下 | We were talking about it; The neighbor asked if the humidifier is leaking. |
| `NRF-third_party_or_directed_elsewhere-03-09` | 明天记得提醒爸妈带钥匙 | We were talking about it; The shop assistant showed me a new ceiling lamp. |
| `NRF-third_party_or_directed_elsewhere-03-10` | 店里的人说可以上门安装 | We were talking about it; The doctor told me to get more rest. |
| `NRF-third_party_or_directed_elsewhere-04-01` | 我刚叫孩子别碰墙上的开关 | That was directed to my family, not the assistant: Mom, could you turn on the living-room light? |
| `NRF-third_party_or_directed_elsewhere-04-02` | 刚才提醒室友不要挡住风扇 | That was directed to my family, not the assistant: Xiao Wang, can you check what is wrong with the AC? |
| `NRF-third_party_or_directed_elsewhere-04-03` | 邻居说他家的灯泡坏了 | That was directed to my family, not the assistant: Dad, remember to close the curtains when you get home. |
| `NRF-third_party_or_directed_elsewhere-04-04` | 朋友正在教我怎么装窗帘 | That was directed to my family, not the assistant: The repair person says the fan may be broken. |
| `NRF-third_party_or_directed_elsewhere-04-05` | 师傅把空调滤网拆下来清洗 | That was directed to my family, not the assistant: A coworker said the purifier filter needs replacing. |
| `NRF-third_party_or_directed_elsewhere-04-06` | 店员给我看了几款加湿器 | That was directed to my family, not the assistant: A friend asked me to help pick a door lock. |
| `NRF-third_party_or_directed_elsewhere-04-07` | 同事正在讲家里装修的事 | That was directed to my family, not the assistant: The kid said the robot vacuum got stuck again. |
| `NRF-third_party_or_directed_elsewhere-04-08` | 爸妈聊起老房子的门锁 | That was directed to my family, not the assistant: The neighbor asked if the humidifier is leaking. |
| `NRF-third_party_or_directed_elsewhere-04-09` | 孩子把扫地机器人当成玩具 | That was directed to my family, not the assistant: The shop assistant showed me a new ceiling lamp. |
| `NRF-third_party_or_directed_elsewhere-04-10` | 朋友在群里分享净化器测评 | That was directed to my family, not the assistant: The doctor told me to get more rest. |
| `NRF-third_party_or_directed_elsewhere-05-01` | 这个问题我准备自己处理 | I was just repeating what someone said: Mom, could you turn on the living-room light? |
| `NRF-third_party_or_directed_elsewhere-05-02` | 我已经跟师傅约好上门时间 | I was just repeating what someone said: Xiao Wang, can you check what is wrong with the AC? |
| `NRF-third_party_or_directed_elsewhere-05-03` | 朋友说等他有空再来看一下 | I was just repeating what someone said: Dad, remember to close the curtains when you get home. |
| `NRF-third_party_or_directed_elsewhere-05-04` | 家里人讨论了半天还没决定 | I was just repeating what someone said: The repair person says the fan may be broken. |
| `NRF-third_party_or_directed_elsewhere-05-05` | 售后那边还没有回复消息 | I was just repeating what someone said: A coworker said the purifier filter needs replacing. |
| `NRF-third_party_or_directed_elsewhere-05-06` | 室友说可以先保持原来的样子 | I was just repeating what someone said: A friend asked me to help pick a door lock. |
| `NRF-third_party_or_directed_elsewhere-05-07` | 我准备周末去店里实际看看 | I was just repeating what someone said: The kid said the robot vacuum got stuck again. |
| `NRF-third_party_or_directed_elsewhere-05-08` | 邻居建议我先量一下窗户尺寸 | I was just repeating what someone said: The neighbor asked if the humidifier is leaking. |
| `NRF-third_party_or_directed_elsewhere-05-09` | 同事说网上有安装教程 | I was just repeating what someone said: The shop assistant showed me a new ceiling lamp. |
| `NRF-third_party_or_directed_elsewhere-05-10` | 这件小事等回家再商量 | I was just repeating what someone said: The doctor told me to get more rest. |
| `NRC-mood-01` | 今天脑子转得不太快。 | My brain is not working very quickly today. |
| `NRC-mood-02` | 刚才想起来，今天耐心好像特别足呢。 | I just remembered that I seem to have plenty of patience today. |
| `NRC-mood-03` | 说起来，刚才突然觉得有点累啊。 | Speaking of that, I suddenly felt a bit tired. |
| `NRC-mood-04` | 今天忽然觉得，刚刚有点走神。 | It suddenly occurred to me that I zoned out for a moment. |
| `NRC-mood-05` | 我发现，心情比昨天放松一些呢。 | I noticed that I feel more relaxed than yesterday. |
| `NRC-mood-06` | 说到这个，一想到周末就开心呢。 | By the way, thinking about the weekend makes me happy. |
| `NRC-mood-07` | 刚刚注意到，早上起来有点迷糊啊。 | Just now, I felt a little groggy when I got up. |
| `NRC-mood-08` | 今天有一点小小的成就感。 | I am quietly pleased with what I got done today. |
| `NRC-mood-09` | 刚才想起来，今天状态还不错呢。 | I just remembered that I am in a pretty good mood today. |
| `NRC-mood-10` | 说起来，忙完以后心里轻松多了。 | Speaking of that, I feel lighter now that I have finished everything. |
| `NRC-mood-11` | 今天忽然觉得，最近总觉得时间过得很快啊。 | It suddenly occurred to me that time seems to be flying by lately. |
| `NRC-mood-12` | 我发现，最近情绪挺平稳。 | I noticed that I have felt pretty steady lately. |
| `NRC-mood-13` | 说到这个，这会儿只想安静一会儿呢。 | By the way, I just want a little quiet time. |
| `NRC-mood-14` | 刚刚注意到，午后有点犯困。 | Just now, I am getting sleepy this afternoon. |
| `NRC-mood-15` | 有些事情想开以后舒服多了呢。 | It feels better after I put things in perspective. |
| `NRC-mood-16` | 刚才想起来，今天脑子转得不太快。 | I just remembered that my brain is not working very quickly today. |
| `NRC-mood-17` | 说起来，今天耐心好像特别足呢。 | Speaking of that, I seem to have plenty of patience today. |
| `NRC-mood-18` | 今天忽然觉得，刚才突然觉得有点累。 | It suddenly occurred to me that I suddenly felt a bit tired. |
| `NRC-mood-19` | 我发现，刚刚有点走神呢。 | I noticed that I zoned out for a moment. |
| `NRC-mood-20` | 说到这个，心情比昨天放松一些啊。 | By the way, I feel more relaxed than yesterday. |
| `NRC-mood-21` | 刚刚注意到，一想到周末就开心呢。 | Just now, thinking about the weekend makes me happy. |
| `NRC-mood-22` | 早上起来有点迷糊。 | I felt a little groggy when I got up. |
| `NRC-mood-23` | 刚才想起来，今天有一点小小的成就感呢。 | I just remembered that I am quietly pleased with what I got done today. |
| `NRC-mood-24` | 说起来，今天状态还不错啊。 | Speaking of that, I am in a pretty good mood today. |
| `NRC-mood-25` | 今天忽然觉得，忙完以后心里轻松多了。 | It suddenly occurred to me that I feel lighter now that I have finished everything. |
| `NRC-food-01` | 那家小店的馄饨味道不错。 | The wontons at that little place taste great. |
| `NRC-food-02` | 刚才想起来，今天做的番茄炒蛋挺下饭呢。 | I just remembered that the tomato and egg stir-fry was great with rice. |
| `NRC-food-03` | 说起来，晚上想吃点清淡的啊。 | Speaking of that, I feel like having something light for dinner. |
| `NRC-food-04` | 今天忽然觉得，最近吃辣有点多了。 | It suddenly occurred to me that I have been eating a lot of spicy food lately. |
| `NRC-food-05` | 我发现，刚买的橘子挺甜呢。 | I noticed that the oranges I bought are really sweet. |
| `NRC-food-06` | 说到这个，这家的饺子皮擀得很薄呢。 | By the way, the dumpling wrappers at this place are so thin. |
| `NRC-food-07` | 刚刚注意到，今天的米饭煮得刚刚好啊。 | Just now, the rice came out just right today. |
| `NRC-food-08` | 刚才喝了一杯热豆浆。 | I just had a cup of warm soy milk. |
| `NRC-food-09` | 刚才想起来，中午吃了碗面呢。 | I just remembered that I had a bowl of noodles for lunch. |
| `NRC-food-10` | 说起来，这两天特别想吃烤红薯。 | Speaking of that, I have been craving roasted sweet potatoes lately. |
| `NRC-food-11` | 今天忽然觉得，这个季节的葡萄很新鲜啊。 | It suddenly occurred to me that grapes are really fresh this time of year. |
| `NRC-food-12` | 我发现，冰箱里还有半个西瓜。 | I noticed that there is half a watermelon left in the fridge. |
| `NRC-food-13` | 说到这个，下午泡了杯茶呢。 | By the way, I made myself a cup of tea this afternoon. |
| `NRC-food-14` | 刚刚注意到，早饭吃得有点晚。 | Just now, I had breakfast pretty late today. |
| `NRC-food-15` | 买的面包还留了一半呢。 | There is still half a loaf of bread left. |
| `NRC-food-16` | 刚才想起来，那家小店的馄饨味道不错。 | I just remembered that the wontons at that little place taste great. |
| `NRC-food-17` | 说起来，今天做的番茄炒蛋挺下饭呢。 | Speaking of that, the tomato and egg stir-fry was great with rice. |
| `NRC-food-18` | 今天忽然觉得，晚上想吃点清淡的。 | It suddenly occurred to me that I feel like having something light for dinner. |
| `NRC-food-19` | 我发现，最近吃辣有点多了呢。 | I noticed that I have been eating a lot of spicy food lately. |
| `NRC-food-20` | 说到这个，刚买的橘子挺甜啊。 | By the way, the oranges I bought are really sweet. |
| `NRC-food-21` | 刚刚注意到，这家的饺子皮擀得很薄呢。 | Just now, the dumpling wrappers at this place are so thin. |
| `NRC-food-22` | 今天的米饭煮得刚刚好。 | The rice came out just right today. |
| `NRC-food-23` | 刚才想起来，刚才喝了一杯热豆浆呢。 | I just remembered that I just had a cup of warm soy milk. |
| `NRC-food-24` | 说起来，中午吃了碗面啊。 | Speaking of that, I had a bowl of noodles for lunch. |
| `NRC-food-25` | 今天忽然觉得，这两天特别想吃烤红薯。 | It suddenly occurred to me that I have been craving roasted sweet potatoes lately. |
| `NRC-weather-01` | 雨停以后路面还没干。 | The road is still wet after the rain. |
| `NRC-weather-02` | 刚才想起来，这个天气适合出去走走呢。 | I just remembered that this weather would be nice for a walk. |
| `NRC-weather-03` | 说起来，今天云层看起来很厚啊。 | Speaking of that, the sky looks very overcast today. |
| `NRC-weather-04` | 今天忽然觉得，窗外一直有树叶在晃。 | It suddenly occurred to me that the leaves keep rustling outside. |
| `NRC-weather-05` | 我发现，天气预报说晚上会降温呢。 | I noticed that the forecast says it will cool down tonight. |
| `NRC-weather-06` | 说到这个，今天感觉比昨天闷热呢。 | By the way, it feels more muggy than yesterday. |
| `NRC-weather-07` | 刚刚注意到，这几天湿度好像挺高啊。 | Just now, it has felt quite humid these past few days. |
| `NRC-weather-08` | 天空的颜色看起来很清。 | The sky looks so clear today. |
| `NRC-weather-09` | 刚才想起来，刚才飘了几滴雨呢。 | I just remembered that a few raindrops just fell. |
| `NRC-weather-10` | 说起来，最近早晚温差有点大。 | Speaking of that, the temperature has been changing a lot between morning and night. |
| `NRC-weather-11` | 今天忽然觉得，午后的阳光特别好啊。 | It suddenly occurred to me that the afternoon sunshine is lovely. |
| `NRC-weather-12` | 我发现，傍晚的风吹着很舒服。 | I noticed that the evening breeze feels nice. |
| `NRC-weather-13` | 说到这个，早上出门时空气有点凉呢。 | By the way, it felt a little chilly when I went out this morning. |
| `NRC-weather-14` | 刚刚注意到，太阳出来以后暖和多了。 | Just now, it got much warmer once the sun came out. |
| `NRC-weather-15` | 今天外面风挺大的呢。 | It is pretty windy outside today. |
| `NRC-weather-16` | 刚才想起来，雨停以后路面还没干。 | I just remembered that the road is still wet after the rain. |
| `NRC-weather-17` | 说起来，这个天气适合出去走走呢。 | Speaking of that, this weather would be nice for a walk. |
| `NRC-weather-18` | 今天忽然觉得，今天云层看起来很厚。 | It suddenly occurred to me that the sky looks very overcast today. |
| `NRC-weather-19` | 我发现，窗外一直有树叶在晃呢。 | I noticed that the leaves keep rustling outside. |
| `NRC-weather-20` | 说到这个，天气预报说晚上会降温啊。 | By the way, the forecast says it will cool down tonight. |
| `NRC-weather-21` | 刚刚注意到，今天感觉比昨天闷热呢。 | Just now, it feels more muggy than yesterday. |
| `NRC-weather-22` | 这几天湿度好像挺高。 | It has felt quite humid these past few days. |
| `NRC-weather-23` | 刚才想起来，天空的颜色看起来很清呢。 | I just remembered that the sky looks so clear today. |
| `NRC-weather-24` | 说起来，刚才飘了几滴雨啊。 | Speaking of that, a few raindrops just fell. |
| `NRC-weather-25` | 今天忽然觉得，最近早晚温差有点大。 | It suddenly occurred to me that the temperature has been changing a lot between morning and night. |
| `NRC-commute-01` | 今天停车的位置离门口很近。 | I found a parking spot close to the entrance today. |
| `NRC-commute-02` | 刚才想起来，外面路灯亮得挺早呢。 | I just remembered that the streetlights came on pretty early today. |
| `NRC-commute-03` | 说起来，骑车回家一路都挺顺啊。 | Speaking of that, my bike ride home went smoothly. |
| `NRC-commute-04` | 今天忽然觉得，回家时刚好赶上电梯。 | It suddenly occurred to me that I got home just as the elevator arrived. |
| `NRC-commute-05` | 我发现，路口的红灯好像等了很久呢。 | I noticed that I waited at that red light for ages. |
| `NRC-commute-06` | 说到这个，高架上车流走得很慢呢。 | By the way, traffic moved very slowly on the overpass. |
| `NRC-commute-07` | 刚刚注意到，早高峰的时候人特别多啊。 | Just now, it was really crowded during rush hour. |
| `NRC-commute-08` | 路上那棵树开花了。 | A tree along the road is blooming. |
| `NRC-commute-09` | 刚才想起来，公交车今天来得挺快呢。 | I just remembered that the bus came pretty quickly today. |
| `NRC-commute-10` | 说起来，今天出门比平时早了十分钟。 | Speaking of that, I left home ten minutes earlier than usual. |
| `NRC-commute-11` | 今天忽然觉得，回来的路上经过一家花店啊。 | It suddenly occurred to me that I passed a flower shop on the way back. |
| `NRC-commute-12` | 我发现，车站旁边新开了一家便利店。 | I noticed that a new convenience store opened by the station. |
| `NRC-commute-13` | 说到这个，地铁上刚才空出来一个座位呢。 | By the way, an empty seat opened up on the train a moment ago. |
| `NRC-commute-14` | 刚刚注意到，下班路上看到一只小狗。 | Just now, I saw a little dog on my way home. |
| `NRC-commute-15` | 今天路上的车比平时多呢。 | Traffic was heavier than usual today. |
| `NRC-commute-16` | 刚才想起来，今天停车的位置离门口很近。 | I just remembered that I found a parking spot close to the entrance today. |
| `NRC-commute-17` | 说起来，外面路灯亮得挺早呢。 | Speaking of that, the streetlights came on pretty early today. |
| `NRC-commute-18` | 今天忽然觉得，骑车回家一路都挺顺。 | It suddenly occurred to me that my bike ride home went smoothly. |
| `NRC-commute-19` | 我发现，回家时刚好赶上电梯呢。 | I noticed that I got home just as the elevator arrived. |
| `NRC-commute-20` | 说到这个，路口的红灯好像等了很久啊。 | By the way, I waited at that red light for ages. |
| `NRC-commute-21` | 刚刚注意到，高架上车流走得很慢呢。 | Just now, traffic moved very slowly on the overpass. |
| `NRC-commute-22` | 早高峰的时候人特别多。 | It was really crowded during rush hour. |
| `NRC-commute-23` | 刚才想起来，路上那棵树开花了呢。 | I just remembered that a tree along the road is blooming. |
| `NRC-commute-24` | 说起来，公交车今天来得挺快啊。 | Speaking of that, the bus came pretty quickly today. |
| `NRC-commute-25` | 今天忽然觉得，今天出门比平时早了十分钟。 | It suddenly occurred to me that I left home ten minutes earlier than usual. |
| `NRC-work-01` | 手头的事情基本都处理好了。 | I have taken care of most of my tasks today. |
| `NRC-work-02` | 刚才想起来，这周的安排已经排出来了呢。 | I just remembered that this week’s schedule is all mapped out. |
| `NRC-work-03` | 说起来，同事分享了一个挺有用的方法啊。 | Speaking of that, a coworker shared a really useful tip. |
| `NRC-work-04` | 今天忽然觉得，下午临时多了一项任务。 | It suddenly occurred to me that an extra task came up this afternoon. |
| `NRC-work-05` | 我发现，今天邮件比平时多呢。 | I noticed that I got more emails than usual today. |
| `NRC-work-06` | 说到这个，新来的同事挺好沟通呢。 | By the way, the new coworker is easy to talk to. |
| `NRC-work-07` | 刚刚注意到，那份表格终于整理完了啊。 | Just now, I finally finished organizing that spreadsheet. |
| `NRC-work-08` | 刚才把文件归档好了。 | I just filed those documents away. |
| `NRC-work-09` | 刚才想起来，上午开了两个会呢。 | I just remembered that I had two meetings this morning. |
| `NRC-work-10` | 说起来，今天工作节奏有点快。 | Speaking of that, work moved pretty fast today. |
| `NRC-work-11` | 今天忽然觉得，刚结束一场挺长的讨论啊。 | It suddenly occurred to me that I just got out of a pretty long discussion. |
| `NRC-work-12` | 我发现，午休时聊了会儿项目进度。 | I noticed that we talked about the project over lunch. |
| `NRC-work-13` | 说到这个，电脑桌面总算清理了一遍呢。 | By the way, I finally cleaned up my desktop. |
| `NRC-work-14` | 刚刚注意到，报告还差最后一小段。 | Just now, the report only needs one last section. |
| `NRC-work-15` | 今天写东西比昨天顺呢。 | Writing went more smoothly today. |
| `NRC-work-16` | 刚才想起来，手头的事情基本都处理好了。 | I just remembered that I have taken care of most of my tasks today. |
| `NRC-work-17` | 说起来，这周的安排已经排出来了呢。 | Speaking of that, this week’s schedule is all mapped out. |
| `NRC-work-18` | 今天忽然觉得，同事分享了一个挺有用的方法。 | It suddenly occurred to me that a coworker shared a really useful tip. |
| `NRC-work-19` | 我发现，下午临时多了一项任务呢。 | I noticed that an extra task came up this afternoon. |
| `NRC-work-20` | 说到这个，今天邮件比平时多啊。 | By the way, I got more emails than usual today. |
| `NRC-work-21` | 刚刚注意到，新来的同事挺好沟通呢。 | Just now, the new coworker is easy to talk to. |
| `NRC-work-22` | 那份表格终于整理完了。 | I finally finished organizing that spreadsheet. |
| `NRC-work-23` | 刚才想起来，刚才把文件归档好了呢。 | I just remembered that I just filed those documents away. |
| `NRC-work-24` | 说起来，上午开了两个会啊。 | Speaking of that, I had two meetings this morning. |
| `NRC-work-25` | 今天忽然觉得，今天工作节奏有点快。 | It suddenly occurred to me that work moved pretty fast today. |
| `NRC-study-01` | 刚才把笔记重新整理了一下。 | I just reorganized my notes. |
| `NRC-study-02` | 刚才想起来，笔记里有一页写得特别认真呢。 | I just remembered that I wrote one page of notes very carefully. |
| `NRC-study-03` | 说起来，课程里的例子很贴近日常啊。 | Speaking of that, the examples in the course feel very practical. |
| `NRC-study-04` | 今天忽然觉得，这个问题换个角度就容易了。 | It suddenly occurred to me that the problem gets easier from another angle. |
| `NRC-study-05` | 我发现，今天记住了几个新单词呢。 | I noticed that I remembered a few new words today. |
| `NRC-study-06` | 说到这个，学到一半发现时间过得很快呢。 | By the way, time flew by while I was studying. |
| `NRC-study-07` | 刚刚注意到，这段文章读起来挺有意思啊。 | Just now, that article is quite interesting to read. |
| `NRC-study-08` | 今天听了一节挺清楚的课。 | I had a very clear lesson today. |
| `NRC-study-09` | 刚才想起来，刚学会一个新的快捷键呢。 | I just remembered that I just learned a new keyboard shortcut. |
| `NRC-study-10` | 说起来，那本书的插图画得很好。 | Speaking of that, the illustrations in that book are lovely. |
| `NRC-study-11` | 今天忽然觉得，最近在看一本关于历史的书啊。 | It suddenly occurred to me that I have been reading a book about history. |
| `NRC-study-12` | 我发现，最近想把基础知识再复习一遍。 | I noticed that I want to review the basics again. |
| `NRC-study-13` | 说到这个，练习题比想象中简单一些呢。 | By the way, the practice questions were easier than I expected. |
| `NRC-study-14` | 刚刚注意到，有个概念想了半天才明白。 | Just now, it took me a while to understand one concept. |
| `NRC-study-15` | 最近看到一个有趣的科普解释呢。 | I came across an interesting science explanation. |
| `NRC-study-16` | 刚才想起来，刚才把笔记重新整理了一下。 | I just remembered that I just reorganized my notes. |
| `NRC-study-17` | 说起来，笔记里有一页写得特别认真呢。 | Speaking of that, I wrote one page of notes very carefully. |
| `NRC-study-18` | 今天忽然觉得，课程里的例子很贴近日常。 | It suddenly occurred to me that the examples in the course feel very practical. |
| `NRC-study-19` | 我发现，这个问题换个角度就容易了呢。 | I noticed that the problem gets easier from another angle. |
| `NRC-study-20` | 说到这个，今天记住了几个新单词啊。 | By the way, I remembered a few new words today. |
| `NRC-study-21` | 刚刚注意到，学到一半发现时间过得很快呢。 | Just now, time flew by while I was studying. |
| `NRC-study-22` | 这段文章读起来挺有意思。 | That article is quite interesting to read. |
| `NRC-study-23` | 刚才想起来，今天听了一节挺清楚的课呢。 | I just remembered that I had a very clear lesson today. |
| `NRC-study-24` | 说起来，刚学会一个新的快捷键啊。 | Speaking of that, I just learned a new keyboard shortcut. |
| `NRC-study-25` | 今天忽然觉得，那本书的插图画得很好。 | It suddenly occurred to me that the illustrations in that book are lovely. |
| `NRC-shopping-01` | 包裹比预计时间早到了。 | The package arrived earlier than expected. |
| `NRC-shopping-02` | 刚才想起来，刚买的水果还挺新鲜呢。 | I just remembered that the fruit I just bought looks fresh. |
| `NRC-shopping-03` | 说起来，刚才看到一双鞋挺合适啊。 | Speaking of that, I just saw a pair of shoes that looked nice. |
| `NRC-shopping-04` | 今天忽然觉得，商店里正在放一首老歌。 | It suddenly occurred to me that they were playing an old song in the store. |
| `NRC-shopping-05` | 我发现，菜市场今天人不算多呢。 | I noticed that the market was not too busy today. |
| `NRC-shopping-06` | 说到这个，买的杯子拿在手里很顺手呢。 | By the way, this cup feels nice in my hand. |
| `NRC-shopping-07` | 刚刚注意到，买东西前比了好几家价格啊。 | Just now, I compared prices at a few shops before buying. |
| `NRC-shopping-08` | 最近的快递包装很结实。 | The delivery packaging was really sturdy. |
| `NRC-shopping-09` | 刚才想起来，网购的衣服颜色比照片深一点呢。 | I just remembered that the clothes I ordered are darker than the photos. |
| `NRC-shopping-10` | 说起来，门口新开了一家小杂货铺。 | Speaking of that, a little shop opened near the entrance. |
| `NRC-shopping-11` | 今天忽然觉得，快递已经放到取件柜了啊。 | It suddenly occurred to me that the parcel is already in the pickup locker. |
| `NRC-shopping-12` | 我发现，这次买的日用品够用一阵子。 | I noticed that I bought enough household supplies for a while. |
| `NRC-shopping-13` | 说到这个，那家店最近换了新的招牌呢。 | By the way, that shop has a new sign now. |
| `NRC-shopping-14` | 刚刚注意到，收银台排队的人挺多。 | Just now, there was quite a line at the checkout. |
| `NRC-shopping-15` | 今天路过超市顺手买了牛奶呢。 | I picked up some milk at the supermarket. |
| `NRC-shopping-16` | 刚才想起来，包裹比预计时间早到了。 | I just remembered that the package arrived earlier than expected. |
| `NRC-shopping-17` | 说起来，刚买的水果还挺新鲜呢。 | Speaking of that, the fruit I just bought looks fresh. |
| `NRC-shopping-18` | 今天忽然觉得，刚才看到一双鞋挺合适。 | It suddenly occurred to me that I just saw a pair of shoes that looked nice. |
| `NRC-shopping-19` | 我发现，商店里正在放一首老歌呢。 | I noticed that they were playing an old song in the store. |
| `NRC-shopping-20` | 说到这个，菜市场今天人不算多啊。 | By the way, the market was not too busy today. |
| `NRC-shopping-21` | 刚刚注意到，买的杯子拿在手里很顺手呢。 | Just now, this cup feels nice in my hand. |
| `NRC-shopping-22` | 买东西前比了好几家价格。 | I compared prices at a few shops before buying. |
| `NRC-shopping-23` | 刚才想起来，最近的快递包装很结实呢。 | I just remembered that the delivery packaging was really sturdy. |
| `NRC-shopping-24` | 说起来，网购的衣服颜色比照片深一点啊。 | Speaking of that, the clothes I ordered are darker than the photos. |
| `NRC-shopping-25` | 今天忽然觉得，门口新开了一家小杂货铺。 | It suddenly occurred to me that a little shop opened near the entrance. |
| `NRC-sleep-01` | 枕头换了以后感觉还行。 | The new pillow feels all right. |
| `NRC-sleep-02` | 刚才想起来，最近起床没有以前那么困难呢。 | I just remembered that getting up has not been as hard lately. |
| `NRC-sleep-03` | 说起来，昨天半夜醒了一次啊。 | Speaking of that, I woke up once in the middle of the night. |
| `NRC-sleep-04` | 今天忽然觉得，睡前看书容易忘记时间。 | It suddenly occurred to me that I lose track of time when I read before bed. |
| `NRC-sleep-05` | 我发现，最近晚上容易做梦呢。 | I noticed that I have been having vivid dreams lately. |
| `NRC-sleep-06` | 说到这个，夜里外面下雨听得很清楚呢。 | By the way, I could hear the rain outside last night. |
| `NRC-sleep-07` | 刚刚注意到，午睡醒来以后精神好多了啊。 | Just now, I felt much better after my nap. |
| `NRC-sleep-08` | 周末终于睡了个懒觉。 | I finally slept in over the weekend. |
| `NRC-sleep-09` | 刚才想起来，今天早上醒得比较早呢。 | I just remembered that I woke up pretty early this morning. |
| `NRC-sleep-10` | 说起来，今天困意来得比较早。 | Speaking of that, I started feeling sleepy pretty early today. |
| `NRC-sleep-11` | 今天忽然觉得，昨晚睡得比前几天踏实啊。 | It suddenly occurred to me that I slept more soundly than I have recently. |
| `NRC-sleep-12` | 我发现，早上闹钟响之前就醒了。 | I noticed that I woke up before the alarm this morning. |
| `NRC-sleep-13` | 说到这个，昨晚梦见小时候住的地方呢。 | By the way, I dreamed about where I lived as a child. |
| `NRC-sleep-14` | 刚刚注意到，这几天作息有点不规律。 | Just now, my sleep schedule has been a bit irregular. |
| `NRC-sleep-15` | 下午喝了咖啡晚上得晚点睡呢。 | I had coffee this afternoon, so I may sleep later. |
| `NRC-sleep-16` | 刚才想起来，枕头换了以后感觉还行。 | I just remembered that the new pillow feels all right. |
| `NRC-sleep-17` | 说起来，最近起床没有以前那么困难呢。 | Speaking of that, getting up has not been as hard lately. |
| `NRC-sleep-18` | 今天忽然觉得，昨天半夜醒了一次。 | It suddenly occurred to me that I woke up once in the middle of the night. |
| `NRC-sleep-19` | 我发现，睡前看书容易忘记时间呢。 | I noticed that I lose track of time when I read before bed. |
| `NRC-sleep-20` | 说到这个，最近晚上容易做梦啊。 | By the way, I have been having vivid dreams lately. |
| `NRC-sleep-21` | 刚刚注意到，夜里外面下雨听得很清楚呢。 | Just now, I could hear the rain outside last night. |
| `NRC-sleep-22` | 午睡醒来以后精神好多了。 | I felt much better after my nap. |
| `NRC-sleep-23` | 刚才想起来，周末终于睡了个懒觉呢。 | I just remembered that I finally slept in over the weekend. |
| `NRC-sleep-24` | 说起来，今天早上醒得比较早啊。 | Speaking of that, I woke up pretty early this morning. |
| `NRC-sleep-25` | 今天忽然觉得，今天困意来得比较早。 | It suddenly occurred to me that I started feeling sleepy pretty early today. |
| `NRC-hobby-01` | 空闲时会翻翻旅行照片。 | I like looking through old travel photos in my spare time. |
| `NRC-hobby-02` | 刚才想起来，有空想去看看新的展览呢。 | I just remembered that I would like to visit a new exhibition sometime. |
| `NRC-hobby-03` | 说起来，最近迷上了做手冲咖啡啊。 | Speaking of that, I am getting into making pour-over coffee. |
| `NRC-hobby-04` | 今天忽然觉得，最近又把老游戏打开了。 | It suddenly occurred to me that I have started up an old game again. |
| `NRC-hobby-05` | 我发现，这两天在练习画画呢。 | I noticed that I have been practicing drawing lately. |
| `NRC-hobby-06` | 说到这个，我挺喜欢下雨天听音乐呢。 | By the way, I like listening to music on rainy days. |
| `NRC-hobby-07` | 刚刚注意到，刚拼完一个小模型啊。 | Just now, I just finished building a small model. |
| `NRC-hobby-08` | 刚看完一本轻松的小说。 | I just finished a lighthearted novel. |
| `NRC-hobby-09` | 刚才想起来，周末看了几集纪录片呢。 | I just remembered that I watched a few episodes of a documentary this weekend. |
| `NRC-hobby-10` | 说起来，最近开始学着种花。 | Speaking of that, I have started growing flowers lately. |
| `NRC-hobby-11` | 今天忽然觉得，最近又开始听老歌了啊。 | It suddenly occurred to me that I have started listening to old songs again. |
| `NRC-hobby-12` | 我发现，正在整理以前拍的照片。 | I noticed that I am sorting through the photos I took. |
| `NRC-hobby-13` | 说到这个，最近在试着做不同口味的面包呢。 | By the way, I am trying out different kinds of bread. |
| `NRC-hobby-14` | 刚刚注意到，昨天看了一场精彩的球赛。 | Just now, I watched an exciting game yesterday. |
| `NRC-hobby-15` | 今天随手拍的云很好看呢。 | The clouds I photographed today looked lovely. |
| `NRC-hobby-16` | 刚才想起来，空闲时会翻翻旅行照片。 | I just remembered that I like looking through old travel photos in my spare time. |
| `NRC-hobby-17` | 说起来，有空想去看看新的展览呢。 | Speaking of that, I would like to visit a new exhibition sometime. |
| `NRC-hobby-18` | 今天忽然觉得，最近迷上了做手冲咖啡。 | It suddenly occurred to me that I am getting into making pour-over coffee. |
| `NRC-hobby-19` | 我发现，最近又把老游戏打开了呢。 | I noticed that I have started up an old game again. |
| `NRC-hobby-20` | 说到这个，这两天在练习画画啊。 | By the way, I have been practicing drawing lately. |
| `NRC-hobby-21` | 刚刚注意到，我挺喜欢下雨天听音乐呢。 | Just now, I like listening to music on rainy days. |
| `NRC-hobby-22` | 刚拼完一个小模型。 | I just finished building a small model. |
| `NRC-hobby-23` | 刚才想起来，刚看完一本轻松的小说呢。 | I just remembered that I just finished a lighthearted novel. |
| `NRC-hobby-24` | 说起来，周末看了几集纪录片啊。 | Speaking of that, I watched a few episodes of a documentary this weekend. |
| `NRC-hobby-25` | 今天忽然觉得，最近开始学着种花。 | It suddenly occurred to me that I have started growing flowers lately. |
| `NRC-family-01` | 亲戚刚刚分享了一道菜谱。 | A relative just shared a recipe. |
| `NRC-family-02` | 刚才想起来，家里聚餐的时候特别热闹呢。 | I just remembered that family dinner was especially lively. |
| `NRC-family-03` | 说起来，家里群里聊了好多旧照片啊。 | Speaking of that, our family chat was full of old photos. |
| `NRC-family-04` | 今天忽然觉得，表哥最近搬到了新地方。 | It suddenly occurred to me that my cousin recently moved somewhere new. |
| `NRC-family-05` | 我发现，姐姐发来一张旅行照片呢。 | I noticed that my sister sent me a travel photo. |
| `NRC-family-06` | 说到这个，奶奶讲了一个小时候的故事呢。 | By the way, grandma told a story from when she was young. |
| `NRC-family-07` | 刚刚注意到，孩子今天讲了学校里的趣事啊。 | Just now, the kid told me a funny story from school. |
| `NRC-family-08` | 家里人记得我喜欢吃什么。 | My family remembers what I like to eat. |
| `NRC-family-09` | 刚才想起来，爸妈最近身体都挺好呢。 | I just remembered that my parents have been doing well lately. |
| `NRC-family-10` | 说起来，今晚大家难得都有空。 | Speaking of that, everyone happens to be free tonight. |
| `NRC-family-11` | 今天忽然觉得，周末准备和家人一起吃饭啊。 | It suddenly occurred to me that I am going to have a meal with my family this weekend. |
| `NRC-family-12` | 我发现，弟弟终于完成了手头的考试。 | I noticed that my younger brother finally finished his exam. |
| `NRC-family-13` | 说到这个，家里人今天打来一个电话呢。 | By the way, someone in my family called today. |
| `NRC-family-14` | 刚刚注意到，家人说最近院子里的花开了。 | Just now, my family said the flowers in the yard are blooming. |
| `NRC-family-15` | 妈妈说窗台上的植物长高了呢。 | Mom said the plant by the window has grown taller. |
| `NRC-family-16` | 刚才想起来，亲戚刚刚分享了一道菜谱。 | I just remembered that a relative just shared a recipe. |
| `NRC-family-17` | 说起来，家里聚餐的时候特别热闹呢。 | Speaking of that, family dinner was especially lively. |
| `NRC-family-18` | 今天忽然觉得，家里群里聊了好多旧照片。 | It suddenly occurred to me that our family chat was full of old photos. |
| `NRC-family-19` | 我发现，表哥最近搬到了新地方呢。 | I noticed that my cousin recently moved somewhere new. |
| `NRC-family-20` | 说到这个，姐姐发来一张旅行照片啊。 | By the way, my sister sent me a travel photo. |
| `NRC-family-21` | 刚刚注意到，奶奶讲了一个小时候的故事呢。 | Just now, grandma told a story from when she was young. |
| `NRC-family-22` | 孩子今天讲了学校里的趣事。 | The kid told me a funny story from school. |
| `NRC-family-23` | 刚才想起来，家里人记得我喜欢吃什么呢。 | I just remembered that my family remembers what I like to eat. |
| `NRC-family-24` | 说起来，爸妈最近身体都挺好啊。 | Speaking of that, my parents have been doing well lately. |
| `NRC-family-25` | 今天忽然觉得，今晚大家难得都有空。 | It suddenly occurred to me that everyone happens to be free tonight. |
| `NRC-entertainment-01` | 这集播客聊了不少旅行。 | That podcast talked a lot about travel. |
| `NRC-entertainment-02` | 刚才想起来，这部剧的背景音乐不错呢。 | I just remembered that the background music in this series is nice. |
| `NRC-entertainment-03` | 说起来，最近的纪录片拍得很细致啊。 | Speaking of that, the new documentary is very detailed. |
| `NRC-entertainment-04` | 今天忽然觉得，昨天的比赛最后几分钟很精彩。 | It suddenly occurred to me that the last few minutes of yesterday’s game were exciting. |
| `NRC-entertainment-05` | 我发现，刚听到一首以前常放的歌呢。 | I noticed that I just heard a song I used to play a lot. |
| `NRC-entertainment-06` | 说到这个，片尾曲听起来很熟悉呢。 | By the way, the end song sounds familiar. |
| `NRC-entertainment-07` | 刚刚注意到，这场演出的灯光设计很好啊。 | Just now, the stage lighting at the show was lovely. |
| `NRC-entertainment-08` | 最近有个综艺挺轻松。 | There is a pretty relaxing variety show lately. |
| `NRC-entertainment-09` | 刚才想起来，我还记得那部老动画呢。 | I just remembered that I still remember that old cartoon. |
| `NRC-entertainment-10` | 说起来，那个演员的台词说得很自然。 | Speaking of that, that actor delivered the lines very naturally. |
| `NRC-entertainment-11` | 今天忽然觉得，主持人讲故事挺有意思啊。 | It suddenly occurred to me that the host tells stories in an interesting way. |
| `NRC-entertainment-12` | 我发现，刚看完一部节奏很慢的电影。 | I noticed that I just watched a slow-paced movie. |
| `NRC-entertainment-13` | 说到这个，电影里的海边景色很漂亮呢。 | By the way, the seaside scenery in the movie was beautiful. |
| `NRC-entertainment-14` | 刚刚注意到，舞台上的布景看起来很精致。 | Just now, the stage set looked really polished. |
| `NRC-entertainment-15` | 刚刷到一个有趣的短片呢。 | I just came across a funny clip. |
| `NRC-entertainment-16` | 刚才想起来，这集播客聊了不少旅行。 | I just remembered that that podcast talked a lot about travel. |
| `NRC-entertainment-17` | 说起来，这部剧的背景音乐不错呢。 | Speaking of that, the background music in this series is nice. |
| `NRC-entertainment-18` | 今天忽然觉得，最近的纪录片拍得很细致。 | It suddenly occurred to me that the new documentary is very detailed. |
| `NRC-entertainment-19` | 我发现，昨天的比赛最后几分钟很精彩呢。 | I noticed that the last few minutes of yesterday’s game were exciting. |
| `NRC-entertainment-20` | 说到这个，刚听到一首以前常放的歌啊。 | By the way, I just heard a song I used to play a lot. |
| `NRC-entertainment-21` | 刚刚注意到，片尾曲听起来很熟悉呢。 | Just now, the end song sounds familiar. |
| `NRC-entertainment-22` | 这场演出的灯光设计很好。 | The stage lighting at the show was lovely. |
| `NRC-entertainment-23` | 刚才想起来，最近有个综艺挺轻松呢。 | I just remembered that there is a pretty relaxing variety show lately. |
| `NRC-entertainment-24` | 说起来，我还记得那部老动画啊。 | Speaking of that, I still remember that old cartoon. |
| `NRC-entertainment-25` | 今天忽然觉得，那个演员的台词说得很自然。 | It suddenly occurred to me that that actor delivered the lines very naturally. |
| `NRC-health-01` | 出门晒了会儿太阳。 | I spent a while outside in the sunshine. |
| `NRC-health-02` | 刚才想起来，最近晚上少喝了咖啡呢。 | I just remembered that I have been drinking less coffee at night. |
| `NRC-health-03` | 说起来，这两天嗓子有点干啊。 | Speaking of that, my throat has felt a little dry lately. |
| `NRC-health-04` | 今天忽然觉得，坐久了站起来活动一下。 | It suddenly occurred to me that I got up and moved around after sitting for a while. |
| `NRC-health-05` | 我发现，今天记得按时吃饭呢。 | I noticed that I remembered to eat on time today. |
| `NRC-health-06` | 说到这个，散步回来心情挺好呢。 | By the way, I felt good after my walk. |
| `NRC-health-07` | 刚刚注意到，午饭后散步了十分钟啊。 | Just now, I took a ten-minute walk after lunch. |
| `NRC-health-08` | 今天没有忘记带水杯。 | I remembered to bring my water bottle today. |
| `NRC-health-09` | 刚才想起来，最近肩膀有点酸呢。 | I just remembered that my shoulders have been a little sore lately. |
| `NRC-health-10` | 说起来，天气变化时鼻子容易不舒服。 | Speaking of that, my nose gets uncomfortable when the weather changes. |
| `NRC-health-11` | 今天忽然觉得，喝完水以后舒服多了啊。 | It suddenly occurred to me that I felt better after drinking some water. |
| `NRC-health-12` | 我发现，刚才伸展了一下腰背。 | I noticed that I stretched my lower back a little. |
| `NRC-health-13` | 说到这个，今天走路比昨天多一些呢。 | By the way, I walked more than I did yesterday. |
| `NRC-health-14` | 刚刚注意到，最近运动量比之前大。 | Just now, I have been exercising more than before. |
| `NRC-health-15` | 今天呼吸到外面的新鲜空气呢。 | I got some fresh air outside today. |
| `NRC-health-16` | 刚才想起来，出门晒了会儿太阳。 | I just remembered that I spent a while outside in the sunshine. |
| `NRC-health-17` | 说起来，最近晚上少喝了咖啡呢。 | Speaking of that, I have been drinking less coffee at night. |
| `NRC-health-18` | 今天忽然觉得，这两天嗓子有点干。 | It suddenly occurred to me that my throat has felt a little dry lately. |
| `NRC-health-19` | 我发现，坐久了站起来活动一下呢。 | I noticed that I got up and moved around after sitting for a while. |
| `NRC-health-20` | 说到这个，今天记得按时吃饭啊。 | By the way, I remembered to eat on time today. |
| `NRC-health-21` | 刚刚注意到，散步回来心情挺好呢。 | Just now, I felt good after my walk. |
| `NRC-health-22` | 午饭后散步了十分钟。 | I took a ten-minute walk after lunch. |
| `NRC-health-23` | 刚才想起来，今天没有忘记带水杯呢。 | I just remembered that I remembered to bring my water bottle today. |
| `NRC-health-24` | 说起来，最近肩膀有点酸啊。 | Speaking of that, my shoulders have been a little sore lately. |
| `NRC-health-25` | 今天忽然觉得，天气变化时鼻子容易不舒服。 | It suddenly occurred to me that my nose gets uncomfortable when the weather changes. |
| `NRC-friends-01` | 老同学群里今天挺热闹。 | The old classmates’ group chat is lively today. |
| `NRC-friends-02` | 刚才想起来，这次聚会大家都到齐了呢。 | I just remembered that everyone made it to the reunion this time. |
| `NRC-friends-03` | 说起来，周末可能会和大家一起吃饭啊。 | Speaking of that, we might have dinner together this weekend. |
| `NRC-friends-04` | 今天忽然觉得，昨天和邻居聊了几分钟。 | It suddenly occurred to me that I talked with a neighbor for a few minutes yesterday. |
| `NRC-friends-05` | 我发现，朋友最近换了新的工作呢。 | I noticed that my friend recently started a new job. |
| `NRC-friends-06` | 说到这个，同事下班后讲了个趣事呢。 | By the way, a coworker told me a funny story after work. |
| `NRC-friends-07` | 刚刚注意到，有人分享了一个冷笑话啊。 | Just now, someone shared a silly joke. |
| `NRC-friends-08` | 朋友给我看他拍的日落。 | My friend showed me a photo of the sunset. |
| `NRC-friends-09` | 刚才想起来，刚才聊起以前一起旅行的事呢。 | I just remembered that we reminisced about an old trip a moment ago. |
| `NRC-friends-10` | 说起来，大家约好改天去郊外走走。 | Speaking of that, everyone agreed to go for a walk in the countryside sometime. |
| `NRC-friends-11` | 今天忽然觉得，朋友推荐的餐馆还不错啊。 | It suddenly occurred to me that the restaurant my friend recommended was good. |
| `NRC-friends-12` | 我发现，刚收到一条挺暖心的消息。 | I noticed that I just got a really kind message. |
| `NRC-friends-13` | 说到这个，上周和老同学见了一面呢。 | By the way, I met up with an old classmate last week. |
| `NRC-friends-14` | 刚刚注意到，朋友说他家附近开了新店。 | Just now, my friend said a new shop opened nearby. |
| `NRC-friends-15` | 朋友刚发来一张搞笑图片呢。 | A friend just sent me a funny picture. |
| `NRC-friends-16` | 刚才想起来，老同学群里今天挺热闹。 | I just remembered that the old classmates’ group chat is lively today. |
| `NRC-friends-17` | 说起来，这次聚会大家都到齐了呢。 | Speaking of that, everyone made it to the reunion this time. |
| `NRC-friends-18` | 今天忽然觉得，周末可能会和大家一起吃饭。 | It suddenly occurred to me that we might have dinner together this weekend. |
| `NRC-friends-19` | 我发现，昨天和邻居聊了几分钟呢。 | I noticed that I talked with a neighbor for a few minutes yesterday. |
| `NRC-friends-20` | 说到这个，朋友最近换了新的工作啊。 | By the way, my friend recently started a new job. |
| `NRC-friends-21` | 刚刚注意到，同事下班后讲了个趣事呢。 | Just now, a coworker told me a funny story after work. |
| `NRC-friends-22` | 有人分享了一个冷笑话。 | Someone shared a silly joke. |
| `NRC-friends-23` | 刚才想起来，朋友给我看他拍的日落呢。 | I just remembered that my friend showed me a photo of the sunset. |
| `NRC-friends-24` | 说起来，刚才聊起以前一起旅行的事啊。 | Speaking of that, we reminisced about an old trip a moment ago. |
| `NRC-friends-25` | 今天忽然觉得，大家约好改天去郊外走走。 | It suddenly occurred to me that everyone agreed to go for a walk in the countryside sometime. |
| `NRC-travel-01` | 坐长途车时看了好久窗外。 | I watched the scenery for ages on the long-distance bus. |
| `NRC-travel-02` | 刚才想起来，旅途中遇到的人很热情呢。 | I just remembered that the people I met on the trip were very kind. |
| `NRC-travel-03` | 说起来，山上的空气闻起来很清新啊。 | Speaking of that, the mountain air smelled so fresh. |
| `NRC-travel-04` | 今天忽然觉得，海边的日出比想象中早。 | It suddenly occurred to me that sunrise by the sea came earlier than I expected. |
| `NRC-travel-05` | 我发现，旅行时拍的照片还没整理呢。 | I noticed that I have not sorted through my travel photos yet. |
| `NRC-travel-06` | 说到这个，那座古城晚上特别热闹呢。 | By the way, the old town was lively at night. |
| `NRC-travel-07` | 刚刚注意到，行李箱的轮子有点卡啊。 | Just now, one wheel on my suitcase is sticking. |
| `NRC-travel-08` | 车票已经放进手机里了。 | I saved the ticket on my phone. |
| `NRC-travel-09` | 刚才想起来，那趟火车一路上挺平稳呢。 | I just remembered that the train ride was pretty smooth. |
| `NRC-travel-10` | 说起来，沿路有一片很大的稻田。 | Speaking of that, there was a huge rice field along the way. |
| `NRC-travel-11` | 今天忽然觉得，火车站附近的小店挺多啊。 | It suddenly occurred to me that there are lots of little shops near the train station. |
| `NRC-travel-12` | 我发现，我还记得第一次坐飞机。 | I noticed that I still remember my first flight. |
| `NRC-travel-13` | 说到这个，上次去海边的风景很漂亮呢。 | By the way, the coast looked beautiful on my last trip. |
| `NRC-travel-14` | 刚刚注意到，那家民宿的院子很安静。 | Just now, the guesthouse had a very quiet courtyard. |
| `NRC-travel-15` | 这次出门带的东西刚刚好呢。 | I brought just the right amount of stuff this time. |
| `NRC-travel-16` | 刚才想起来，坐长途车时看了好久窗外。 | I just remembered that I watched the scenery for ages on the long-distance bus. |
| `NRC-travel-17` | 说起来，旅途中遇到的人很热情呢。 | Speaking of that, the people I met on the trip were very kind. |
| `NRC-travel-18` | 今天忽然觉得，山上的空气闻起来很清新。 | It suddenly occurred to me that the mountain air smelled so fresh. |
| `NRC-travel-19` | 我发现，海边的日出比想象中早呢。 | I noticed that sunrise by the sea came earlier than I expected. |
| `NRC-travel-20` | 说到这个，旅行时拍的照片还没整理啊。 | By the way, I have not sorted through my travel photos yet. |
| `NRC-travel-21` | 刚刚注意到，那座古城晚上特别热闹呢。 | Just now, the old town was lively at night. |
| `NRC-travel-22` | 行李箱的轮子有点卡。 | One wheel on my suitcase is sticking. |
| `NRC-travel-23` | 刚才想起来，车票已经放进手机里了呢。 | I just remembered that I saved the ticket on my phone. |
| `NRC-travel-24` | 说起来，那趟火车一路上挺平稳啊。 | Speaking of that, the train ride was pretty smooth. |
| `NRC-travel-25` | 今天忽然觉得，沿路有一片很大的稻田。 | It suddenly occurred to me that there was a huge rice field along the way. |
| `NRC-plants-01` | 今天看到路边有棵樱花树。 | I saw a cherry tree by the road today. |
| `NRC-plants-02` | 刚才想起来，盆栽旁边冒出来一棵小草呢。 | I just remembered that a little weed has sprouted next to the pot. |
| `NRC-plants-03` | 说起来，这盆多肉比上个月胖了一圈啊。 | Speaking of that, that succulent has gotten plumper this month. |
| `NRC-plants-04` | 今天忽然觉得，雨后花园里有股泥土味。 | It suddenly occurred to me that the garden smells earthy after the rain. |
| `NRC-plants-05` | 我发现，楼下的树叶变黄了呢。 | I noticed that the leaves on the trees downstairs are turning yellow. |
| `NRC-plants-06` | 说到这个，窗边那片叶子颜色很鲜亮呢。 | By the way, that leaf by the window has a vivid color. |
| `NRC-plants-07` | 刚刚注意到，花盆里的土看起来有点干啊。 | Just now, the soil in that pot looks a little dry. |
| `NRC-plants-08` | 院子里几株月季开花了。 | A few roses in the yard are blooming. |
| `NRC-plants-09` | 刚才想起来，最近绿萝长得挺快呢。 | I just remembered that the pothos has been growing quickly. |
| `NRC-plants-10` | 说起来，桌上的水培植物长了根。 | Speaking of that, the hydroponic plant on the desk has grown roots. |
| `NRC-plants-11` | 今天忽然觉得，阳台那盆花开了一朵啊。 | It suddenly occurred to me that a flower just opened on the balcony. |
| `NRC-plants-12` | 我发现，小区里的桂花香味很浓。 | I noticed that the osmanthus near the building smells lovely. |
| `NRC-plants-13` | 说到这个，窗台上的薄荷长出新叶了呢。 | By the way, the mint by the window has new leaves. |
| `NRC-plants-14` | 刚刚注意到，新买的植物还在适应环境。 | Just now, the new plant is still getting used to its spot. |
| `NRC-plants-15` | 这株植物的名字我还不太熟呢。 | I am not very familiar with that plant’s name. |
| `NRC-plants-16` | 刚才想起来，今天看到路边有棵樱花树。 | I just remembered that I saw a cherry tree by the road today. |
| `NRC-plants-17` | 说起来，盆栽旁边冒出来一棵小草呢。 | Speaking of that, a little weed has sprouted next to the pot. |
| `NRC-plants-18` | 今天忽然觉得，这盆多肉比上个月胖了一圈。 | It suddenly occurred to me that that succulent has gotten plumper this month. |
| `NRC-plants-19` | 我发现，雨后花园里有股泥土味呢。 | I noticed that the garden smells earthy after the rain. |
| `NRC-plants-20` | 说到这个，楼下的树叶变黄了啊。 | By the way, the leaves on the trees downstairs are turning yellow. |
| `NRC-plants-21` | 刚刚注意到，窗边那片叶子颜色很鲜亮呢。 | Just now, that leaf by the window has a vivid color. |
| `NRC-plants-22` | 花盆里的土看起来有点干。 | The soil in that pot looks a little dry. |
| `NRC-plants-23` | 刚才想起来，院子里几株月季开花了呢。 | I just remembered that a few roses in the yard are blooming. |
| `NRC-plants-24` | 说起来，最近绿萝长得挺快啊。 | Speaking of that, the pothos has been growing quickly. |
| `NRC-plants-25` | 今天忽然觉得，桌上的水培植物长了根。 | It suddenly occurred to me that the hydroponic plant on the desk has grown roots. |
| `NRC-hobbies_sports-01` | 活动完出了一身汗。 | I worked up a sweat today. |
| `NRC-hobbies_sports-02` | 刚才想起来，比赛最后的比分很接近呢。 | I just remembered that the final score was really close. |
| `NRC-hobbies_sports-03` | 说起来，晨练时遇见了熟悉的邻居啊。 | Speaking of that, I ran into a familiar neighbor during my morning walk. |
| `NRC-hobbies_sports-04` | 今天忽然觉得，这周运动了三次。 | It suddenly occurred to me that I worked out three times this week. |
| `NRC-hobbies_sports-05` | 我发现，球拍的手胶该换新的了呢。 | I noticed that the grip on my racket needs replacing. |
| `NRC-hobbies_sports-06` | 说到这个，骑车经过了河边那条路呢。 | By the way, I rode my bike along the river. |
| `NRC-hobbies_sports-07` | 刚刚注意到，那支球队的配合挺默契啊。 | Just now, that team worked really well together. |
| `NRC-hobbies_sports-08` | 最近打球以后睡得更香。 | I sleep better after playing sports lately. |
| `NRC-hobbies_sports-09` | 刚才想起来，今天拉伸的时候感觉轻松多了呢。 | I just remembered that stretching felt much easier today. |
| `NRC-hobbies_sports-10` | 说起来，球场上今天人挺多。 | Speaking of that, there were lots of people on the court today. |
| `NRC-hobbies_sports-11` | 今天忽然觉得，散步时绕着公园走了两圈啊。 | It suddenly occurred to me that I walked two laps around the park. |
| `NRC-hobbies_sports-12` | 我发现，慢跑时发现路边新修了步道。 | I noticed that I noticed a new path while jogging. |
| `NRC-hobbies_sports-13` | 说到这个，最近在学一个新的游泳动作呢。 | By the way, I am learning a new swimming stroke. |
| `NRC-hobbies_sports-14` | 刚刚注意到，今天看了一会儿羽毛球比赛。 | Just now, I watched some badminton today. |
| `NRC-hobbies_sports-15` | 跑步时听到了鸟叫声呢。 | I heard birds while running. |
| `NRC-hobbies_sports-16` | 刚才想起来，活动完出了一身汗。 | I just remembered that I worked up a sweat today. |
| `NRC-hobbies_sports-17` | 说起来，比赛最后的比分很接近呢。 | Speaking of that, the final score was really close. |
| `NRC-hobbies_sports-18` | 今天忽然觉得，晨练时遇见了熟悉的邻居。 | It suddenly occurred to me that I ran into a familiar neighbor during my morning walk. |
| `NRC-hobbies_sports-19` | 我发现，这周运动了三次呢。 | I noticed that I worked out three times this week. |
| `NRC-hobbies_sports-20` | 说到这个，球拍的手胶该换新的了啊。 | By the way, the grip on my racket needs replacing. |
| `NRC-hobbies_sports-21` | 刚刚注意到，骑车经过了河边那条路呢。 | Just now, I rode my bike along the river. |
| `NRC-hobbies_sports-22` | 那支球队的配合挺默契。 | That team worked really well together. |
| `NRC-hobbies_sports-23` | 刚才想起来，最近打球以后睡得更香呢。 | I just remembered that I sleep better after playing sports lately. |
| `NRC-hobbies_sports-24` | 说起来，今天拉伸的时候感觉轻松多了啊。 | Speaking of that, stretching felt much easier today. |
| `NRC-hobbies_sports-25` | 今天忽然觉得，球场上今天人挺多。 | It suddenly occurred to me that there were lots of people on the court today. |
| `NRC-home_life-01` | 地板上有一道夕阳。 | There is a stripe of sunlight on the floor. |
| `NRC-home_life-02` | 刚才想起来，沙发靠垫换了个位置呢。 | I just remembered that I moved the sofa cushions around. |
| `NRC-home_life-03` | 说起来，今天把旧杂志分类收好了啊。 | Speaking of that, I sorted through some old magazines today. |
| `NRC-home_life-04` | 今天忽然觉得，桌上的书终于摆整齐了。 | It suddenly occurred to me that the books on the table are finally tidy. |
| `NRC-home_life-05` | 我发现，水杯放在熟悉的位置呢。 | I noticed that my cup is back in its usual spot. |
| `NRC-home_life-06` | 说到这个，家里的小钟走得挺准呢。 | By the way, the little clock at home keeps good time. |
| `NRC-home_life-07` | 刚刚注意到，玄关多了一双鞋啊。 | Just now, there is a new pair of shoes in the entryway. |
| `NRC-home_life-08` | 晾好的衣服已经干了。 | The laundry I hung up is dry now. |
| `NRC-home_life-09` | 刚才想起来，刚换的床单摸起来很软呢。 | I just remembered that the new bedsheets feel really soft. |
| `NRC-home_life-10` | 说起来，刚才听到楼道有人走过。 | Speaking of that, I just heard someone walk down the hall. |
| `NRC-home_life-11` | 今天忽然觉得，屋里今天特别安静啊。 | It suddenly occurred to me that it is especially quiet indoors today. |
| `NRC-home_life-12` | 我发现，房间里的空气闻起来很清爽。 | I noticed that the room smells fresh today. |
| `NRC-home_life-13` | 说到这个，窗外的鸟停在栏杆上呢。 | By the way, a bird landed on the railing outside. |
| `NRC-home_life-14` | 刚刚注意到，餐桌上摆着一盘水果。 | Just now, a plate of fruit is on the dining table. |
| `NRC-home_life-15` | 厨房里飘着刚煮好的汤香呢。 | Dinner cooking in the kitchen smells wonderful. |
| `NRC-home_life-16` | 刚才想起来，地板上有一道夕阳。 | I just remembered that there is a stripe of sunlight on the floor. |
| `NRC-home_life-17` | 说起来，沙发靠垫换了个位置呢。 | Speaking of that, I moved the sofa cushions around. |
| `NRC-home_life-18` | 今天忽然觉得，今天把旧杂志分类收好了。 | It suddenly occurred to me that I sorted through some old magazines today. |
| `NRC-home_life-19` | 我发现，桌上的书终于摆整齐了呢。 | I noticed that the books on the table are finally tidy. |
| `NRC-home_life-20` | 说到这个，水杯放在熟悉的位置啊。 | By the way, my cup is back in its usual spot. |
| `NRC-home_life-21` | 刚刚注意到，家里的小钟走得挺准呢。 | Just now, the little clock at home keeps good time. |
| `NRC-home_life-22` | 玄关多了一双鞋。 | There is a new pair of shoes in the entryway. |
| `NRC-home_life-23` | 刚才想起来，晾好的衣服已经干了呢。 | I just remembered that the laundry I hung up is dry now. |
| `NRC-home_life-24` | 说起来，刚换的床单摸起来很软啊。 | Speaking of that, the new bedsheets feel really soft. |
| `NRC-home_life-25` | 今天忽然觉得，刚才听到楼道有人走过。 | It suddenly occurred to me that I just heard someone walk down the hall. |
| `NRC-opinions-01` | 有些老东西用起来更顺手。 | Some old things are simply easier to use. |
| `NRC-opinions-02` | 刚才想起来，我觉得慢一点也没关系呢。 | I just remembered that I think it is fine to take things slowly. |
| `NRC-opinions-03` | 说起来，计划留一点空白比较自在啊。 | Speaking of that, leaving some room in the schedule feels freeing. |
| `NRC-opinions-04` | 今天忽然觉得，慢慢整理思路会清楚一些。 | It suddenly occurred to me that taking time to sort out your thoughts helps. |
| `NRC-opinions-05` | 我发现，最近越来越喜欢安静的早晨呢。 | I noticed that I have been enjoying quiet mornings more and more. |
| `NRC-opinions-06` | 说到这个，天气好的时候心情也会变好呢。 | By the way, good weather can lift your mood. |
| `NRC-opinions-07` | 刚刚注意到，小事做好了心情会轻松啊。 | Just now, taking care of small things can lift your mood. |
| `NRC-opinions-08` | 生活里有很多细小的快乐。 | There are lots of small joys in everyday life. |
| `NRC-opinions-09` | 刚才想起来，偶尔发发呆也挺好的呢。 | I just remembered that it is nice to let your mind wander sometimes. |
| `NRC-opinions-10` | 说起来，听别人讲经历也挺有意思。 | Speaking of that, it is interesting to hear about other people’s experiences. |
| `NRC-opinions-11` | 今天忽然觉得，熟悉的地方总让人安心啊。 | It suddenly occurred to me that familiar places always feel comforting. |
| `NRC-opinions-12` | 我发现，适合自己的节奏最重要。 | I noticed that finding a pace that suits you matters most. |
| `NRC-opinions-13` | 说到这个，每个人喜欢的颜色都不一样呢。 | By the way, everyone likes different colors. |
| `NRC-opinions-14` | 刚刚注意到，新鲜的体验总会带来惊喜。 | Just now, new experiences always bring a little surprise. |
| `NRC-opinions-15` | 有时候简单一点反而舒服呢。 | Sometimes keeping things simple feels better. |
| `NRC-opinions-16` | 刚才想起来，有些老东西用起来更顺手。 | I just remembered that some old things are simply easier to use. |
| `NRC-opinions-17` | 说起来，我觉得慢一点也没关系呢。 | Speaking of that, I think it is fine to take things slowly. |
| `NRC-opinions-18` | 今天忽然觉得，计划留一点空白比较自在。 | It suddenly occurred to me that leaving some room in the schedule feels freeing. |
| `NRC-opinions-19` | 我发现，慢慢整理思路会清楚一些呢。 | I noticed that taking time to sort out your thoughts helps. |
| `NRC-opinions-20` | 说到这个，最近越来越喜欢安静的早晨啊。 | By the way, I have been enjoying quiet mornings more and more. |
| `NRC-opinions-21` | 刚刚注意到，天气好的时候心情也会变好呢。 | Just now, good weather can lift your mood. |
| `NRC-opinions-22` | 小事做好了心情会轻松。 | Taking care of small things can lift your mood. |
| `NRC-opinions-23` | 刚才想起来，生活里有很多细小的快乐呢。 | I just remembered that there are lots of small joys in everyday life. |
| `NRC-opinions-24` | 说起来，偶尔发发呆也挺好的啊。 | Speaking of that, it is nice to let your mind wander sometimes. |
| `NRC-opinions-25` | 今天忽然觉得，听别人讲经历也挺有意思。 | It suddenly occurred to me that it is interesting to hear about other people’s experiences. |
| `NRC-memories-01` | 以前的课桌上刻过几个字。 | I carved a few words into my old desk. |
| `NRC-memories-02` | 刚才想起来，想起小时候放学回家的路呢。 | I just remembered that I remember the walk home from school as a kid. |
| `NRC-memories-03` | 说起来，那时候邻居家的小狗总爱叫啊。 | Speaking of that, the neighbor’s dog used to bark all the time. |
| `NRC-memories-04` | 今天忽然觉得，突然想起很久没见的同学。 | It suddenly occurred to me that I suddenly remembered a classmate I have not seen in years. |
| `NRC-memories-05` | 我发现，记得第一次自己坐公交车呢。 | I noticed that I remember taking the bus by myself for the first time. |
| `NRC-memories-06` | 说到这个，那年生日大家都来了呢。 | By the way, everyone came to that birthday party. |
| `NRC-memories-07` | 刚刚注意到，以前住的地方离学校很近啊。 | Just now, the place I used to live was close to school. |
| `NRC-memories-08` | 以前放学经常绕路去买糖。 | I used to take a detour to buy candy after school. |
| `NRC-memories-09` | 刚才想起来，小时候最喜欢吃外婆做的点心呢。 | I just remembered that grandma’s pastries were my favorite as a child. |
| `NRC-memories-10` | 说起来，老房子门前有一棵大树。 | Speaking of that, there was a big tree in front of the old house. |
| `NRC-memories-11` | 今天忽然觉得，有一年冬天雪下得特别厚啊。 | It suddenly occurred to me that one winter, the snow came down really thick. |
| `NRC-memories-12` | 我发现，想起一次很早很早的旅行。 | I noticed that I just remembered a trip from a long time ago. |
| `NRC-memories-13` | 说到这个，老照片里的那条街变化很大呢。 | By the way, that street in the old photos has changed so much. |
| `NRC-memories-14` | 刚刚注意到，小时候的暑假好像特别长。 | Just now, summer vacation felt so long back then. |
| `NRC-memories-15` | 以前夏天总在院子里乘凉呢。 | We used to sit in the courtyard on summer evenings. |
| `NRC-memories-16` | 刚才想起来，以前的课桌上刻过几个字。 | I just remembered that I carved a few words into my old desk. |
| `NRC-memories-17` | 说起来，想起小时候放学回家的路呢。 | Speaking of that, I remember the walk home from school as a kid. |
| `NRC-memories-18` | 今天忽然觉得，那时候邻居家的小狗总爱叫。 | It suddenly occurred to me that the neighbor’s dog used to bark all the time. |
| `NRC-memories-19` | 我发现，突然想起很久没见的同学呢。 | I noticed that I suddenly remembered a classmate I have not seen in years. |
| `NRC-memories-20` | 说到这个，记得第一次自己坐公交车啊。 | By the way, I remember taking the bus by myself for the first time. |
| `NRC-memories-21` | 刚刚注意到，那年生日大家都来了呢。 | Just now, everyone came to that birthday party. |
| `NRC-memories-22` | 以前住的地方离学校很近。 | The place I used to live was close to school. |
| `NRC-memories-23` | 刚才想起来，以前放学经常绕路去买糖呢。 | I just remembered that I used to take a detour to buy candy after school. |
| `NRC-memories-24` | 说起来，小时候最喜欢吃外婆做的点心啊。 | Speaking of that, grandma’s pastries were my favorite as a child. |
| `NRC-memories-25` | 今天忽然觉得，老房子门前有一棵大树。 | It suddenly occurred to me that there was a big tree in front of the old house. |
| `NRC-weekend-01` | 假期还有几天才到。 | There are still a few days until the holiday. |
| `NRC-weekend-02` | 刚才想起来，这个星期终于能慢下来一点呢。 | I just remembered that I can finally slow down a little this week. |
| `NRC-weekend-03` | 说起来，周末的天气看起来不错啊。 | Speaking of that, the weather looks good this weekend. |
| `NRC-weekend-04` | 今天忽然觉得，休息的时候不想把时间排太满。 | It suddenly occurred to me that I do not want to pack my day off too tightly. |
| `NRC-weekend-05` | 我发现，周日准备在家做顿饭呢。 | I noticed that I am planning to cook at home on Sunday. |
| `NRC-weekend-06` | 说到这个，周末附近有个市集呢。 | By the way, there is a market nearby this weekend. |
| `NRC-weekend-07` | 刚刚注意到，周六可能会去看看新开的书店啊。 | Just now, I might check out the new bookstore on Saturday. |
| `NRC-weekend-08` | 最近特别期待一个完整的休息日。 | I am really looking forward to a full day off. |
| `NRC-weekend-09` | 刚才想起来，休息日过得比工作日快呢。 | I just remembered that days off seem to go by faster than workdays. |
| `NRC-weekend-10` | 说起来，想把空闲时间留给自己。 | Speaking of that, I want to leave some free time for myself. |
| `NRC-weekend-11` | 今天忽然觉得，这周末想睡到自然醒啊。 | It suddenly occurred to me that I want to sleep in this weekend. |
| `NRC-weekend-12` | 我发现，周末或许和朋友喝杯咖啡。 | I noticed that maybe I will get coffee with a friend this weekend. |
| `NRC-weekend-13` | 说到这个，打算找个时间去公园走走呢。 | By the way, I might find some time to walk in the park. |
| `NRC-weekend-14` | 刚刚注意到，这次休息想整理一下照片。 | Just now, I want to sort through my photos on my next day off. |
| `NRC-weekend-15` | 周末暂时没有安排太多事情呢。 | I do not have much planned for the weekend. |
| `NRC-weekend-16` | 刚才想起来，假期还有几天才到。 | I just remembered that there are still a few days until the holiday. |
| `NRC-weekend-17` | 说起来，这个星期终于能慢下来一点呢。 | Speaking of that, I can finally slow down a little this week. |
| `NRC-weekend-18` | 今天忽然觉得，周末的天气看起来不错。 | It suddenly occurred to me that the weather looks good this weekend. |
| `NRC-weekend-19` | 我发现，休息的时候不想把时间排太满呢。 | I noticed that I do not want to pack my day off too tightly. |
| `NRC-weekend-20` | 说到这个，周日准备在家做顿饭啊。 | By the way, I am planning to cook at home on Sunday. |
| `NRC-weekend-21` | 刚刚注意到，周末附近有个市集呢。 | Just now, there is a market nearby this weekend. |
| `NRC-weekend-22` | 周六可能会去看看新开的书店。 | I might check out the new bookstore on Saturday. |
| `NRC-weekend-23` | 刚才想起来，最近特别期待一个完整的休息日呢。 | I just remembered that I am really looking forward to a full day off. |
| `NRC-weekend-24` | 说起来，休息日过得比工作日快啊。 | Speaking of that, days off seem to go by faster than workdays. |
| `NRC-weekend-25` | 今天忽然觉得，想把空闲时间留给自己。 | It suddenly occurred to me that I want to leave some free time for myself. |
