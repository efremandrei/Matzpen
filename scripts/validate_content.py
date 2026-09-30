from __future__ import annotations

import hashlib
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / "app/src/main/assets/content.json"
feed = root / "docs/feed"
data = json.loads(source.read_text(encoding="utf-8"))
assert data["schemaVersion"] == 1 and data["electionId"] == "il-knesset-26"
assert len(data["questions"]) == 18
assert len(data["lists"]) == 38
assert len(data["positions"]) == 265
assert all(q["he"] and q["en"] and q["ar"] for q in data["questions"])
assert all(p["sourceUrl"].startswith("https://") for p in data["positions"])
assert len({(p["listId"], p["questionId"]) for p in data["positions"]}) == len(data["positions"])
assert data["attribution"]["license"] == "CC BY 4.0"
manifest = json.loads((feed / "manifest.json").read_text(encoding="utf-8"))
assert manifest["revision"] == data["revision"]
assert manifest["sha256"] == hashlib.sha256((feed / "content.json").read_bytes()).hexdigest()
assert source.read_bytes() == (feed / "content.json").read_bytes()
subprocess.run([
    r"C:\Program Files\Git\usr\bin\openssl.exe", "dgst", "-sha256", "-verify",
    str(root / "feed-public.pem"), "-signature", str(feed / "manifest.sig"), str(feed / "manifest.json")
], check=True)
print("Validated 38 lists, 18 questions, 265 sourced positions, and signed feed")
