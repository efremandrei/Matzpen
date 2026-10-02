"""Check that every current screen and snapshot item has Russian wording."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ui_source = (ROOT / "app/src/main/java/com/efremandrei/matzpen/MainActivity.kt").read_text(encoding="utf-8")
translations = (ROOT / "app/src/main/java/com/efremandrei/matzpen/RussianText.kt").read_text(encoding="utf-8")
content = json.loads((ROOT / "app/src/main/assets/content.json").read_text(encoding="utf-8"))


def mapping(name: str) -> set[str]:
    section = re.search(rf"\b(?:val|private val) {name} = mapOf\((.*?)\n    \)", translations, re.S)
    assert section, f"Missing Russian {name} map"
    pairs = re.findall(r'"([^"]+)" to "([^"]+)"', section.group(1))
    keys = [key for key, _ in pairs]
    assert len(keys) == len(set(keys)), f"Duplicate Russian {name} keys"
    assert all(re.search("[А-Яа-яЁё]", value) for _, value in pairs), f"Untranslated {name} value"
    return set(keys)


assert mapping("questions") == {item["id"] for item in content["questions"]}
assert mapping("lists") == {item["id"] for item in content["lists"]}
ui_keys = mapping("uiStrings")


def arguments(start: int) -> list[str]:
    args: list[str] = []
    chars: list[str] = []
    quoted = escaped = False
    parentheses = brackets = braces = 0
    index = start
    while index < len(ui_source):
        char = ui_source[index]
        if quoted:
            if char == "$" and ui_source[index:index + 2] == "${":
                end = index + 2
                depth = 1
                inner_quoted = inner_escaped = False
                while depth:
                    current = ui_source[end]
                    if inner_quoted:
                        if inner_escaped:
                            inner_escaped = False
                        elif current == "\\":
                            inner_escaped = True
                        elif current == '"':
                            inner_quoted = False
                    elif current == '"':
                        inner_quoted = True
                    elif current == '{':
                        depth += 1
                    elif current == '}':
                        depth -= 1
                    end += 1
                chars.append(ui_source[index:end])
                index = end
                continue
            chars.append(char)
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
            chars.append(char)
        elif char == '(':
            parentheses += 1
            chars.append(char)
        elif char == ')':
            if parentheses == brackets == braces == 0:
                args.append("".join(chars).strip())
                return args
            parentheses -= 1
            chars.append(char)
        elif char == '[':
            brackets += 1
            chars.append(char)
        elif char == ']':
            brackets -= 1
            chars.append(char)
        elif char == '{':
            braces += 1
            chars.append(char)
        elif char == '}':
            braces -= 1
            chars.append(char)
        elif char == ',' and parentheses == brackets == braces == 0:
            args.append("".join(chars).strip())
            chars = []
        else:
            chars.append(char)
        index += 1
    raise AssertionError("Unclosed tr call")


checked = 0
for match in re.finditer(r"\btr\(", ui_source):
    args = arguments(match.end())
    if args[0] == "en: String":  # Function declaration.
        continue
    line = ui_source.count("\n", 0, match.start()) + 1
    english = args[0]
    if "$" in english:
        localized_depth = len(args) >= 4 and args[3] == english and "depthName(" in english
        assert len(args) >= 4 and (re.search("[А-Яа-яЁё]", args[3]) or localized_depth), f"Missing dynamic Russian copy at line {line}: {args}"
    else:
        assert len(args) >= 4 or english[1:-1] in ui_keys, f"Missing Russian UI copy at line {line}: {english}"
    checked += 1

print(f"Validated Russian copy for {checked} UI calls, 18 questions, and 38 lists")
