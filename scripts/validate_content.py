from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / "app/src/main/assets/content.json"
feed = root / "docs/feed"
data = json.loads(source.read_text(encoding="utf-8"))
assert data["schemaVersion"] == 1 and data["electionId"] == "il-knesset-26"
assert len(data["questions"]) == 50
assert len(data["lists"]) == 38
assert len(data["positions"]) == 351
assert all(q["he"] and q["en"] and q["ar"] and (q.get("ru") or q["id"] in {
    "palestinian-state", "death-penalty", "haredi-draft", "judicial-reform",
    "oct7-inquiry", "religion-state", "gaza-control", "lower-taxes",
    "yeshiva-budgets", "price-controls", "temple-mount", "settlements",
    "term-limits", "gaza-resettlement", "food-imports", "lgbt-equality",
    "saudi-normalization", "universal-service"
}) for q in data["questions"])
assert all(p["sourceUrl"].startswith("https://") for p in data["positions"])
assert len({(p["listId"], p["questionId"]) for p in data["positions"]}) == len(data["positions"])
assert data["attribution"]["license"] == "CC BY 4.0"
assert len(data["profiles"]) == 38
assert sum(len(profile["bullets"]) for profile in data["profiles"]) == 122
assert all(b["sourceUrl"].startswith("https://") for profile in data["profiles"] for b in profile["bullets"])
assert {p["listId"] for p in data["profiles"]} == {p["id"] for p in data["lists"]}
assert all(any(p["questionId"] == q["id"] for p in data["positions"]) for q in data["questions"])
manifest = json.loads((feed / "manifest.json").read_text(encoding="utf-8"))
assert manifest["revision"] == data["revision"]
assert manifest["sha256"] == hashlib.sha256((feed / "content.json").read_bytes()).hexdigest()
assert source.read_bytes() == (feed / "content.json").read_bytes()
openssl = shutil.which("openssl") or r"C:\Program Files\Git\usr\bin\openssl.exe"
subprocess.run([
    openssl, "dgst", "-sha256", "-verify",
    str(root / "feed-public.pem"), "-signature", str(feed / "manifest.sig"), str(feed / "manifest.json")
], check=True)
print("Validated 38 lists, 50 questions, 351 sourced positions, 122 profile bullets, and signed feed")
