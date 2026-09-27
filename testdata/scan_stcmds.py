"""Scan a local ST-CMDS tar.gz, including a partially downloaded prefix.

This reads metadata only; it does not extract or relabel audio.
"""

import json
import re
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ARCHIVE = ROOT / "corpora" / "ST-CMDS-20170001_1-OS.tar.gz"
CASES = [json.loads(line) for line in (ROOT / "voice-cases-starter.jsonl").read_text(encoding="utf-8").splitlines()]
OUT = ROOT / "corpora" / "st-cmds-scan.json"
KEYWORDS = re.compile(r"灯|窗帘|闹钟|电脑|空调|风扇|音量|提醒|开关|电视|加湿器|明天.*[七7]点")


def normalize(text: str) -> str:
    return re.sub(r"[\s，。！？、,.!?；;：:]", "", text)


def main() -> None:
    wanted = {normalize(row["reference_text"]): row["id"] for row in CASES}
    wav_ids: set[str] = set()
    txt_by_id: dict[str, str] = {}
    related: list[dict[str, str]] = []
    count = 0
    complete = False
    try:
        with tarfile.open(ARCHIVE, "r|gz") as archive:
            for member in archive:
                count += 1
                suffix = Path(member.name).suffix.lower()
                stem = Path(member.name).stem
                if suffix == ".wav":
                    wav_ids.add(stem)
                elif suffix == ".txt" and member.isfile():
                    raw = archive.extractfile(member)
                    if raw is None:
                        continue
                    transcription = raw.read().decode("utf-8-sig", errors="replace").strip()
                    txt_by_id[stem] = transcription
                    if KEYWORDS.search(transcription):
                        related.append({"source_id": stem, "text": transcription})
        complete = True
    except (EOFError, OSError, tarfile.ReadError) as exc:
        print(f"Partial archive reached: {exc}")

    exact = [
        {"case_id": wanted[normalize(text)], "source_id": stem, "text": text, "wav_seen": stem in wav_ids}
        for stem, text in txt_by_id.items()
        if normalize(text) in wanted
    ]
    for row in related:
        row["wav_seen"] = row["source_id"] in wav_ids
    pairs = sum(stem in wav_ids for stem in txt_by_id)
    result = {
        "archive_complete": complete,
        "archive_bytes_at_scan": ARCHIVE.stat().st_size,
        "members_seen": count,
        "transcripts_seen": len(txt_by_id),
        "wavs_seen": len(wav_ids),
        "complete_pairs_seen": pairs,
        "exact_matches": exact,
        "related": related,
    }
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"members={count} transcripts={len(txt_by_id)} wavs={len(wav_ids)} pairs={pairs} exact={len(exact)} related={len(related)} related_pairs={sum(row['wav_seen'] for row in related)} complete={complete}")
    for row in exact[:20]:
        print("EXACT", row)
    for row in related[:25]:
        print("RELATED", row)


if __name__ == "__main__":
    main()
