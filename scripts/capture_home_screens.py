"""Capture the same home screen in each supported language on an emulator."""
from __future__ import annotations

import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ADB = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "AppData/Local/Android/Sdk"))) / "platform-tools/adb.exe"
LABELS = {"he": "עברית", "ar": "العربية", "en": "English", "ru": "Русский"}


def adb(*args: str) -> None:
    subprocess.run([str(ADB), *args], check=True, stdout=subprocess.DEVNULL)


def nodes() -> list[ET.Element]:
    adb("shell", "uiautomator", "dump", "/sdcard/matzpen-ui.xml")
    adb("pull", "/sdcard/matzpen-ui.xml", str(ROOT / "build-matzpen-ui.xml"))
    return list(ET.parse(ROOT / "build-matzpen-ui.xml").iter("node"))


def tap(node: ET.Element) -> None:
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


for code, label in LABELS.items():
    current = next(n for n in nodes() if n.attrib.get("class") == "android.widget.Button" and n.attrib.get("text") in LABELS.values())
    tap(current)
    choice = next(n for n in nodes() if n.attrib.get("text") == label)
    tap(choice)
    time.sleep(0.6)
    assert any(n.attrib.get("class") == "android.widget.Button" and n.attrib.get("text") == label for n in nodes())
    adb("shell", "screencap", "-p", "/sdcard/matzpen-home.png")
    adb("pull", "/sdcard/matzpen-home.png", str(ROOT / f"docs/screenshots/home-{code}.png"))
    print(f"Captured {code}")

(ROOT / "build-matzpen-ui.xml").unlink(missing_ok=True)
