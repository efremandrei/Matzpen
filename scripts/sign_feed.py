"""Publish a signed static feed for GitHub Pages.

The private signing key stays outside Git. Set MATZPEN_FEED_KEY to its path.
"""
from __future__ import annotations

import hashlib
import json
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app" / "src" / "main" / "assets" / "content.json"
DEST = ROOT / "docs" / "feed"


def main() -> None:
    key = Path(os.environ.get("MATZPEN_FEED_KEY", str(Path.home() / ".android" / "keystores" / "matzpen-feed-private.pem")))
    if not key.is_file():
        raise SystemExit(f"Feed signing key not found: {key}")
    content = SOURCE.read_bytes()
    data = json.loads(content)
    manifest = {
        "schemaVersion": 1,
        "electionId": "il-knesset-26",
        "revision": data["revision"],
        "publishedAt": data["updatedAt"],
        "sha256": hashlib.sha256(content).hexdigest(),
    }
    DEST.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SOURCE, DEST / "content.json")
    (DEST / "manifest.json").write_bytes((json.dumps(manifest, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8"))
    subprocess.run([
        r"C:\Program Files\Git\usr\bin\openssl.exe", "dgst", "-sha256", "-sign", str(key),
        "-out", str(DEST / "manifest.sig"), str(DEST / "manifest.json")
    ], check=True)
    subprocess.run([
        r"C:\Program Files\Git\usr\bin\openssl.exe", "dgst", "-sha256", "-verify",
        str(ROOT / "feed-public.pem"), "-signature", str(DEST / "manifest.sig"), str(DEST / "manifest.json")
    ], check=True)
    print(f"Signed revision {data['revision']} with {manifest['sha256']}")


if __name__ == "__main__":
    main()
