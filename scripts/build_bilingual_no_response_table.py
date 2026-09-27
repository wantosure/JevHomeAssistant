"""Pair each no-response text with a natural English equivalent under the same stable ID."""
from pathlib import Path
import csv, json

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'testdata'/'no-response-expansion-v1.jsonl'
CSVOUT=ROOT/'testdata'/'all-voice-test-cases-v1.csv'
MDOUT=ROOT/'docs'/'all-voice-test-cases.md'
COMMANDS=ROOT/'testdata'/'smart-home-command-cases-v1.jsonl'

# False-trigger scenario phrases are paired with varied English frames. The paired texts
# retain the same no-response decision and scenario intent; English is adapted for natural TTS.
EN={
 'unknown_location':(
  ['on the second floor','on the third floor','in the basement','in the attic','in the study','in the kids’ room','in the guest room','in the storage room','at the far end of the hallway','in the garage'],
  ['Turn on the lights {x}.','Switch off the air conditioner {x}.','Open the curtains halfway {x}.','Turn on the fan {x}.','Turn off all the lights {x}.']),
 'unsupported_media':(
  ['CCTV-1','the evening news','the weather forecast','a nature program','a documentary','a sports channel','a movie channel','local news','a cartoon','a variety show'],
  ['Play {x} on the TV.','Put on {x} for me.','Switch the TV to {x}.','Search for {x} on the TV.','Let the living-room TV keep playing {x}.']),
 'device_mentioned_only':(
  ['I bought a lamp today and plan to install it this weekend.','A friend recently got a new air conditioner.','I saw someone putting up curtains yesterday.','Dad says the old fan at home has started making noise.','A coworker just bought an air purifier.','The doorbell at the downstairs neighbor’s place sounds loud.','Someone online recommended a robot vacuum.','I passed a shop selling smart speakers.','I used to have a very old humidifier.','The lamp at home used to flicker when I was a kid.'],
  ['I was just chatting about home stuff: {x}','The conversation drifted to appliances: {x}','I suddenly remembered something: {x}','It came up while we were talking: {x}','I was telling someone about this: {x}']),
 'quoted_or_reported':(
  ['“Turn on the bedroom light.”','“Set the living-room AC to 26 degrees.”','“Please turn off all the lights.”','“Turn on the light.”','“Switch on the air conditioner.”','“Open the curtains.”','“Control the smart lights.”','“Turn on the fan.”','“Turn on the lights.”','“Turn off the AC.”'],
  ['He said, {x}','She mentioned, {x}','The video included the line, {x}','I am only quoting the phrase, {x}','Someone else sent me a message saying, {x}']),
 'negated_or_cancelled':(
  ['turn on the living-room light','switch on the bedroom ceiling light','set the AC to 26 degrees','open the curtains','turn on the fan','switch off the air purifier','run the humidifier','start the robot vacuum','turn on the control-panel screen','play any music today'],
  ['Do not {x}.','Please don’t {x} yet.','There is no need to {x}.','I am not asking you to {x}.','I changed my mind. Don’t {x}.']),
 'conditional_or_hypothetical':(
  ['turn on the light if I get home late','switch on the AC when it gets hot','close the curtains if it rains','turn on the hallway light if someone comes in','raise the temperature if I feel cold','start the robot vacuum after I leave','run the purifier if the air gets bad','notify me if someone rings the bell','turn on the humidifier if the air gets dry','what would happen if the power went out'],
  ['What if we were to {x}?','I am thinking about whether to {x}.','Maybe I will {x} later.','It would be nice if I could {x}.','I have not decided whether to {x}.']),
 'questions_not_requests':(
  ['Would setting the living-room AC to 26 degrees feel too cold?','Is the bedroom ceiling light too bright?','Is it a good idea to open the curtains every day?','Would running a fan all night be uncomfortable?','How often should an air-purifier filter be changed?','How long does a smart-lock battery usually last?','Should a robot vacuum clean every day?','Is it okay to put a humidifier by the bed?','Is warm lighting better in the evening?','What is on CCTV-1 today?'],
  ['I wonder about this: {x}','This question came to mind: {x}','I was just wondering: {x}','Someone asked me, “{x}”','I have a question about this: {x}']),
 'background_media_or_transcription':(
  ['the news is on CCTV-1','someone on the radio is talking about the weather','a phone video says “turn on the living-room light”','people nearby are discussing the AC temperature','someone next door said to open the curtains','a short video is reviewing robot vacuums','the broadcast mentioned air purifiers','a TV show is talking about smart locks','someone is watching a humidifier review','there is an ad for a fan playing in the living room'],
  ['In the background, {x}.','The TV is on; {x}.','I can hear that {x}.','A video is playing where {x}.','There is some background audio: {x}.']),
 'fragment_or_ambiguous':(
  ['that lamp','that thing in the living room','the temperature, more or less','the device we talked about last time','that thing by the window','the room upstairs','that appliance from yesterday','the lighting situation','the AC thing','that thing we mentioned later'],
  ['I was thinking about {x}.','Maybe it was {x}; I cannot quite remember.','Something about {x}…','I am not sure what to do about {x}.','What was I saying about {x}?']),
 'third_party_or_directed_elsewhere':(
  ['Mom, could you turn on the living-room light?','Xiao Wang, can you check what is wrong with the AC?','Dad, remember to close the curtains when you get home.','The repair person says the fan may be broken.','A coworker said the purifier filter needs replacing.','A friend asked me to help pick a door lock.','The kid said the robot vacuum got stuck again.','The neighbor asked if the humidifier is leaking.','The shop assistant showed me a new ceiling lamp.','The doctor told me to get more rest.'],
  ['I told someone else: {x}','Someone asked me to pass this along: {x}','We were talking about it; {x}','That was directed to my family, not the assistant: {x}','I was just repeating what someone said: {x}']),
}

CHAT_EN={
 'mood':['I am in a pretty good mood today.','I felt a little groggy when I got up.','I feel more relaxed than yesterday.','I suddenly felt a bit tired.','My brain is not working very quickly today.','I am getting sleepy this afternoon.','I have felt pretty steady lately.','I feel lighter now that I have finished everything.','I am quietly pleased with what I got done today.','Thinking about the weekend makes me happy.','I zoned out for a moment.','I seem to have plenty of patience today.','It feels better after I put things in perspective.','I just want a little quiet time.','Time seems to be flying by lately.'],
 'food':['I had a bowl of noodles for lunch.','The rice came out just right today.','The oranges I bought are really sweet.','I feel like having something light for dinner.','The wontons at that little place taste great.','I had breakfast pretty late today.','There is half a watermelon left in the fridge.','I have been craving roasted sweet potatoes lately.','I just had a cup of warm soy milk.','The dumpling wrappers at this place are so thin.','I have been eating a lot of spicy food lately.','The tomato and egg stir-fry was great with rice.','There is still half a loaf of bread left.','I made myself a cup of tea this afternoon.','Grapes are really fresh this time of year.'],
 'weather':['It is pretty windy outside today.','It felt a little chilly when I went out this morning.','The afternoon sunshine is lovely.','A few raindrops just fell.','It has felt quite humid these past few days.','The forecast says it will cool down tonight.','The sky looks very overcast today.','The road is still wet after the rain.','It got much warmer once the sun came out.','The evening breeze feels nice.','The temperature has been changing a lot between morning and night.','The sky looks so clear today.','It feels more muggy than yesterday.','The leaves keep rustling outside.','This weather would be nice for a walk.'],
 'commute':['Traffic was heavier than usual today.','An empty seat opened up on the train a moment ago.','I passed a flower shop on the way back.','The bus came pretty quickly today.','It was really crowded during rush hour.','I waited at that red light for ages.','My bike ride home went smoothly.','I found a parking spot close to the entrance today.','I saw a little dog on my way home.','A new convenience store opened by the station.','I left home ten minutes earlier than usual.','A tree along the road is blooming.','Traffic moved very slowly on the overpass.','I got home just as the elevator arrived.','The streetlights came on pretty early today.'],
 'work':['I had two meetings this morning.','I finally finished organizing that spreadsheet.','I got more emails than usual today.','A coworker shared a really useful tip.','I have taken care of most of my tasks today.','The report only needs one last section.','We talked about the project over lunch.','Work moved pretty fast today.','I just filed those documents away.','The new coworker is easy to talk to.','An extra task came up this afternoon.','This week’s schedule is all mapped out.','Writing went more smoothly today.','I finally cleaned up my desktop.','I just got out of a pretty long discussion.'],
 'study':['I have been reading a book about history.','I just learned a new keyboard shortcut.','That article is quite interesting to read.','I remembered a few new words today.','The examples in the course feel very practical.','I just reorganized my notes.','It took me a while to understand one concept.','I want to review the basics again.','The illustrations in that book are lovely.','I had a very clear lesson today.','Time flew by while I was studying.','The problem gets easier from another angle.','I wrote one page of notes very carefully.','I came across an interesting science explanation.','The practice questions were easier than I expected.'],
 'shopping':['The fruit I just bought looks fresh.','I picked up some milk at the supermarket.','That shop has a new sign now.','The parcel is already in the pickup locker.','The clothes I ordered are darker than the photos.','I compared prices at a few shops before buying.','The market was not too busy today.','I just saw a pair of shoes that looked nice.','The package arrived earlier than expected.','There was quite a line at the checkout.','I bought enough household supplies for a while.','A little shop opened near the entrance.','The delivery packaging was really sturdy.','This cup feels nice in my hand.','They were playing an old song in the store.'],
 'sleep':['I slept more soundly than I have recently.','I woke up pretty early this morning.','I felt much better after my nap.','I have been having vivid dreams lately.','I woke up once in the middle of the night.','The new pillow feels all right.','My sleep schedule has been a bit irregular.','I woke up before the alarm this morning.','I started feeling sleepy pretty early today.','I finally slept in over the weekend.','I could hear the rain outside last night.','I lose track of time when I read before bed.','Getting up has not been as hard lately.','I had coffee this afternoon, so I may sleep later.','I dreamed about where I lived as a child.'],
 'hobby':['I have started listening to old songs again.','I watched a few episodes of a documentary this weekend.','I just finished building a small model.','I have been practicing drawing lately.','I am getting into making pour-over coffee.','I like looking through old travel photos in my spare time.','I watched an exciting game yesterday.','I am sorting through the photos I took.','I have started growing flowers lately.','I just finished a lighthearted novel.','I like listening to music on rainy days.','I have started up an old game again.','I would like to visit a new exhibition sometime.','The clouds I photographed today looked lovely.','I am trying out different kinds of bread.'],
 'family':['Someone in my family called today.','I am going to have a meal with my family this weekend.','My parents have been doing well lately.','The kid told me a funny story from school.','My sister sent me a travel photo.','Our family chat was full of old photos.','A relative just shared a recipe.','My family said the flowers in the yard are blooming.','My younger brother finally finished his exam.','Everyone happens to be free tonight.','My family remembers what I like to eat.','Grandma told a story from when she was young.','My cousin recently moved somewhere new.','Family dinner was especially lively.','Mom said the plant by the window has grown taller.'],
 'entertainment':['I just watched a slow-paced movie.','That actor delivered the lines very naturally.','There is a pretty relaxing variety show lately.','The end song sounds familiar.','The last few minutes of yesterday’s game were exciting.','The background music in this series is nice.','I just came across a funny clip.','The seaside scenery in the movie was beautiful.','The host tells stories in an interesting way.','I still remember that old cartoon.','The stage lighting at the show was lovely.','I just heard a song I used to play a lot.','The new documentary is very detailed.','That podcast talked a lot about travel.','The stage set looked really polished.'],
 'health':['I walked more than I did yesterday.','I felt better after drinking some water.','My shoulders have been a little sore lately.','I took a ten-minute walk after lunch.','I remembered to eat on time today.','My throat has felt a little dry lately.','I spent a while outside in the sunshine.','I have been exercising more than before.','I stretched my lower back a little.','My nose gets uncomfortable when the weather changes.','I remembered to bring my water bottle today.','I felt good after my walk.','I got up and moved around after sitting for a while.','I have been drinking less coffee at night.','I got some fresh air outside today.'],
 'friends':['A friend just sent me a funny picture.','I met up with an old classmate last week.','The restaurant my friend recommended was good.','We reminisced about an old trip a moment ago.','Someone shared a silly joke.','My friend recently started a new job.','We might have dinner together this weekend.','The old classmates’ group chat is lively today.','My friend said a new shop opened nearby.','I just got a really kind message.','Everyone agreed to go for a walk in the countryside sometime.','My friend showed me a photo of the sunset.','A coworker told me a funny story after work.','I talked with a neighbor for a few minutes yesterday.','Everyone made it to the reunion this time.'],
 'travel':['The coast looked beautiful on my last trip.','There are lots of little shops near the train station.','The train ride was pretty smooth.','One wheel on my suitcase is sticking.','I have not sorted through my travel photos yet.','The mountain air smelled so fresh.','I watched the scenery for ages on the long-distance bus.','The guesthouse had a very quiet courtyard.','I still remember my first flight.','There was a huge rice field along the way.','I saved the ticket on my phone.','The old town was lively at night.','Sunrise by the sea came earlier than I expected.','The people I met on the trip were very kind.','I brought just the right amount of stuff this time.'],
 'plants':['The mint by the window has new leaves.','A flower just opened on the balcony.','The pothos has been growing quickly.','The soil in that pot looks a little dry.','The leaves on the trees downstairs are turning yellow.','That succulent has gotten plumper this month.','I saw a cherry tree by the road today.','The new plant is still getting used to its spot.','The osmanthus near the building smells lovely.','The hydroponic plant on the desk has grown roots.','A few roses in the yard are blooming.','That leaf by the window has a vivid color.','The garden smells earthy after the rain.','A little weed has sprouted next to the pot.','I am not very familiar with that plant’s name.'],
 'hobbies_sports':['I watched some badminton today.','I noticed a new path while jogging.','There were lots of people on the court today.','I sleep better after playing sports lately.','I rode my bike along the river.','I worked out three times this week.','The final score was really close.','I heard birds while running.','I am learning a new swimming stroke.','I walked two laps around the park.','Stretching felt much easier today.','That team worked really well together.','The grip on my racket needs replacing.','I ran into a familiar neighbor during my morning walk.','I worked up a sweat today.'],
 'home_life':['The books on the table are finally tidy.','I moved the sofa cushions around.','Dinner cooking in the kitchen smells wonderful.','A bird landed on the railing outside.','It is especially quiet indoors today.','The new bedsheets feel really soft.','There is a new pair of shoes in the entryway.','My cup is back in its usual spot.','I sorted through some old magazines today.','There is a stripe of sunlight on the floor.','A plate of fruit is on the dining table.','The room smells fresh today.','I just heard someone walk down the hall.','The laundry I hung up is dry now.','The little clock at home keeps good time.'],
 'opinions':['I think it is fine to take things slowly.','Sometimes keeping things simple feels better.','Everyone likes different colors.','Familiar places always feel comforting.','It is nice to let your mind wander sometimes.','Taking care of small things can lift your mood.','I have been enjoying quiet mornings more and more.','Leaving some room in the schedule feels freeing.','Some old things are simply easier to use.','New experiences always bring a little surprise.','Finding a pace that suits you matters most.','It is interesting to hear about other people’s experiences.','There are lots of small joys in everyday life.','Good weather can lift your mood.','Taking time to sort out your thoughts helps.'],
 'memories':['I remember the walk home from school as a kid.','We used to sit in the courtyard on summer evenings.','That street in the old photos has changed so much.','One winter, the snow came down really thick.','Grandma’s pastries were my favorite as a child.','The place I used to live was close to school.','I remember taking the bus by myself for the first time.','The neighbor’s dog used to bark all the time.','I carved a few words into my old desk.','Summer vacation felt so long back then.','I just remembered a trip from a long time ago.','There was a big tree in front of the old house.','I used to take a detour to buy candy after school.','Everyone came to that birthday party.','I suddenly remembered a classmate I have not seen in years.'],
 'weekend':['I do not have much planned for the weekend.','I might find some time to walk in the park.','I want to sleep in this weekend.','Days off seem to go by faster than workdays.','I might check out the new bookstore on Saturday.','I am planning to cook at home on Sunday.','The weather looks good this weekend.','There are still a few days until the holiday.','I want to sort through my photos on my next day off.','Maybe I will get coffee with a friend this weekend.','I want to leave some free time for myself.','I am really looking forward to a full day off.','There is a market nearby this weekend.','I do not want to pack my day off too tightly.','I can finally slow down a little this week.'],
}

source=[json.loads(line) for line in SOURCE.read_text(encoding='utf-8').splitlines() if line]
assert len(source)==1000
records=[]
for row in source:
    text_id=row['text_id']
    if text_id.startswith('NRF-'):
        _,bucket,p,v=text_id.split('-'); p=int(p)-1; v=int(v)-1
        values,frames=EN[bucket]
        english=frames[p].format(x=values[v])
    else:
        _,bucket,idx=text_id.split('-'); idx=int(idx)-1
        english=CHAT_EN[bucket][(idx*7+len(bucket))%15]
        wrappers=[('',False),('I just remembered that ',True),('Speaking of that, ',False),('It suddenly occurred to me that ',True),('I noticed that ',True),('By the way, ',False),('Just now, ',False)]
        prefix,_=wrappers[idx%len(wrappers)]
        if prefix:
            clause=english if english.startswith('I ') else english[0].lower()+english[1:]
            english=prefix+clause
    records.append({'id':text_id,'中文':row['text'],'English':english})

def command_english(r, ordinal):
    target=r.get('target_en') or 'the device'
    a=r.get('action'); p=r.get('parameters',{})
    n=ordinal%4
    if a=='set_power':
        on=p['on']; verb=('turn on' if on else 'turn off')
        return [f'{verb.capitalize()} the {target}.',f'Please {verb} the {target}.',f'Switch {"on" if on else "off"} the {target}.',f'{target.title()}, {"on" if on else "off"} please.'][n]
    if a=='set_temperature':
        v=p.get('celsius',26)
        return [f'Set the {target} temperature to {v} degrees Celsius.',f'Set the temperature on the {target} to {v} degrees.',f'Please set {target} to {v} degrees.',f'{target.title()} temperature: {v} degrees.'][n]
    modes={'cool':'cooling','heat':'heating','dry':'dehumidifying','fan':'fan','auto':'automatic','sleep':'sleep','manual':'manual','eco':'eco','turbo':'turbo','natural':'natural breeze','normal':'normal','both':'vacuum and mop','vacuum':'vacuum only','mop':'mop only','on':'on','off':'off'}
    if a in ('set_mode','set_night_vision'):
        v=modes.get(p.get('mode'),p.get('mode','automatic'))
        return [f'Set the {target} to {v} mode.',f'Switch the {target} to {v} mode.',f'Please use {v} mode on the {target}.',f'{target.title()}, {v} mode.'][n]
    if a in ('set_brightness','set_volume','set_speed','set_suction','set_water_level','set_mist_level','set_intensity','set_ring_volume','set_target_humidity','set_humidity'):
        value=p.get('percent',p.get('level',p.get('on',p.get('target_humidity',50))))
        unit='percent' if 'percent' in p or 'volume' in a or 'brightness' in a or 'humidity' in a else ('on' if isinstance(value,bool) else 'level')
        return [f'Set the {target} to {value} {unit}.',f'Please set {target} at {value} {unit}.',f'Change the {target} setting to {value} {unit}.',f'{target.title()} setting: {value} {unit}.'][n]
    if a=='set_color_temperature': return f'Set the color temperature of the {target} to {p.get("kelvin")} kelvin.'
    if a=='set_color':
        colors={'#FF0000':'red','#00FF00':'green','#0000FF':'blue','#FFFF00':'yellow','#800080':'purple','#00FFFF':'cyan','#FFFFFF':'white','#000000':'black'}
        return f'Set the {target} to {colors.get(p.get("hex"),p.get("hex"))}.'
    if a=='adjust_brightness':
        delta=p.get('delta',0); word='brighter' if delta>0 else 'dimmer'
        return f'Make the {target} {abs(delta)} percentage points {word}.'
    if a=='set_channel_power':
        channel=p.get('channel','all'); where='all channels' if channel=='all' else f'channel {channel[2:]}'
        state='on' if p.get('on') else 'off'
        return f'Turn {state} {where} on the {target}.'
    if a=='set_fan_speed':
        value={'auto':'automatic','low':'low','medium':'medium','high':'high'}.get(p.get('level'),p.get('level'))
        return f'Set the fan speed on the {target} to {value}.'
    if a in ('set_fresh_air','set_privacy','set_recording','set_motion_alert','set_oscillation','set_vertical_swing','set_horizontal_swing','set_mute','set_light','set_ventilation','set_heating','set_output','set_screen'):
        value=next(iter(p.values()),False)
        state='on' if value is True else 'off' if value is False else str(value)
        if a=='set_mute': return f'{"Mute" if value else "Unmute"} the {target}.'
        noun={'set_fresh_air':'fresh-air mode','set_privacy':'privacy mode','set_recording':'recording','set_motion_alert':'motion alerts','set_oscillation':'oscillation','set_vertical_swing':'vertical swing','set_horizontal_swing':'horizontal swing','set_light':'the light','set_ventilation':'ventilation','set_heating':'heating','set_output':'power output','set_screen':'the screen'}[a]
        return f'Turn {state} {noun} on the {target}.'
    if a in ('set_position','set_duration','set_timer'):
        value=next(iter(p.values()),0); key=next(iter(p), '')
        unit={'open_percent':'percent open','height_percent':'percent height','minutes':'minutes'}[key] if key in ('open_percent','height_percent','minutes') else ''
        return f'Set the {target} to {value} {unit}.'
    if a=='dispense': return f'Dispense {p.get("portions")} portions from the {target}.'
    if a=='start_heating': return f'Heat for {p.get("seconds")} seconds at {p.get("power_percent")} percent power in the {target}.'
    if a=='set_fresh_air_level': return f'Set the fresh-air level on the {target} to {p.get("level")}.'
    if a=='get_last_measurement': return f'Check the latest measurement on the {target}.'
    if a in ('get_occupied','get_motion','get_pressed','get_smoke_detected'):
        return f'Check whether the {target} detected {a.removeprefix("get_").replace("_"," ") }.'
    if a in ('open','close','stop','pause','resume','next_track','previous_track','start_clean','return_to_dock','raise','lower','lock','unlock','start_self_clean','stop_self_clean','get_state','get_temperature','get_humidity','get_battery','get_clients','get_children','get_last_measurement','get_temperature','get_mist_level'):
        verbs={'open':'Open','close':'Close','stop':'Stop','pause':'Pause','resume':'Resume','next_track':'Play the next track on','previous_track':'Play the previous track on','start_clean':'Start cleaning with','return_to_dock':'Send back to the dock','raise':'Raise','lower':'Lower','lock':'Lock','unlock':'Unlock','start_self_clean':'Start self-cleaning on','stop_self_clean':'Stop self-cleaning on','get_state':'Check the status of','get_temperature':'Check the temperature of','get_humidity':'Check the humidity of','get_battery':'Check the battery level of','get_clients':'Check the connected devices for','get_children':'Check the connected devices for','get_last_measurement':'Check the latest measurement on'}
        return f'{verbs.get(a,"Use")} the {target}.'
    # General English fallback keeps a valid paired utterance for less common capabilities.
    return f'Please use the {target} to {a.replace("_"," ")}.'

commands=[json.loads(line) for line in COMMANDS.read_text(encoding='utf-8').splitlines() if line]
assert len(commands)==1932
for i,r in enumerate(commands):
    if r['expected_decision']=='needs_input':
        english='Please specify which room or light you mean.' if r['expected_reason']=='missing_room' else 'Which light do you mean?'
    elif r['expected_decision']=='ignore':
        english=f'Try an unsupported control request for the {r.get("target_en") or "device"}.'
    else:
        english=command_english(r,i)
    records.append({'id':r['id'],'中文':r['text'],'English':english})

assert len(records)==1000+len(commands)
assert len({r['id'] for r in records})==len(records)
assert all(r['中文'] and r['English'] for r in records)
assert sum(r['id'].startswith('NRF-') for r in records)==500
assert sum(r['id'].startswith('NRC-') for r in records)==500
with CSVOUT.open('w',encoding='utf-8-sig',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=['id','中文','English'],quoting=csv.QUOTE_MINIMAL)
    writer.writeheader(); writer.writerows(records)
lines=['# 静默语音助手全量测试指令中英表\n\n',f'共 {len(records)} 个唯一用例 ID，每个 ID 对应一条中文和一条英文，可分别送入 TTS。包括 500 条误触发负例、500 条日常闲聊，以及 {len(commands)} 条设备动作、逐设备口语变体、设备专属负例和分组指令。所有 ID 与[家庭模拟设备目录](simulated-home-device-catalog.md)中的例句一一对应；分组成员、预期动作和忽略标签在 [smart-home-command-cases-v1.jsonl](../testdata/smart-home-command-cases-v1.jsonl) 中。\n\n','可直接批量导入的 CSV：[all-voice-test-cases-v1.csv](../testdata/all-voice-test-cases-v1.csv)，UTF-8 with BOM 编码，表头为 `id,中文,English`。建议音频文件名分别使用 `{id}_zh-CN.wav` 与 `{id}_en-US.wav`。英文命令按标准答案生成同义 TTS 文本，不一定逐字翻译每个中文改写。\n\n','| id | 中文 | English |\n| --- | --- | --- |\n']
for r in records:
    zh=r['中文'].replace('|','\\|'); en=r['English'].replace('|','\\|')
    lines.append(f"| `{r['id']}` | {zh} | {en} |\n")
MDOUT.write_text(''.join(lines),encoding='utf-8')
print(json.dumps({'markdown':str(MDOUT),'csv':str(CSVOUT),'rows':len(records),'paired_ids':len({r['id'] for r in records}),'no_response_rows':len(source),'command_rows':len(commands)},ensure_ascii=False))
