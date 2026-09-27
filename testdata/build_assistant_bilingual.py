"""Build a reproducible, source-labelled bilingual audio evaluation starter.

Inputs are locally downloaded CATSLU test + qmeeus/SLURP test parquet files.
SLURP membership and labels are checked against the author's test.jsonl.
No corpus label is silently converted into a real-home execution instruction.
"""
from __future__ import annotations

import collections
import hashlib
import io
import json
from pathlib import Path
import re
import sys
import tarfile
import wave
import zipfile

ROOT = Path(__file__).resolve().parent
RAW = ROOT / "corpora" / "assistant-bilingual"
OUT = ROOT / "assistant-bilingual-v1"
sys.path.insert(0, str(RAW / "python-deps"))
import pyarrow.parquet as pq


def rank(value):
    return hashlib.sha256(("jev-bilingual-v1:" + str(value)).encode()).hexdigest()


def dump_lines(path, rows):
    path.write_text("".join(json.dumps(x, ensure_ascii=False) + "\n" for x in rows), encoding="utf-8")


def audio_info(data):
    if data[:4] == b"RIFF":
        with wave.open(io.BytesIO(data), "rb") as w:
            return {"sample_rate": w.getframerate(), "channels": w.getnchannels(), "duration_s": round(w.getnframes() / w.getframerate(), 4)}
    if data[:4] == b"fLaC" and data[4] & 127 == 0:
        info = int.from_bytes(data[18:26], "big")
        sample_rate = info >> 44
        samples = info & ((1 << 36) - 1)
        return {"sample_rate": sample_rate, "channels": ((info >> 41) & 7) + 1, "duration_s": round(samples / sample_rate, 4)}
    raise ValueError("Unexpected audio format")


def save_audio(record, data, extension):
    rel = f"audio/{record['id']}.{extension}"
    (OUT / rel).write_bytes(data)
    record.update(audio_path=rel, sha256=hashlib.sha256(data).hexdigest(), **audio_info(data))
    assert record["duration_s"] > 0


def base_record(identifier, language, dataset, source_id, text, labels, domain):
    return {"id": identifier, "language": language, "source_dataset": dataset,
            "source_split": "test", "source_record_id": source_id,
            "audio_kind": "human_recording", "audio_path": None,
            "reference_text": text, "source_domain": domain, "source_labels": labels,
            "product_expected": None,
            "review_status": "source_labels_preserved_product_mapping_pending"}


def load_catslu():
    records = []
    paths = {}
    with tarfile.open(RAW / "catslu_test.tar.gz", "r|gz") as archive:
        for member in archive:
            if member.name.endswith(".wav"):
                paths[(member.name.split("/")[2], Path(member.name).stem)] = member.name
            if not member.name.endswith("/test.json"):
                continue
            domain = member.name.split("/")[2]
            for dialogue in json.load(archive.extractfile(member)):
                for turn in dialogue["utterances"]:
                    source_id = turn["wav_id"]
                    record = base_record("zh-" + domain + "-" + source_id, "zh-CN", "CATSLU", source_id,
                                         turn["manual_transcript"], turn["semantic"], domain)
                    record.update(dialogue_id=dialogue["dlg_id"], turn_id=turn["utt_id"],
                                  recording_group=source_id,
                                  source_asr_1best=turn["asr_1best"])
                    records.append(record)
    assert len(records) == len(paths) == 6555, (len(records), len(paths))
    assert all((r["source_domain"], r["source_record_id"]) in paths for r in records)
    selected = []
    for domain in ("map", "music", "video", "weather"):
        previous_ids = {r["source_record_id"] for r in selected}
        eligible = [r for r in records if r["source_domain"] == domain and r["turn_id"] == 1
                    and r["source_record_id"] not in previous_ids
                    and r["source_labels"] and "(" not in r["reference_text"]]
        # Prefer diverse expressions, while retaining source dialogue identifiers.
        distinct = {}
        for record in sorted(eligible, key=lambda r: rank(r["id"])):
            distinct.setdefault(record["reference_text"], record)
        pool = list(distinct.values())
        assert len(pool) >= 125, (domain, len(pool))
        selected.extend(pool[:125])
    # Preserve several real alarm / volume utterances that the weather ontology
    # left unlabelled. Their suggested tasks are explicitly NOT source gold.
    special = []
    for record in records:
        text = record["reference_text"]
        if re.search(r"帮我定.*闹钟|声音再往大一点|音量调到六|音量调到最大|^声音大一点$", text):
            record["suggested_task_for_review"] = "alarm" if "闹钟" in text else "audio_control"
            record["review_status"] = "unlabelled_in_source_manual_task_review_required"
            special.append(record)
    assert len(special) == 6, len(special)
    selected = selected[:-len(special)] + special
    chosen = {(r["source_domain"], r["source_record_id"]): r for r in selected}
    with tarfile.open(RAW / "catslu_test.tar.gz", "r|gz") as archive:
        for member in archive:
            key = (member.name.split("/")[2], Path(member.name).stem) if len(member.name.split("/")) >= 3 else None
            if member.name.endswith(".wav") and key in chosen:
                save_audio(chosen[key], archive.extractfile(member).read(), "wav")
    for record in records:
        record["source_container"] = "catslu_test.tar.gz"
        record["source_member"] = paths[(record["source_domain"], record["source_record_id"])]
    return records, selected


def en_bucket(intent):
    if intent.startswith("iot_") or intent in {"cleaning", "hue_lightup", "hue_lightoff", "hue_lightdim", "wemo_off"}:
        return "home_control"
    if intent.startswith("alarm_"):
        return "alarm"
    if intent.startswith("audio_volume_"):
        return "audio_control"
    if intent.startswith("general_") or intent in {"joke", "quirky"}:
        return "general_assistant"
    return "other_assistant"


def load_slurp():
    original = [json.loads(line) for line in (RAW / "slurp-test.jsonl").read_text(encoding="utf-8").splitlines()]
    lookup = {row["slurp_id"]: row for row in original}
    expected_pairs = {(r["slurp_id"], a["file"]) for r in original for a in r["recordings"]}
    selected_utterances = []
    for bucket, quota in [("home_control", 9999), ("alarm", 9999), ("audio_control", 9999), ("general_assistant", 60)]:
        pool = sorted([r for r in original if en_bucket(r["intent"]) == bucket], key=lambda r: rank(r["slurp_id"]))
        selected_utterances.extend(pool[:quota])
    remaining = 500 - len(selected_utterances)
    assert remaining >= 0
    selected_utterances.extend(sorted([r for r in original if en_bucket(r["intent"]) == "other_assistant"], key=lambda r: rank(r["slurp_id"]))[:remaining])
    chosen_pairs = set()
    for row in selected_utterances:
        recordings = sorted(row["recordings"], key=lambda r: (r["ent_wer"] != 0, r["wer"] != 0, "-headset" in r["file"], rank(r["file"])))
        chosen_pairs.add((row["slurp_id"], recordings[0]["file"]))
    records, selected, seen = [], [], set()
    for part in range(2):
        filename = f"slurp-test-{part}.parquet"
        parquet = pq.ParquetFile(RAW / filename)
        names = json.loads(parquet.schema_arrow.metadata[b"huggingface"])["info"]["features"]["intent"]["names"]
        for batch in parquet.iter_batches(batch_size=128):
            for row in batch.to_pylist():
                source = lookup[row["slurp_id"]]
                audio_name = Path(row["audio"]["path"]).name
                pair = (row["slurp_id"], audio_name)
                assert pair in expected_pairs and pair not in seen, pair
                assert row["sentence"] == source["sentence"], pair
                assert names[row["intent"]] == source["intent"], pair
                seen.add(pair)
                record = base_record("en-" + str(row["slurp_id"]) + "-" + Path(audio_name).stem,
                                     "en", "SLURP", str(row["slurp_id"]), source["sentence"],
                                     {"intent": source["intent"], "action": source["action"],
                                      "entities": source["entities"], "sentence_annotation": source["sentence_annotation"]}, source["scenario"])
                record.update(task_bucket=en_bucket(source["intent"]), source_container=filename,
                              source_audio_file=audio_name, recording_group=str(row["slurp_id"]))
                if pair in chosen_pairs:
                    save_audio(record, row["audio"]["bytes"], "flac")
                    selected.append(record)
                records.append(record)
    assert seen == expected_pairs
    assert len(records) == 13078 and len(selected) == 500
    return records, selected


def main():
    (OUT / "audio").mkdir(parents=True, exist_ok=True)
    zh_all, zh = load_catslu()
    en_all, en = load_slurp()
    selected = zh + en
    assert len(selected) == 1000 and all(r["audio_path"] for r in selected)
    assert len({r["id"] for r in selected}) == len(selected)
    assert len({r["sha256"] for r in selected}) == len(selected)
    dump_lines(OUT / "samples.jsonl", selected)
    dump_lines(OUT / "full-source-inventory.jsonl", zh_all + en_all)
    stats = {"source_records": {"CATSLU": len(zh_all), "SLURP": len(en_all)},
             "selected": {"zh-CN": len(zh), "en": len(en)},
             "english_task_buckets": dict(collections.Counter(r["task_bucket"] for r in en)),
             "chinese_source_domains": dict(collections.Counter(r["source_domain"] for r in zh)),
             "selected_duration_minutes": round(sum(r["duration_s"] for r in selected) / 60, 2),
             "mirror_metadata_verified_against_official_test": len(en_all),
             "chinese_unique_source_audio_ids": len({r["source_record_id"] for r in zh_all}),
             "human_listening_reviewed": 0,
             "product_execution_labels_completed": 0}
    (OUT / "stats.json").write_text(json.dumps(stats, ensure_ascii=False, indent=2), encoding="utf-8")
    template = (ROOT / "bilingual-catalog-template.html").read_text(encoding="utf-8")
    payload = json.dumps(selected, ensure_ascii=False).replace("<", "\\u003c")
    (OUT / "试听与标注.html").write_text(template.replace("__SAMPLES_JSON__", payload), encoding="utf-8")
    (OUT / "README.md").write_text((ROOT / "bilingual-dataset-README.md").read_text(encoding="utf-8"), encoding="utf-8")
    (OUT / "SLURP-LICENSE.txt").write_bytes((RAW / "slurp-LICENSE.txt").read_bytes())
    zip_path = ROOT / "assistant-bilingual-v1.zip"
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=3) as archive:
        for path in sorted(OUT.rglob("*")):
            if path.is_file():
                archive.write(path, Path(OUT.name) / path.relative_to(OUT))
    with zipfile.ZipFile(zip_path) as archive:
        assert archive.testzip() is None
    print(json.dumps(stats, ensure_ascii=False, indent=2))
    print("ZIP", zip_path, zip_path.stat().st_size)


if __name__ == "__main__":
    main()
