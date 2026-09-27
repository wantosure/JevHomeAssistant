"""Extract a small, review-required negative set from a local ST-CMDS archive.

The source archive may be an incomplete prefix. Original WAV bytes are kept
unchanged and source IDs/transcripts are recorded for provenance.
"""

import csv
import json
import tarfile
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ARCHIVE = ROOT / "corpora" / "ST-CMDS-20170001_1-OS.tar.gz"
SCAN = ROOT / "corpora" / "st-cmds-scan.json"
OUT_DIR = ROOT / "audio" / "public-stcmds"
MANIFEST = ROOT / "public-stcmds-candidates.csv"

SELECTED = [
    "20170001P00287I0113",  # 刚才电脑卡着了
    "20170001P00061A0096",  # 为什么笔记本电脑嗡嗡的响
    "20170001P00313I0020",  # 电视里在放勇敢者的游戏
    "20170001P00021I0032",  # 等我起床开电脑
    "20170001P00001I0037",  # 听着人家的空调嗡嗡地转
    "20170001P00153A0044",  # 你不会在看电视又或喝酒吧
    "20170001P00094A0016",  # 什么叫条件不允许看电视啊
    "20170001P00174A0088",  # 到韩雪芹家她家有电脑儿
    "20170001P00031I0066",  # 拿我的铺盖和电脑而已
    "20170001P00032I0077",  # 你妈不准你玩儿电脑所
    "20170001P00129A0057",  # 然后再床上玩电脑
    "20170001P00185A0109",  # 第一个黄灯左拐就是副座
]


def main() -> None:
    scan = json.loads(SCAN.read_text(encoding="utf-8"))
    text_by_id = {item["source_id"]: item["text"] for item in scan["related"]}
    missing_text = set(SELECTED) - text_by_id.keys()
    if missing_text:
        raise ValueError(f"Missing source transcripts: {sorted(missing_text)}")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    found: set[str] = set()
    try:
        with tarfile.open(ARCHIVE, "r|gz") as archive:
            for member in archive:
                stem = Path(member.name).stem
                if stem not in SELECTED or Path(member.name).suffix.lower() != ".wav":
                    continue
                stream = archive.extractfile(member)
                if stream is None:
                    continue
                (OUT_DIR / f"{stem}.wav").write_bytes(stream.read())
                found.add(stem)
                if len(found) == len(SELECTED):
                    break
    except (EOFError, OSError, tarfile.ReadError) as exc:
        print(f"Partial archive reached: {exc}")

    rows = []
    for source_id in SELECTED:
        if source_id not in found:
            continue
        path = OUT_DIR / f"{source_id}.wav"
        with wave.open(str(path), "rb") as audio:
            channels = audio.getnchannels()
            sample_rate = audio.getframerate()
            duration = audio.getnframes() / sample_rate
        rows.append({
            "source_dataset": "ST-CMDS-20170001_1",
            "source_id": source_id,
            "reference_text": text_by_id[source_id],
            "audio_path": path.relative_to(ROOT).as_posix(),
            "duration_s": f"{duration:.3f}",
            "sample_rate": sample_rate,
            "channels": channels,
            "candidate_decision": "ignore",
            "review_status": "needs_listening_and_label_review",
        })
    with MANIFEST.open("w", encoding="utf-8-sig", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=rows[0].keys() if rows else [])
        if rows:
            writer.writeheader()
            writer.writerows(rows)
    print(f"extracted={len(found)}/{len(SELECTED)} manifest={MANIFEST}")
    for row in rows:
        print(f"{row['source_id']} {row['duration_s']}s {row['reference_text']}")


if __name__ == "__main__":
    main()
