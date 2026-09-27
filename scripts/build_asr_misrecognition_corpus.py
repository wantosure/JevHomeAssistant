#!/usr/bin/env python3
"""Build ASR-confusion regression cases from executable Chinese home commands."""

from __future__ import annotations

import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "testdata" / "smart-home-command-cases-v1.jsonl"
JSONL = ROOT / "testdata" / "asr-misrecognition-v1.jsonl"
CSV = ROOT / "testdata" / "asr-misrecognition-v1.csv"

# Deliberately small, reviewable lexicon: frequent Mandarin homophones and
# near-homophones that a recognizer may emit in home-control utterances.
# Longer phrases are checked first so (for example) 吸顶灯 is one confusion.
RULES = [
    ("电视遥控开关", "点事遥控开关", "near_homophone"),
    ("小工具", "小工局", "near_homophone"),
    ("扫地机器人", "扫地机人", "word_boundary"),
    ("充电宝", "冲电宝", "homophone"),
    ("体脂秤", "体脂称", "homophone"),
    ("水暖毯", "水暖谈", "homophone"),
    ("浴霸", "浴吧", "homophone"),
    ("吸顶灯", "吸顶等", "homophone"),
    ("床头灯", "床头等", "homophone"),
    ("筒灯", "筒等", "homophone"),
    ("射灯", "射等", "homophone"),
    ("电视", "点事", "near_homophone"),
    ("空调", "空条", "homophone"),
    ("窗帘", "窗连", "homophone"),
    ("风扇", "风散", "homophone"),
    ("加湿器", "加湿气", "homophone"),
    ("净化器", "净化气", "homophone"),
    ("除湿", "厨湿", "near_homophone"),
    ("制冷", "治冷", "homophone"),
    ("制热", "治热", "homophone"),
    ("新风", "心风", "homophone"),
    ("门锁", "门所", "homophone"),
    ("门铃", "门玲", "homophone"),
    ("遥控", "摇控", "homophone"),
    ("开关", "开观", "homophone"),
    ("插座", "插坐", "homophone"),
    ("音箱", "音香", "homophone"),
    ("灯组", "等组", "homophone"),
    ("灯", "等", "homophone"),
    ("照明", "照鸣", "homophone"),
    ("亮度", "量度", "homophone"),
    ("色温", "色文", "homophone"),
    ("温度", "文度", "homophone"),
    ("湿度", "失度", "homophone"),
    ("客厅", "客停", "near_homophone"),
    ("主卧", "猪窝", "near_homophone"),
    ("次卧", "刺窝", "near_homophone"),
    ("卧室", "我是", "homophone"),
    ("厨房", "厨方", "near_homophone"),
    ("餐厅", "餐停", "near_homophone"),
    ("阳台", "洋台", "homophone"),
    ("菜地", "彩地", "homophone"),
    ("门厅", "门停", "near_homophone"),
    ("打开", "打凯", "near_homophone"),
    ("关掉", "关到", "near_homophone"),
    ("关闭", "关必", "near_homophone"),
    ("设置", "四只", "homophone"),
    ("调到", "掉到", "homophone"),
    ("调整", "掉整", "near_homophone"),
    ("查询", "查寻", "homophone"),
    ("状态", "姿态", "homophone"),
    ("开启", "开起", "near_homophone"),
    ("关", "观", "homophone"),
    ("开", "凯", "homophone"),
    ("温", "文", "homophone"),
    ("风", "封", "homophone"),
    ("机", "鸡", "homophone"),
    ("器", "气", "homophone"),
    ("度", "渡", "homophone"),
    ("的", "地", "homophone"),
    ("把", "八", "homophone"),
    ("了", "啦", "near_homophone"),
    ("灯", "登", "homophone"),
    ("明", "鸣", "homophone"),
    ("设", "社", "homophone"),
    ("数", "树", "homophone"),
    ("档", "挡", "homophone"),
    ("音", "因", "homophone"),
    ("箱", "香", "homophone"),
    ("帘", "连", "homophone"),
    ("扇", "善", "homophone"),
    ("锁", "所", "homophone"),
    ("查", "茶", "homophone"),
    ("量", "亮", "homophone"),
    ("高", "搞", "homophone"),
    ("低", "滴", "homophone"),
    ("色", "涩", "homophone"),
]


def read_cases() -> list[dict]:
    with SOURCE.open(encoding="utf-8") as f:
        return [json.loads(line) for line in f if line.strip()]


def confuse(text: str) -> tuple[str, dict[str, str]]:
    for original, substitute, kind in RULES:
        if original in text:
            return text.replace(original, substitute, 1), {
                "type": kind,
                "original_span": original,
                "recognized_span": substitute,
            }
    raise ValueError(f"No reviewed confusion rule applies: {text}")


def main() -> None:
    source_cases = [r for r in read_cases() if r["expected_decision"] == "execute"]
    output = []
    for source in source_cases:
        recognized, error = confuse(source["text"])
        output.append(
            {
                "id": f"ASR-{source['id']}-01",
                "source_id": source["id"],
                "language": "zh-CN",
                "reference_text": source["text"],
                "asr_text": recognized,
                "tts_text": source["text"],
                "error": error,
                "evaluation_stage": "injected_asr_transcript_to_jev",
                "expected_decision": "execute",
                "expected_reason": source["expected_reason"],
                "target_id": source["target_id"],
                "target_en": source.get("target_en"),
                "action": source["action"],
                "parameters": source["parameters"],
                "registry_version": "sim-home-v1",
            }
        )

    ids = [r["id"] for r in output]
    assert len(output) == len(source_cases)
    assert len(ids) == len(set(ids))
    assert all(r["reference_text"] != r["asr_text"] for r in output)
    assert all(r["source_id"] in {s["id"] for s in source_cases} for r in output)

    JSONL.write_text(
        "".join(json.dumps(row, ensure_ascii=False) + "\n" for row in output),
        encoding="utf-8",
    )
    with CSV.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=[
                "id",
                "source_id",
                "reference_text",
                "asr_text",
                "tts_text",
                "error_type",
                "error_original_span",
                "error_recognized_span",
                "expected_decision",
                "target_id",
                "action",
                "parameters_json",
            ],
        )
        writer.writeheader()
        for row in output:
            writer.writerow(
                {
                    "id": row["id"],
                    "source_id": row["source_id"],
                    "reference_text": row["reference_text"],
                    "asr_text": row["asr_text"],
                    "tts_text": row["tts_text"],
                    "error_type": row["error"]["type"],
                    "error_original_span": row["error"]["original_span"],
                    "error_recognized_span": row["error"]["recognized_span"],
                    "expected_decision": row["expected_decision"],
                    "target_id": row["target_id"],
                    "action": row["action"],
                    "parameters_json": json.dumps(
                        row["parameters"], ensure_ascii=False, separators=(",", ":")
                    ),
                }
            )

    print(
        json.dumps(
            {
                "source_executable_cases": len(source_cases),
                "asr_confusion_cases": len(output),
                "jsonl": str(JSONL),
                "csv": str(CSV),
                "example": next(
                    r for r in output if r["reference_text"] == "关闭客厅空调"
                ),
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
