"""生成基于截图的 87 条目模拟设备与语音测试规范，不修改运行时目录。"""
from pathlib import Path
from collections import Counter, defaultdict
import json

ROOT = Path(__file__).resolve().parents[1]
ROOMS = {'living':'客厅','master':'主卧','secondary':'次卧','kitchen':'厨房','toilet':'厕所','entry':'门厅','balcony':'阳台','utility':'小工具','dining':'餐厅','garden':'菜地'}
# 名称保持截图可见内容；截断名称不自动补全型号。最后一列为本次模拟的明确称呼。
RAW = '''living|ac|米家新风空调立式（3…）|客厅空调
living|light|米家吸顶灯|客厅吸顶灯
living|switch3|小米智能开关Pro（三…）|客厅三键开关
living|switch2|小米智能开关Pro（双…）|客厅双键开关
living|switch6|PTX 智能六键开关|客厅六键开关
living|socket|落地插座|客厅落地插座
living|socket|除臭风扇2|客厅除臭风扇插座
living|strip|插排|客厅插排
living|curtain|Ai智能窗帘电机|客厅窗帘
living|speaker|Xiaomi Sound 2 Max|客厅音箱
living|purifier|米家空气净化器 5S 内…|客厅空气净化器
living|feeder|叮零智能宠物喂食器|客厅喂食器
living|washer|米家无线洗地机4 Ma…|客厅洗地机
living|vacuum|米家无线吸尘器3 基…|客厅吸尘器
living|camera|小米智能摄像机 5 Pro…|客厅摄像机
living|thermo|小米电子温湿度计|客厅电子温湿度计
living|thermo|温湿度传感器2|客厅温湿度传感器2
living|remote|电视开关|客厅电视遥控开关
living|router|Want3_2.4G|客厅路由器
living|gateway|小米智能多模网关2|客厅多模网关
living|gateway|易来网关|客厅易来网关
living|gateway|Xiaomi 中枢网关|客厅中枢网关
living|aroma|米家智能调香机 3|客厅香薰机
living|fan|黑色电风扇|客厅黑色风扇
living|audio|Xiaomi 无线音频连接器|客厅音频连接器
master|ac|米家新风空调（尊享…）|主卧空调
master|light|米家显示器挂灯2|主卧显示器挂灯
master|light|主卧吸顶灯|主卧吸顶灯
master|switch2|小米智能开关2（双开）|主卧双键开关
master|humidifier|米家净化加湿器3 Pro|主卧净化加湿器
master|humidifier|米家无雾加湿器3 600…|主卧无雾加湿器
master|motion|人体传感器2|主卧人体传感器
master|pressure|领普压力有无传感器|主卧压力传感器
master|remote|挂灯|主卧挂灯遥控开关
master|speaker|小米AI音箱|主卧AI音箱
master|speaker|小爱音箱触屏版|主卧触屏音箱
master|curtain|窗帘|主卧窗帘
master|fan|米家智能直流变频循…|主卧循环风扇
master|blanket|绘睡水暖垫HS2205|主卧水暖垫
secondary|ac|米家新风空调（尊享…）|次卧空调
secondary|fan|白色电风扇|次卧白色风扇
secondary|light|米家吸顶灯Pro 超薄…|次卧吸顶灯
secondary|switch2|小米智能开关Pro（双…）|次卧双键开关
secondary|speaker|Xiaomi 智能音箱|次卧音箱
secondary|camera|小米智能摄像机 4C 3…|次卧4C摄像机
secondary|camera|小米智能摄像机3 Pro…|次卧3Pro摄像机
secondary|thermo|宝宝温度|次卧宝宝温度计
secondary|thermo|宝宝湿度|次卧宝宝湿度计
secondary|remote|次卧灯开关|次卧灯遥控开关
secondary|remote|小米智能无线开关（…）|次卧小米无线开关
secondary|humidifier|米家纯净式智能加湿…|次卧加湿器
kitchen|curtain|米家智能窗帘2|厨房窗帘
kitchen|switch2|小米智能开关（双开…）|厨房双键开关
kitchen|presence|子擎存在传感器 Lite|厨房存在传感器
kitchen|light|米家筒灯3 Pro-2|厨房2号筒灯
kitchen|light|米家筒灯3 Pro-4|厨房4号筒灯
kitchen|light|米家筒灯3 Pro 人在感…|厨房感应筒灯
kitchen|light|米家射灯3 Pro-3|厨房3号射灯
kitchen|light_group|灯组|厨房基础灯组
kitchen|light_group|米家筒灯3pro|厨房完整灯组
kitchen|smoke|小米烟感卫士2|厨房烟雾传感器
kitchen|microwave|米家微波炉|厨房微波炉
kitchen|robot|米家扫拖机器人 5 Pro|厨房扫地机器人
toilet|bath|Yeelight 智能浴霸 S20|厕所浴霸
toilet|speaker|小米小爱音箱Play 增…|厕所音箱
toilet|socket|热水|厕所热水插座
toilet|switch1|厕所灯控|厕所灯控开关
toilet|presence|领普人体存在传感器3…|厕所存在传感器
toilet|remote|无线开关|厕所无线开关
toilet|light|青空灯|厕所青空灯
entry|panel|小米智能中控屏|门厅中控屏
entry|switch1|视频|门厅视频开关
entry|switch1|解锁|门厅解锁开关
entry|lock|小米智能门锁 5 Max|门厅门锁
entry|remote|无线开关2|门厅无线开关2
entry|doorbell|小米智能猫眼2 2|门厅猫眼2号
entry|doorbell|小米智能猫眼2|门厅猫眼1号
entry|doorbell|小米智能门铃 4|门厅门铃4
balcony|rack|米家智能晾衣机|阳台晾衣架
balcony|panel|小米智能中控屏 Max 2|阳台中控屏
utility|battery|小米充电宝 Pro 2500…|小工具充电宝
utility|scale|米家八电极体脂秤 S8…|小工具体脂秤
utility|blanket|米家智能水暖毯|小工具水暖毯
utility|light|米家皮皮灯|小工具皮皮灯
utility|speaker|Xiaomi Sound Move|小工具便携音箱
dining|remote|B|餐厅B遥控开关
garden|camera|小米室外摄像机CW7…|菜地室外摄像机'''

# 每个动作显式列参数与全部枚举示例；数字参数列最小/中间/最大值。
def action(key, parameters, examples):
    return {'action':key,'parameters':parameters,'examples':examples}
def enum(key, param, choices, phrase):
    return action(key, f'{param}: enum {{{", ".join(k for k,v in choices)}}}', [(phrase.format(v=v), {param:k}) for k,v in choices])
def number(key, param, lo, mid, hi, unit, phrase):
    values = range(lo,hi+1) if hi-lo<=5 else (lo,mid,hi)
    return action(key, f'{param}: integer，{lo}..{hi}，步长 1；{unit}', [(phrase.format(v=v),{param:v}) for v in values])
def boolean(key, param, yes, no):
    return action(key,f'{param}: boolean',[(yes,{param:True}),(no,{param:False})])
def plain(key, phrase): return action(key,'无参数',[(phrase,{})])
POWER = boolean('set_power','on','打开{n}','关闭{n}')
STATE = plain('get_state','查询{n}的状态')
LIGHT = [POWER, number('set_brightness','percent',0,50,100,'百分比；0 等价关灯','把{{n}}亮度设为{v}%'), number('set_color_temperature','kelvin',2700,4000,5500,'K','把{{n}}色温设为{v}K')]
LIGHT.append(action('set_color','hex: string，格式 #RRGGBB；每通道 00..FF',[(f'把{{n}}设为{label}',{'hex':value}) for label,value in [('红色','#FF0000'),('绿色','#00FF00'),('蓝色','#0000FF'),('黄色','#FFFF00'),('紫色','#800080'),('青色','#00FFFF'),('白色','#FFFFFF'),('黑色','#000000')]]))
LIGHT += [number('adjust_brightness','delta',-100,10,100,'百分点，有符号；结果截断到 0..100','把{{n}}亮度调整{v}个百分点'),STATE]
PROFILES = {}
def profile(key, label, actions, unsupported): PROFILES[key]={'label':label,'actions':actions,'unsupported':unsupported}
profile('light','灯',LIGHT,'打开{n}的新风')
profile('light_group','截图灯组（虚拟）',LIGHT,'让{n}制冷')
profile('ac','空调',[POWER,enum('set_mode','mode',[('cool','制冷'),('heat','制热'),('dry','除湿'),('fan','送风'),('auto','自动模式')],'把{{n}}切换到{v}'),number('set_temperature','celsius',16,26,30,'摄氏度','把{{n}}温度设为{v}度'),enum('set_fan_speed','level',[('auto','自动'),('low','低档'),('medium','中档'),('high','高档')],'把{{n}}风速设为{v}'),boolean('set_fresh_air','enabled','打开{n}的新风','关闭{n}的新风'),number('set_fresh_air_level','level',1,2,3,'新风档位','把{{n}}新风设为{v}档'),boolean('set_vertical_swing','enabled','打开{n}的上下扫风','关闭{n}的上下扫风'),boolean('set_horizontal_swing','enabled','打开{n}的左右扫风','关闭{n}的左右扫风'),STATE],'把{n}温度设为40度')
for channels in (1,2,3,6):
    examples=[]
    for ch in range(1,channels+1):
        for on,word in [(True,'打开'),(False,'关闭')]: examples.append((f'{word}{{n}}第{ch}路',{'channel':f'ch{ch}','on':on}))
    examples.extend([('打开{n}所有通道',{'channel':'all','on':True}),('关闭{n}所有通道',{'channel':'all','on':False})])
    profile(f'switch{channels}','开关',[action('set_channel_power',f'channel: enum {{{", ".join("ch"+str(c) for c in range(1,channels+1))}, all}}；on: boolean',examples),STATE],'把{n}色温设为4000K')
profile('socket','插座',[POWER,STATE],'把{n}亮度设为50%')
profile('strip','插排',[POWER,action('set_channel_power','channel: enum {ch1,ch2,ch3}；on: boolean',[(f'{w}{{n}}第{c}路',{'channel':f'ch{c}','on':on}) for c in range(1,4) for on,w in [(True,'打开'),(False,'关闭')]]),STATE],'把{n}电压调到300伏')
profile('curtain','窗帘',[plain('open','打开{n}'),plain('close','关闭{n}'),plain('stop','停止{n}移动'),number('set_position','open_percent',0,50,100,'打开百分比，0 全关，100 全开','把{{n}}打开到{v}%'),STATE],'把{n}打开到120%')
AUDIO=[POWER,plain('pause','暂停{n}播放'),plain('resume','继续{n}播放'),plain('next_track','让{n}播放下一首'),plain('previous_track','让{n}播放上一首'),number('set_volume','percent',0,40,100,'百分比；0 静音，不是断电','把{{n}}音量设为{v}%'),boolean('set_mute','muted','让{n}静音','取消{n}静音'),STATE]
profile('speaker','音箱',AUDIO,'让{n}购买一首歌')
profile('audio','影音配件/音频连接器',AUDIO,'让{n}制冷')
profile('purifier','空气净化器',[POWER,enum('set_mode','mode',[('auto','自动模式'),('sleep','睡眠模式'),('manual','手动模式')],'把{{n}}切换为{v}'),number('set_speed','percent',1,50,100,'风速百分比','把{{n}}风速设为{v}%'),STATE],'把{n}温度设为26度')
profile('humidifier','加湿器',[POWER,enum('set_mode','mode',[('auto','自动模式'),('sleep','睡眠模式'),('manual','手动模式')],'把{{n}}切换为{v}'),number('set_target_humidity','percent',30,50,80,'相对湿度百分比','把{{n}}目标湿度设为{v}%'),number('set_mist_level','level',1,2,3,'加湿档位；无雾型号也统一用此模拟字段','把{{n}}加湿档位设为{v}档'),STATE],'把{n}湿度设为120%')
profile('fan','风扇',[POWER,number('set_speed','level',1,3,5,'档位','把{{n}}风速设为{v}档'),enum('set_mode','mode',[('normal','标准风'),('natural','自然风'),('sleep','睡眠风')],'把{{n}}切换到{v}'),boolean('set_oscillation','enabled','让{n}开始摇头','让{n}停止摇头'),STATE],'把{n}切换为制冷模式')
profile('feeder','宠物喂食机',[number('dispense','portions',1,3,10,'份，每份模拟 10 克','让{{n}}出粮{v}份'),STATE],'让{n}出粮100份')
for key,label in [('washer','擦地机/洗地机'),('vacuum','吸尘器')]:
    actions=[POWER,enum('set_mode','mode',[('eco','节能模式'),('auto','自动模式'),('turbo','强力模式')],'把{{n}}切换为{v}')]
    if key=='washer': actions += [plain('start_self_clean','让{n}开始自清洁'),plain('stop_self_clean','让{n}停止自清洁')]
    profile(key,label,actions+[STATE],'让{n}自己走到主卧清扫')
profile('robot','扫地机器人',[plain('start_clean','让{n}开始清扫'),plain('pause','暂停{n}清扫'),plain('resume','继续{n}清扫'),plain('return_to_dock','让{n}返回充电'),enum('set_mode','mode',[('vacuum','只扫地'),('mop','只拖地'),('both','扫拖同时')],'让{{n}}切换为{v}'),number('set_suction','level',1,2,4,'吸力档位','把{{n}}吸力设为{v}档'),number('set_water_level','level',1,2,3,'水量档位','把{{n}}水量设为{v}档'),STATE],'让{n}清扫地图上不存在的区域')
profile('camera','摄像机',[POWER,boolean('set_privacy','enabled','打开{n}隐私模式','关闭{n}隐私模式'),boolean('set_recording','enabled','让{n}开始录像','让{n}停止录像'),enum('set_night_vision','mode',[('auto','自动'),('on','开启'),('off','关闭')],'把{{n}}夜视设为{v}'),STATE],'让{n}识别陌生人的身份证号码')
profile('doorbell','可视门铃/猫眼',[boolean('set_privacy','enabled','打开{n}隐私模式','关闭{n}隐私模式'),number('set_ring_volume','percent',0,50,100,'铃声音量百分比','把{{n}}铃声音量设为{v}%'),boolean('set_motion_alert','enabled','打开{n}移动侦测提醒','关闭{n}移动侦测提醒'),STATE],'让{n}打开门锁')
profile('thermo','温湿度传感器',[plain('get_temperature','查询{n}的温度'),plain('get_humidity','查询{n}的湿度'),STATE],'把{n}温度设为26度')
for key,label,field,speech in [('presence','存在传感器','occupied','检测到有人了吗'),('motion','人体传感器','motion','检测到移动了吗'),('pressure','压力传感器','pressed','感应到压力了吗'),('smoke','烟雾传感器','smoke_detected','检测到烟雾了吗')]:
    profile(key,label,[plain(f'get_{field}','{n}'+speech),STATE],'关闭{n}的检测功能')
profile('remote','遥控器/无线开关',[STATE],'让{n}模拟单击')
profile('router','路由器',[plain('get_clients','查询{n}的已连接设备'),STATE],'修改{n}的WiFi密码')
profile('gateway','网关',[plain('get_children','查询{n}的子设备'),STATE],'让{n}恢复出厂设置')
profile('aroma','香薰机',[POWER,number('set_intensity','level',1,2,3,'香氛强度档位','把{{n}}香氛强度设为{v}档'),number('set_duration','minutes',1,30,120,'分钟','让{{n}}运行{v}分钟'),STATE],'把{n}香氛强度设为10档')
profile('blanket','电热毯/水暖垫',[POWER,number('set_temperature','celsius',25,35,45,'摄氏度','把{{n}}温度设为{v}度'),number('set_timer','minutes',1,120,480,'倒计时，到期关机','让{{n}}在{v}分钟后关闭'),STATE],'把{n}温度设为90度')
profile('microwave','微波炉',[action('start_heating','seconds: integer 1..1800；power_percent: enum {20,40,60,80,100}',[(f'让{{n}}以{p}%火力加热{s}秒',{'seconds':s,'power_percent':p}) for p,s in [(20,1),(40,60),(60,120),(80,300),(100,1800)]]),plain('pause','暂停{n}加热'),plain('resume','继续{n}加热'),plain('stop','停止{n}加热'),STATE],'让{n}加热两个小时')
profile('bath','浴霸',[boolean('set_light','on','打开{n}照明','关闭{n}照明'),boolean('set_ventilation','on','打开{n}换气','关闭{n}换气'),boolean('set_heating','on','打开{n}暖风','关闭{n}暖风'),number('set_temperature','celsius',20,28,40,'摄氏度','把{{n}}暖风温度设为{v}度'),STATE],'让{n}制冷')
profile('panel','控制面板',[boolean('set_screen','on','点亮{n}屏幕','熄灭{n}屏幕'),number('set_brightness','percent',0,50,100,'屏幕亮度百分比','把{{n}}屏幕亮度设为{v}%'),number('set_volume','percent',0,40,100,'屏幕音量百分比','把{{n}}音量设为{v}%'),STATE],'给{n}安装任意软件')
profile('lock','门锁',[plain('lock','锁上{n}'),plain('unlock','解锁{n}'),STATE],'把{n}密码改成123456')
profile('rack','晾衣架',[plain('raise','升起{n}'),plain('lower','降下{n}'),plain('stop','停止{n}移动'),number('set_position','height_percent',0,50,100,'高度百分比；0 最低，100 最高','把{{n}}升到{v}%高度'),boolean('set_light','on','打开{n}照明','关闭{n}照明'),STATE],'让{n}旋转一圈')
profile('battery','储能电源/充电宝',[boolean('set_output','enabled','打开{n}输出','关闭{n}输出'),plain('get_battery','查询{n}的剩余电量'),STATE],'把{n}输出电压设为220伏')
profile('scale','体重/体脂秤',[plain('get_last_measurement','查询{n}最近一次测量'),plain('get_battery','查询{n}的剩余电量'),STATE],'把{n}测量结果改为50公斤')

counts=Counter(); devices=[]
for line in RAW.splitlines():
    room,kind,name,alias=line.split('|'); counts[(room,kind)]+=1
    devices.append({'id':f'sim87_{room}_{kind}_{counts[(room,kind)]:03}', 'room':room,'profile':kind,'name':name,'alias':alias})
assert len(devices)==87 and len({d['id'] for d in devices})==87
by_alias={d['alias']:d for d in devices}
for alias,members in [('厨房基础灯组',['厨房2号筒灯','厨房4号筒灯']),('厨房完整灯组',['厨房2号筒灯','厨房4号筒灯','厨房感应筒灯','厨房3号射灯'])]:
    by_alias[alias]['members']=[by_alias[a]['id'] for a in members]

def family(d): return 'switch' if d['profile'].startswith('switch') else d['profile']
families=defaultdict(list)
for d in devices:
    if d['profile']!='light_group': families[family(d)].append(d)
groups=[]
for scope in ['all',*ROOMS]:
    for kind,ds in families.items():
        members=[d for d in ds if scope=='all' or d['room']==scope]
        if not members: continue
        label='开关' if kind=='switch' else PROFILES[kind]['label'].split('/')[0]
        groups.append({'id':f'simgrp_{scope}_{kind}', 'name':('全屋' if scope=='all' else ROOMS[scope])+f'所有{label}', 'members':[d['id'] for d in members], 'kind':kind})
assert sum(len(g['members']) for g in groups if g['id'].startswith('simgrp_all_'))==85
assert dict(Counter(d['room'] for d in devices)) == {'living':25,'master':14,'secondary':12,'kitchen':12,'toilet':7,'entry':8,'balcony':2,'utility':5,'dining':1,'garden':1}
ids={d['id'] for d in devices}
assert len({g['id'] for g in groups})==len(groups)
for g in groups:
    assert len(g['members'])==len(set(g['members'])) and set(g['members'])<=ids
for d in devices:
    p=PROFILES[d['profile']]
    assert len({a['action'] for a in p['actions']})==len(p['actions'])
    for a in p['actions']:
        assert a['examples']
        for sentence,params in a['examples']:
            assert '{' not in sentence.format(n=d['alias'])
            assert isinstance(params,dict)

intro='''# 家庭模拟设备、分组与语音指令全集 v1

版本：2026-09-27。用途：静默语音助手的意图识别、设备解析、参数提取与模拟执行评测。

本文件中每条执行、口语和设备专属负例句子都有固定用例 ID；英文对应句在[全量双语测试表](all-voice-test-cases.md)中使用相同 ID。分组用例也纳入该表。

## 1. 使用约定

- 共 87 个截图条目：85 个模拟叶子设备、2 个截图灯组；10 个房间分类。忽略截图的在线和可用状态，所有条目都可参与模拟测试。
- 名称、房间、类别取自米家极客版截图；名称保留截图省略号。语音称呼、ID、通道数、能力、数值范围、灯组成员由本规范人为设定，不是硬件实测结论。例如本版所有灯具均假设支持 RGB、2700–5500K 色温及调光。
- 本文是独立测试夹具，优先用于本次语料生成。既有 `mijia-100-devices.json` 含不同房间与命名，本次不使用、不覆盖。旧真实接入协议的限制仍适用于真实执行。
- `sim87_` 是条目 ID，`simgrp_` 是派生组 ID，永不当作米家物理 ID。改名和移动房间不改已分配 ID。每条语料记录 `registry_version=sim-home-v1`。
- 控制动作为封闭集合；未列出的动作不支持。只读设备同样有查询语句，但不得因测试需要虚构可写的温湿度、烟雾、人体状态。无线开关是输入设备，不是可远程按压的执行器。
- 物理墙壁开关只模拟各路通断，亮度/色温/颜色属于灯具。`电视开关` 不代表电视本体；`除臭风扇2` 按截图是插座；`热水` 是插座；`视频`、`解锁` 是独立开关，不推断其关联负载，也不隐含门锁动作。
- 本文的“穷举”是穷举支持的动作、离散参数和典型边界，并不是穷举无限自然语言。连续/整数范围以约束定义，语音例句覆盖最小、中间、最大；组合与越界值按第 6 节生成。
- 所有执行都写入模拟状态，不访问真实设备。微波炉加热、门锁开锁、出粮等在此仅用于分类测试，不据此扩展真实产品权限。

## 2. 统一语义与参数规则

### 2.1 判断输出

```json
{
  "registry_version": "sim-home-v1",
  "decision": "execute",
  "reason": "explicit_request",
  "target_id": "simgrp_all_ac",
  "action": "set_power",
  "parameters": {"on": false},
  "execution_mode": "simulate"
}
```

`decision` 固定为 `execute / ignore / needs_input`。不支持的动作或越界参数返回 `ignore`，reason 分别为 `unsupported_capability / invalid_parameter`；目标不唯一或缺参数返回 `needs_input`。普通闲聊、引用、否定返回 `ignore`。`needs_input` 只在页面显示缺失内容，不主动语音追问。查询也可为 execute，结果仅在页面显示。

### 2.2 参数和状态

- 数字按指定单位归一化：百分之五十、50 percent → 50；半开 → open_percent=50；2700K/2700开尔文 → 2700。绝对数值越界直接拒绝，不截断。
- 灯亮度 0 → power=false；亮度 1..100 → power=true。关灯保留上次非零亮度，开灯恢复该亮度（初始 50）。色温/颜色只改设置，不自动开灯；set_color 切为 RGB 模式，set_color_temperature 切为白光模式。黑色 #000000 保持供电状态、光输出为零，区别于关灯。
- “调亮一点/调暗一点”对应 adjust_brightness 的 delta=+10/-10 个百分点；该相对动作计算结果允许截断到 0..100。0 是合法 delta，表示不变。“最亮/最暗”明确映射 100/0。
- 空调 set_mode/set_temperature/set_fan_speed/扫风只修改设置，不隐含开机；“打开空调并制冷到26度”展开为有序的开机、模式、温度三个动作。新风是独立布尔功能；“切换到新风”映射 set_fresh_air(enabled=true)，不造 fresh_air 主模式。
- 插排总电源与分路状态独立，实际输出=总电源 AND 分路开关；单独开某一路不自动开总电源。墙壁开关没有 channel 时，多路需要补全；“整个/全部”用 all；单路默认 ch1。
- 窗帘百分比是打开比例；晾衣架百分比是高度，二者不要反向。移动动作在模拟器中立即到位；stop 保持当前值。
- 音量为整数 0..100，音箱与音频连接器 mute 独立于电源；音量大于0会解除静音。播放只操作预置本地测试曲目，不做在线检索和购买。
- 普通开关/插座与传感器无隐式联动。房间灯组不包含墙壁开关、浴霸照明、晾衣架照明或屏幕；“所有照明”若要跨这些类别应另行定义，不默认为所有灯。
- 默认关闭所有可切换功能，灯记忆亮度50/色温4000/RGB白色，空调26度/auto，新风1档，窗帘打开比例0，晾衣架高度100，音量40，风扇1档，加湿目标50%，加湿/香氛1档，水暖35度。其余数值默认取动作允许的最小值；枚举默认取表中首项。每条独立用例重置状态，多轮用例共享同一会话状态。
- 传感器固定夹具：温度24℃、湿度50%、occupied=false、motion=false、pressed=false、smoke_detected=false；电量80%；PM2.5=15μg/m³；机器人 docked；微波炉 idle；门锁 locked；最近体重60kg/体脂20%；路由器客户端和网关子设备列表初始为空。查询返回相应字段，get_state 返回该类型全部模拟状态。
- 其余查询状态：遥控器 last_event=null、battery_percent=80；喂食器 remaining_grams=1000、dispensed_portions=0；摄像机 recording=false、privacy=false；门铃 motion_alert=false；屏幕 screen_on=false；播放设备 playback=paused、track_index=0。喂食扣减每份10克，余粮不足返回模拟执行失败，不假装成功。
- 暂停/恢复/下一首等有状态动作必须在用例中指定前置状态；不满足前置条件时，意图判断仍可正确，模拟执行返回 invalid_state。门锁初始locked，重复lock等幂等设置成功但状态不变。所有开关通道初始关闭，不绑定其他设备。
- 不含隐藏的定时语义：“十分钟后开灯”交给调度业务，不能当作现在开灯；只有表中 set_timer / set_duration 有本设备计时功能。

## 3. 分组规则与完整成员

全屋组按类型聚合；房间组按房间+类型聚合，单成员也建组。明确“全部/所有/都”才展开组。没有“全部”且匹配多台时 needs_input，不随便选第一台。类型不跨房间猜测；未指定房间但某类型全屋唯一时可直接解析，其他情况需用例明确提供 default_room 或补充房间。

组操作递归展开、按设备 ID 去重，再做全部成员能力及参数校验，最后统一执行。任一成员不支持时，整组不执行并报告具体成员；不要静默跳过。只读组只聚合查询；成员能力不同则只接受共有动作。例如所有开关只接受 set_channel_power(channel=all,on=...)，不对所有开关统一指定 ch6。组相对调光使用各成员自身当前值。

示例：“关闭所有空调”→ simgrp_all_ac / set_power / {on:false}；“关闭主卧所有灯”→ simgrp_master_light / set_power / {on:false}；“所有空调设为26度”→ simgrp_all_ac / set_temperature / {celsius:26}；“厨房所有灯设为暖白光”→ simgrp_kitchen_light / set_color_temperature / {kelvin:3000}。

白光别名：暖光2700K、暖白3000K、自然光4000K、冷白5500K。各设备唯一语音称呼可直接用；原始同名必须结合房间。厨房“基础灯组”和“完整灯组”是下文人为设定的成员映射，不代表真实米家已有绑定。

| 分组 ID | 分组名 | 数量 | 叶子成员 ID |
| --- | --- | ---: | --- |
'''
out=[intro]
for g in groups: out.append(f"| `{g['id']}` | {g['name']} | {len(g['members'])} | "+'、'.join(f'`{m}`' for m in g['members'])+' |\n')
out.append('\n### 3.1 灯光常用叫法与目标消歧\n\n房间泛称“灯/照明”作为房间灯组的别名；产品类型词或名称别名用于定位单灯。如下口语例句是重要正例：\n\n')
light_room_groups=[g for g in groups if g['id'].endswith('_light') and not g['id'].startswith('simgrp_all_')]
for g in light_room_groups:
    room_label=ROOMS[g['id'].split('_')[1]]
    on_phrases=[f'开{room_label}灯',f'开{room_label}的灯',f'打开{room_label}照明',f'把{room_label}的灯打开',f'{room_label}开灯',f'点亮{room_label}所有灯',f'{room_label}灯都打开',f'麻烦开一下{room_label}的照明']
    off_phrases=[f'关{room_label}灯',f'关{room_label}的灯',f'关闭{room_label}照明',f'把{room_label}的灯关掉',f'{room_label}关灯',f'熄灭{room_label}所有灯',f'{room_label}灯都关掉',f'麻烦关一下{room_label}的照明']
    out.append(f'- **{room_label}灯组开灯**（{len(g["members"])} 盏，逐台执行）：\n')
    for qi,phrase in enumerate(on_phrases,1):
        case_id=f'GRP-{g["id"]}-ROOM-ON-{qi:02}'
        out.append(f'  - `{case_id}` “{phrase}”\n')
    out.append(f'- **{room_label}灯组关灯**：\n')
    for qi,phrase in enumerate(off_phrases,1):
        case_id=f'GRP-{g["id"]}-ROOM-OFF-{qi:02}'
        out.append(f'  - `{case_id}` “{phrase}”\n')
out.append('- “开灯”“把灯打开”“灯亮一下”“照明打开”只有 `context.default_room` 已设置且该房间有灯组时，才映射到默认房间灯组；缺默认房间时 `needs_input/missing_room`。\n')
out.append('- “关灯”“把灯关了”“照明全关”按同一默认房间规则映射 `set_power(on=false)`；“所有房间灯都关了”“全屋灯关闭”映射 `simgrp_all_light`。\n')
out.append('- “开吸顶灯”“把顶灯打开”先用明确房间或 `default_room` 消歧；全屋有多盏吸顶灯而没有房间上下文时 `needs_input/ambiguous_target`。有房间且该房间只匹配一盏吸顶灯时，映射该设备 ID。\n')
out.append('- “开主卧灯”指主卧灯组；“开主卧吸顶灯”指主卧吸顶灯单设备；“开卧室灯”因主卧、次卧都有灯且卧室未指明，需澄清。厨房“所有灯”组展开4盏叶子灯，两个截图灯组条目不额外产生状态。\n')
out.append('- “开电视灯/看电视时把灯关暗一点”不是设备目标明确的开关命令；需要照明氛围联动时应作为单独场景，不在本设备目录内推断。\n')
out.append('\n## 4. 房间与数量索引\n\n| 房间 | 条目数 |\n| --- | ---: |\n')
for room,name in ROOMS.items(): out.append(f'| {name} | {sum(d["room"]==room for d in devices)} |\n')
out.append('\n## 5. 全部设备详情与逐动作语音指令\n\n每个语音例句后是预期 action(parameters)。同一行各参数示例互为独立用例；并非一次执行多次。每台设备最后附一条不支持或越界负例。\n')
example_count=0
variation_count=0
case_rows=[]
def english_target(d):
    room_en={'living':'living room','master':'master bedroom','secondary':'second bedroom','kitchen':'kitchen','toilet':'bathroom','entry':'entryway','balcony':'balcony','utility':'utility area','dining':'dining room','garden':'garden'}[d['room']]
    kind=d['profile']; name=d['name']
    if kind=='ac': label='air conditioner'
    elif kind in ('light','light_group'):
        label=('monitor lamp' if '挂灯' in name else 'ceiling light' if '吸顶灯' in name else 'occupancy downlight' if '人在感' in name else 'downlight' if '筒灯' in name else 'spotlight' if '射灯' in name else 'bathroom light' if '青空灯' in name else 'light')
    else:
        labels={'switch1':'wall switch','switch2':'wall switch','switch3':'wall switch','switch6':'wall switch','socket':'smart plug','strip':'power strip','curtain':'curtain','speaker':'smart speaker','audio':'audio adapter','purifier':'air purifier','humidifier':'humidifier','fan':'fan','feeder':'pet feeder','washer':'floor washer','vacuum':'vacuum cleaner','robot':'robot vacuum','camera':'camera','doorbell':'video doorbell','thermo':'temperature and humidity sensor','presence':'presence sensor','motion':'motion sensor','pressure':'pressure sensor','smoke':'smoke detector','remote':'remote control','router':'router','gateway':'gateway','aroma':'aroma diffuser','blanket':'heated blanket','microwave':'microwave','bath':'bathroom heater','panel':'control panel','lock':'door lock','rack':'drying rack','battery':'power bank','scale':'body composition scale'}
        label=labels.get(kind,'device')
    siblings=[x for x in devices if x['room']==d['room'] and x['profile']==kind]
    if len(siblings)>1 and kind not in ('light_group',): label+=f' {siblings.index(d)+1}'
    return f'{room_en} {label}'

def record_case(case_id,text,target,action_name,parameters,decision='execute',reason='explicit_request',target_en=''):
    case_rows.append({'id':case_id,'text':text,'expected_decision':decision,'expected_reason':reason,'target_id':target,'target_en':target_en,'action':action_name,'parameters':parameters})
def variants(d):
    """独立于动作表的自然口语例句，预期参数仍须落在同一封闭协议。"""
    n=d['alias']; kind=d['profile']; room=ROOMS[d['room']]
    if kind=='ac':
        short='空调'
        return [
            (f'把{n}温度设为26度','set_temperature',{'celsius':26}),
            (f'{room}{short}温度26','set_temperature',{'celsius':26}),
            (f'{room}的{short}设为26','set_temperature',{'celsius':26}),
            (f'{room}那台{short}调到二十六度','set_temperature',{'celsius':26}),
            (f'{n}给我调26','set_temperature',{'celsius':26}),
            (f'{room}空调，二十六度','set_temperature',{'celsius':26}),
            (f'二十六度，{room}空调','set_temperature',{'celsius':26}),
            (f'{room}空调温度改成26℃','set_temperature',{'celsius':26}),
            (f'麻烦{room}空调降到26','set_temperature',{'celsius':26}),
            (f'{room}空调温控拨到26','set_temperature',{'celsius':26}),
            (f'{n}开制冷','set_mode',{'mode':'cool'}),
            (f'{room}的空调新风打开','set_fresh_air',{'enabled':True}),
            (f'{room}空调除湿一下','set_mode',{'mode':'dry'}),
        ]
    if kind in ('light','light_group'):
        target_name=n
        name=d['name']
        if '吸顶灯' in name: aliases=['吸顶灯','顶灯','天花板灯']
        elif '人在感' in name: aliases=['感应筒灯','人体感应筒灯']
        elif '筒灯3 Pro-2' in name: aliases=['2号筒灯','二号筒灯']
        elif '筒灯3 Pro-4' in name: aliases=['4号筒灯','四号筒灯']
        elif '筒灯' in name: aliases=['感应筒灯','人在感应筒灯']
        elif '射灯' in name: aliases=['3号射灯','三号射灯']
        elif '挂灯' in name: aliases=['挂灯','显示器灯','屏幕挂灯']
        elif '青空灯' in name: aliases=['青空灯','厕所灯']
        elif '皮皮灯' in name: aliases=['皮皮灯','氛围灯']
        elif '灯组' in name or kind=='light_group': aliases=[n]
        elif '灯' in name: aliases=[name.replace('米家','').replace('Pro','').strip(),'灯']
        else: aliases=['灯']
        phr=[]
        # 产品词的八种常见语序。别名若在本房间重复，单独称呼须消歧。
        for alias in dict.fromkeys(a for a in aliases if a):
            phr.extend([
                (f'开{room}{alias}','set_power',{'on':True}),
                (f'开{room}的{alias}','set_power',{'on':True}),
                (f'打开{room}的{alias}','set_power',{'on':True}),
                (f'把{room}{alias}打开','set_power',{'on':True}),
                (f'点亮{room}的{alias}','set_power',{'on':True}),
                (f'麻烦开一下{room}{alias}','set_power',{'on':True}),
                (f'{room}{alias}开起来','set_power',{'on':True}),
                (f'关掉{room}的{alias}','set_power',{'on':False}),
                (f'{room}的{alias}灭掉','set_power',{'on':False}),
                (f'把{room}{alias}关了','set_power',{'on':False}),
            ])
        return phr + [
            (f'{n}开一下','set_power',{'on':True}),
            (f'把{n}给关了','set_power',{'on':False}),
            (f'{n}亮度给我调到五十','set_brightness',{'percent':50}),
            (f'{n}开到一半亮','set_brightness',{'percent':50}),
            (f'{n}最亮','set_brightness',{'percent':100}),
            (f'{n}暗一点','adjust_brightness',{'delta':-10}),
            (f'{n}暖白光','set_color_temperature',{'kelvin':3000}),
            (f'{n}改成红灯','set_color',{'hex':'#FF0000'}),
        ]
    if kind.startswith('switch'):
        return [
            (f'{n}全关','set_channel_power',{'channel':'all','on':False}),
            (f'{n}所有路打开','set_channel_power',{'channel':'all','on':True}),
            (f'{n}第一路给我开','set_channel_power',{'channel':'ch1','on':True}),
            (f'{n}1号键关了','set_channel_power',{'channel':'ch1','on':False}),
        ]
    if kind=='curtain':
        return [(f'{n}拉开','open',{}),(f'{n}合上','close',{}),(f'{n}开一半','set_position',{'open_percent':50}),(f'{n}别动了','stop',{})]
    if kind=='fan':
        return [(f'{n}打开','set_power',{'on':True}),(f'{n}关掉','set_power',{'on':False}),(f'{n}风速三档','set_speed',{'level':3}),(f'{n}不要摇头了','set_oscillation',{'enabled':False})]
    if kind in ('speaker','audio'):
        return [(f'{n}音量一半','set_volume',{'percent':50}),(f'{n}别出声了','set_mute',{'muted':True}),(f'{n}下一首','next_track',{}),(f'{n}继续播','resume',{})]
    if kind in ('socket','strip','purifier','humidifier','aroma','blanket','washer','vacuum','camera'):
        return [(f'{n}开一下','set_power',{'on':True}),(f'{n}关掉','set_power',{'on':False}),(f'看一下{n}现在什么状态','get_state',{})]
    return [(f'看看{n}现在什么状态','get_state',{}),(f'{n}状态查一下','get_state',{})]
for idx,d in enumerate(devices,1):
    p=PROFILES[d['profile']]; n=d['alias']
    out.append(f"\n### {idx:02d}. {n}\n\n- 设备名字：{d['name']}\n- 唯一 ID：`{d['id']}`\n- 所在房间：{ROOMS[d['room']]}\n- 设备类型：{p['label']}\n- 唯一语音称呼：{n}\n")
    if 'members' in d:
        out.append('- 模拟成员：'+'、'.join(f'`{x}`' for x in d['members'])+'。按叶子展开，不保存独立灯状态。\n')
        out.append('- 所属派生组：不直接加入；成员已加入厨房所有灯、全屋所有灯，执行时去重。\n')
    else:
        out.append('- 所属分组：'+ '、'.join(f'`{g["id"]}`' for g in groups if d['id'] in g['members'])+'。\n')
    out.append('\n| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |\n| --- | --- | --- |\n')
    for ai,a in enumerate(p['actions'],1):
        examples=[]
        for ei,(speech,params) in enumerate(a['examples'],1):
            utterance=speech.format(n=n)
            case_id=f'CMD-{d["id"]}-{ai:02}-{ei:02}'
            record_case(case_id,utterance,d['id'],a['action'],params,target_en=english_target(d))
            examples.append(f'`{case_id}` “'+utterance+'” → `'+json.dumps(params,ensure_ascii=False,separators=(',',':'))+'`')
            example_count+=1
        out.append(f"| `{a['action']}` | {a['parameters']} | "+'<br>'.join(examples)+' |\n')
    negative=p['unsupported'].format(n=n)
    negative_id=f'NEG-{d["id"]}'
    record_case(negative_id,negative,d['id'],None,{},'ignore','unsupported_capability',english_target(d))
    out.append(f'\n负例 `{negative_id}`：“'+negative+'”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。\n')
    out.append('\n口语变体（预期均为 `execute`，设备目标均为上述 ID）：\n\n')
    for vi,(speech,act,params) in enumerate(variants(d),1):
        assert act in {a['action'] for a in p['actions']}, (d['id'],act)
        case_id=f'VAR-{d["id"]}-{vi:03}'
        record_case(case_id,speech,d['id'],act,params,target_en=english_target(d))
        out.append(f'- `{case_id}` “{speech}” → `{act}` `'+json.dumps(params,ensure_ascii=False,separators=(',',':'))+'`\n')
        variation_count+=1

# Group commands are explicit tests too, and share the same ID namespace as device utterances.
group_case_count=0
for g in groups:
    member_devices=[d for d in devices if d['id'] in g['members']]
    if not member_devices: continue
    supported=set(a['action'] for a in PROFILES[member_devices[0]['profile']]['actions'])
    for md in member_devices[1:]: supported &= {a['action'] for a in PROFILES[md['profile']]['actions']}
    labels={'ac':'air conditioner','light':'light','speaker':'speaker','socket':'outlet','strip':'power strip','fan':'fan','purifier':'air purifier','humidifier':'humidifier','camera':'camera','feeder':'pet feeder','washer':'floor washer','vacuum':'vacuum','curtain':'curtains','aroma':'aroma diffuser','blanket':'heated blanket','bath':'bathroom heater','panel':'control panel','rack':'drying rack','battery':'power bank','robot':'robot vacuum'}
    ROOM_EN={'living':'living room','master':'master bedroom','secondary':'second bedroom','kitchen':'kitchen','toilet':'bathroom','entry':'entryway','balcony':'balcony','utility':'utility area','dining':'dining room','garden':'garden'}
    target_en=f"{ROOM_EN.get(g['id'].split('_')[1],'whole home')} {labels.get(g['kind'],'device')} group"
    if 'set_power' in supported:
        group_label=g['name']
        for state,verb,phrases in [(True,'on',[f'打开{group_label}',f'把{group_label}都打开',f'{group_label}全开']), (False,'off',[f'关闭{group_label}',f'把{group_label}都关掉',f'{group_label}全关'])]:
            for qi,phrase in enumerate(phrases,1):
                case_id=f'GRP-{g["id"]}-PWR-{verb.upper()}-{qi:02}'
                record_case(case_id,phrase,g['id'],'set_power',{'on':state},target_en=target_en)
                group_case_count+=1
    if g['kind']=='ac' and 'set_temperature' in supported:
        for value in (20,26,30):
            for qi,phrase in enumerate([f'把{g["name"]}设为{value}度',f'{g["name"]}温度{value}',f'所有空调都调到{value}度' if g['id']=='simgrp_all_ac' else f'{ROOMS[g["id"].split("_")[1]]}的空调调到{value}度'],1):
                case_id=f'GRP-{g["id"]}-TEMP-{value}-{qi:02}'
                record_case(case_id,phrase,g['id'],'set_temperature',{'celsius':value},target_en=target_en)
                group_case_count+=1
    if g['kind']=='light' and 'set_power' in supported:
        scope=g['id'].split('_')[1]
        room_label='全屋' if scope=='all' else ROOMS[scope]
        on_phrases=[f'开{room_label}灯',f'开{room_label}的灯',f'打开{room_label}照明',f'把{room_label}的灯打开',f'{room_label}开灯',f'点亮{room_label}所有灯',f'{room_label}灯都打开',f'麻烦开一下{room_label}的照明']
        off_phrases=[f'关{room_label}灯',f'关{room_label}的灯',f'关闭{room_label}照明',f'把{room_label}的灯关掉',f'{room_label}关灯',f'熄灭{room_label}所有灯',f'{room_label}灯都关掉',f'麻烦关一下{room_label}的照明']
        for state,verb,phrases in [(True,'ON',on_phrases),(False,'OFF',off_phrases)]:
            for qi,phrase in enumerate(phrases,1):
                record_case(f'GRP-{g["id"]}-ROOM-{verb}-{qi:02}',phrase,g['id'],'set_power',{'on':state},target_en=target_en)
                group_case_count+=1

# Generic device mentions require context; keep their expected abstention in the same ID set.
for qi,phrase in enumerate(['开灯','把灯打开','灯亮一下','照明打开','关灯','开吸顶灯','把顶灯打开','开卧室灯'],1):
    ambiguous=qi>5
    case_id=f'AMB-LIGHT-{qi:02}'
    case_rows.append({'id':case_id,'text':phrase,'expected_decision':'needs_input','expected_reason':'ambiguous_target' if ambiguous else 'missing_room','target_id':None,'target_en':'a light','action':None,'parameters':{},'context':{'default_room':None}})
    group_case_count+=1

# Append group utterances and export the authoritative structured command set.
out.append('\n## 5.1 分组口语指令与可关联 ID\n\n分组测试语句和标准答案也有稳定 ID，可在双语 TTS 表中按 ID 查找。\n\n')
for r in case_rows:
    if r['id'].startswith('GRP-'):
        out.append(f'- `{r["id"]}` “{r["text"]}” → `{r["target_id"]}` / `{r["action"]}` `'+json.dumps(r['parameters'],ensure_ascii=False,separators=(',',':'))+'`\n')
command_path=ROOT/'testdata'/'smart-home-command-cases-v1.jsonl'
command_path.parent.mkdir(parents=True,exist_ok=True)
command_path.write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in case_rows),encoding='utf-8')

out.append('''
## 6. 语音测试扩展与验收标准

### 6.1 语句变体

每一条正例至少扩展以下形式，预期结构不变。第 5 节还为每台设备给出经逐动作校验的口语变体：

| 表达类型 | 例句 |
| --- | --- |
| 直接指令 | 关闭客厅吸顶灯 |
| 礼貌请求 | 麻烦帮我把客厅吸顶灯关一下 |
| 简略命令 | 客厅吸顶灯，关掉 |
| 口语修饰 | 那个客厅吸顶灯给我关了吧 |
| 英文 | Turn off the living room ceiling light. |
| 中英混合 | 把客厅吸顶灯 turn off |
| 参数口语 | 厨房窗帘开一半；主卧空调二十六度 |
| 指令加闲话 | 有点刺眼，帮我把客厅吸顶灯关掉 |

口语变化要跨越语序、动词、省略、数字读法和礼貌程度，不能只替换“请/帮我”。完整温控等价集（以下正例均为 `sim87_living_ac_001 / set_temperature / {celsius:26}`）：

| 变化方向 | 例句 |
| --- | --- |
| 正式动作 | 把客厅空调温度设为26度；将客厅空调调至26摄氏度 |
| 省去动词 | 客厅空调温度26；客厅空调二十六度；客厅那台空调，26 |
| 省去“温度” | 客厅的空调设为26；客厅空调给我调26；客厅空调弄成26 |
| 宾语前置 | 26度，客厅空调；二十六，客厅那台空调 |
| 口语谓词 | 客厅空调温控拨到26；客厅空调降到26；客厅空调别那么热，26度 |
| 单位与数字 | 客厅空调调26℃；客厅空调调二十六度；客厅空调调到26度整 |
| 礼貌与填充词 | 麻烦把客厅那个空调调成26呗；嗯，客厅的空调，调到26就行 |
| 房间代称，仅有唯一可写目标时 | 客厅的温度改成26；客厅温度给我调到26度 |

“客厅的温度改成26”没有说空调，但本夹具中客厅仅有一台可写 `set_temperature` 的设备，因此可推断为空调；客厅温湿度计是只读设备，不是候选。相反，“主卧的温度改成26”同时可能指主卧空调或主卧水暖垫，标注为 `needs_input/ambiguous_target`。没有明确修改动词的“客厅温度26”可能是陈述，不作为执行正例；“客厅温度多少”是查询，不能混入调温正例。显式说“空调”的“主卧空调温度26”可消除歧义。“客厅空调开到26”同时包含开机，标注两个有序动作：`set_power(on=true)`、`set_temperature(celsius=26)`。

不同动作也要做语序和省略变化：

| 目标动作 | 等价说法（同一行语义一致） |
| --- | --- |
| 客厅吸顶灯关 | 关了客厅灯；客厅的吸顶灯灭掉；客厅灯给我关一下；客厅那盏灯别亮了 |
| 厨房窗帘开至50% | 厨房窗帘开一半；厨房的窗帘拉到中间；厨房窗帘打开百分之五十 |
| 次卧白色风扇3档 | 次卧白色风扇三档；白色风扇在次卧，风速开三档；次卧那台白色电风扇调到3挡 |
| 主卧净化加湿器目标50% | 主卧净化加湿器目标湿度五十；把主卧那台净化加湿器设成百分之五十 |
| 关闭所有空调 | 全屋空调都关掉；把每个房间的空调关了；三台空调全关 |
| 关闭主卧所有灯 | 主卧的灯全灭；主卧两盏灯都关上；把主卧所有照明灯关了（仅指 light 类型） |

同一目标还需对照非指令：“客厅空调是不是26度？”是查询；“如果客厅空调到26度就好了”是愿望；“昨天客厅空调设了26”是历史；“别把客厅空调设26”是否定；“客厅空调……不对，次卧空调26”最终只改次卧。评分时必须成对覆盖，不能只看命中关键词。

英文房间固定映射：living room=客厅，master bedroom=主卧，second bedroom=次卧，kitchen=厨房，bathroom/toilet=厕所，entrance=门厅，balcony=阳台，utility=小工具，dining room=餐厅，garden=菜地。“bedroom/卧室”不自动等于主卧。

英文设备类型按本表动作模板映射，多个同类仍需额外限定。例如 master bedroom purifier humidifier=主卧净化加湿器，master bedroom mist-free humidifier=主卧无雾加湿器；second bedroom 4C camera / 3 Pro camera；kitchen downlight number two / four、kitchen occupancy downlight、kitchen spotlight number three。不允许用完全相同英文称呼标注为两个不同设备的确定正例。

### 6.2 每台设备必须配套的负例

1. 否定：“不要关闭客厅吸顶灯”→ ignore/negated。
2. 引用：“他说‘关闭客厅吸顶灯’”→ ignore/quoted。
3. 历史：“我昨天关闭了客厅吸顶灯”→ ignore/past_event。
4. 能力询问：“客厅吸顶灯能调色温吗”→ ignore/capability_question，不执行调色温。
5. 状态查询：“客厅吸顶灯现在开着吗”→ execute/get_state，与能力询问区分。
6. 不完整：“把主卧空调温度调到”→ needs_input/missing_parameter。
7. 歧义：“打开主卧音箱”→ needs_input/ambiguous_target，因为有 AI 音箱和触屏音箱。
8. 纠正：“打开次卧白色风扇，不对，关掉”→ 仅 set_power(false)，同一最终语句先完成解析再执行。
9. 条件：“如果热了就开主卧空调”→ ignore/unsupported_condition，不马上开机。
10. 不存在：“打开书房空调”→ ignore/unknown_target，不用主卧或客厅空调替代。
11. 撤回：“把客厅吸顶灯打开，算了别开了”→ ignore/cancelled，不留下首次开灯副作用。
12. 多轮代词：“把它关掉”只有 context.last_target 唯一且仍有效时才解析，否则 needs_input。有效期在用例中固定为30秒。

### 6.3 数值、组合和重复覆盖

- 每个整数参数：min、min+1、中间值、max-1、max；非法 min-1、max+1、小数、缺失、NaN、错误单位。参数未允许小数时不自动四舍五入。
- 每个枚举：全部合法取值、一个不存在的值；每个布尔值 true/false；通道逐路测试，不只测试第一路。
- RGB 解析接受 #RRGGBB 或具名颜色表。任意合法 RGB 可构造；拒绝不合法十六进制、缺位和超出0..255的通道。不把“暖白”误当 RGB 白色。
- 双参数动作按合法域生成笛卡尔组合或成对覆盖；微波炉用不同火力与秒数交叉，不只沿用表中5个示例配对。
- 状态依赖用例显式记录 initial_state。例如灯当前95，调亮10后为100；灯当前5，调暗10后关灯；窗帘停止不改变已有位置。
- 群组覆盖全屋组、房间组、单成员组、截图虚拟灯组、重叠组和不存在的组。例：“关闭厨房基础灯组和厨房所有灯”只对4个唯一叶子灯各执行一次。
- 复合指令先完全解析，再验证所有动作。例如“主卧空调开机制冷26度”三步都正确才提交；“关客厅灯并把空调设为100度”整条不执行，防止部分副作用。
- 用固定 utterance_id / request_id 测试幂等：同一 final ASR 的网络重试只执行一次；用户重新说相同指令但新 request_id，应允许再次执行。出粮等非幂等动作尤其需要此测试。
- 语义分组后划分数据集，再合成多个 TTS 声音；同一文本、翻译、近义改写及其音频变体不跨训练/调试/最终测试分区泄漏。

### 6.4 标注字段和判分

每条用例至少包含 text_id、semantic_group、language、text、registry_version、context（默认房间/时间/历史/初始状态）、expected_decision、expected_reason、expected_actions（target_id/action/parameters）、expected_leaf_ids、expected_final_state。负例的 expected_actions 为空。音频另记 audio_path、tts_engine、voice、rate，不改变语义标签。

分别统计：是否应执行、业务意图、目标设备/组、动作、参数、展开后的叶子集合、最终状态、误执行率。负例误执行单独报告，不能被大量简单开关正例掩盖。多目标指令要求集合完全匹配，选少或多选都不能算全对。

## 7. 范围边界

本规范对模拟集提供完整封闭动作集合；真实接入仍需独立映射验证。截图无法确定的型号不补全，灯组成员与多路通道均为测试设定。无线开关关联哪个灯、插座控制哪个负载、摄像机物理云台能力、真实音箱媒体服务等均不自动推断。

本次不改变安卓应用现有配置，不接入设备，不生成TTS音频；本文可直接交给另一个AI用来生成有标准答案的语音测试集和模拟器。
''')
out.append(f'\n生成校验：87 条设备/组详情；85 个叶子设备；{len(groups)} 个派生分组；{example_count} 条逐动作正例表达；{variation_count} 条逐设备口语变体；87 条设备专属负例；{group_case_count} 条分组口语用例。\n')
target=ROOT/'docs/simulated-home-device-catalog.md'
target.write_text(''.join(out),encoding='utf-8')
print(json.dumps({'file':str(target),'devices':len(devices),'groups':len(groups),'positive_examples':example_count,'device_variations':variation_count,'group_cases':group_case_count,'all_cases':len(case_rows),'rooms':dict(Counter(d['room'] for d in devices))},ensure_ascii=False))
