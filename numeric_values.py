"""Extract bounded-choice numbers without changing the user's original wording."""
import re


def numeric_candidates(text):
    digits = dict(zip("零一二三四五六七八九", "0123456789"))
    digits.update({"〇": "0", "两": "2"})
    units = {"十": 10, "百": 100, "千": 1000, "万": 10000}
    result = []
    for raw in re.findall(r"[负-]?[0-9零〇一二两三四五六七八九十百千万]+(?:[点.][0-9零〇一二两三四五六七八九]+)?", text.replace("百分之", "")):
        negative = raw.startswith(("负", "-"))
        parts = re.split(r"[点.]", raw.lstrip("负-"))
        whole = parts[0]
        if not any(c in units for c in whole):
            value = int("".join(digits.get(c, c) for c in whole))
        else:
            total, section, pending = 0, 0, ""
            for c in whole:
                if c in units:
                    if units[c] == 10000:
                        total += (section + int(pending or "0")) * 10000
                        section = 0
                    else:
                        section += int(pending or "1") * units[c]
                    pending = ""
                else:
                    pending += digits.get(c, c)
            value = total + section + int(pending or "0")
        if len(parts) > 1:
            value += float("0." + "".join(digits.get(c, c) for c in parts[1]))
        if negative:
            value = -value
        value = str(int(value)) if float(value).is_integer() else str(value)
        if value not in result:
            result.append(value)
    return result
