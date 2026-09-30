"""Build the app's attributed, reviewable election snapshot from a pinned open dataset.

Run only when intentionally updating election content. No voter data is involved.
"""
from __future__ import annotations

import json
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM_SHA = "4b304f9983f981992ecaf5a66cfc597155745d85"
BASE = f"https://raw.githubusercontent.com/dangelm/bhirot26-election-data/{UPSTREAM_SHA}/data/"


def fetch(name: str) -> dict:
    with urllib.request.urlopen(BASE + name, timeout=30) as response:
        return json.load(response)


# Keep the original Hebrew wording for source fidelity. The two coalition-strategy
# issues in the upstream set are excluded from policy-agreement scoring.
EN = [
    "Should a Palestinian state be established alongside Israel as part of a future agreement?",
    "Should Israel legislate the death penalty for terrorists?",
    "Should a law require Haredi yeshiva students to serve, with personal sanctions for avoiding service?",
    "Should the Supreme Court's powers be limited and the Knesset and government strengthened, as proposed in the judicial reform?",
    "Should a state commission of inquiry into the events of October 7 be established immediately?",
    "Should public transport run on Shabbat and civil marriage be introduced in Israel?",
    "Should the IDF retain security control in Gaza in the coming years?",
    "Should taxes be lowered even if the state provides fewer public services?",
    "Should funding for yeshivas and Haredi schools be increased?",
    "Should government price controls on basic food products be expanded?",
    "Should Jewish prayer be allowed on the Temple Mount, even if that changes the status quo?",
    "Should settlements in the West Bank be expanded?",
    "Should the law limit how many years a prime minister may serve?",
    "Should Jewish settlements in Gaza be re-established?",
    "Should food imports be opened and tariffs reduced, even if Israeli agriculture is harmed?",
    "Should full equal rights for LGBTQ people, including recognition of partnerships, be enshrined in law?",
    "Should Israel make concessions to Palestinians, such as freezing settlement building, to reach a peace agreement with Saudi Arabia?",
    "Should military or civilian service be mandatory for all Israeli citizens, including Haredim and Arabs?",
]

AR = [
    "هل ينبغي إقامة دولة فلسطينية إلى جانب إسرائيل ضمن اتفاق مستقبلي؟",
    "هل ينبغي أن تسنّ إسرائيل قانونًا يفرض عقوبة الإعدام على منفذي العمليات الإرهابية؟",
    "هل ينبغي سنّ قانون يُلزم طلاب المعاهد الدينية الحريدية بالخدمة، مع عقوبات شخصية على المتهربين؟",
    "هل ينبغي تقليص صلاحيات المحكمة العليا وتعزيز صلاحيات الكنيست والحكومة، وفقًا للإصلاح القضائي المقترح؟",
    "هل ينبغي إنشاء لجنة تحقيق رسمية في أحداث السابع من أكتوبر فورًا؟",
    "هل ينبغي تشغيل المواصلات العامة يوم السبت وإقرار الزواج المدني في إسرائيل؟",
    "هل ينبغي أن يحتفظ الجيش الإسرائيلي بالسيطرة الأمنية في غزة خلال السنوات القادمة؟",
    "هل ينبغي خفض الضرائب حتى لو أدى ذلك إلى تقليص الخدمات العامة؟",
    "هل ينبغي زيادة ميزانيات المعاهد الدينية والمدارس الحريدية؟",
    "هل ينبغي توسيع الرقابة الحكومية على أسعار المواد الغذائية الأساسية؟",
    "هل ينبغي السماح بصلاة اليهود في الحرم القدسي، حتى لو غيّر ذلك الوضع القائم؟",
    "هل ينبغي توسيع المستوطنات في الضفة الغربية؟",
    "هل ينبغي تحديد عدد السنوات التي يستطيع رئيس الوزراء البقاء فيها بالمنصب بقانون؟",
    "هل ينبغي إعادة إقامة مستوطنات يهودية في غزة؟",
    "هل ينبغي فتح سوق الغذاء للاستيراد وخفض الرسوم الجمركية حتى لو تضررت الزراعة الإسرائيلية؟",
    "هل ينبغي تكريس المساواة الكاملة في الحقوق لمجتمع الميم، بما في ذلك الاعتراف بالشراكات، في القانون؟",
    "هل ينبغي لإسرائيل تقديم تنازلات للفلسطينيين، مثل تجميد البناء في المستوطنات، للتوصل إلى اتفاق سلام مع السعودية؟",
    "هل ينبغي فرض خدمة عسكرية أو مدنية على جميع المواطنين الإسرائيليين، بمن فيهم الحريديم والعرب؟",
]

EN_PARTIES = {
    "likud": "Likud", "beyachad": "Beyachad", "yashar": "Yashar!",
    "democrats": "The Democrats", "shas": "Shas", "utj": "United Torah Judaism",
    "rz": "Religious Zionism and Zehut", "otzma": "Otzma Yehudit",
    "amcha": "Amcha Israel", "beytenu": "Yisrael Beiteinu",
    "joint": "Joint List", "raam": "Ra'am", "noam": "Noam",
    "kachol": "Blue and White", "miluim": "The Reservists and the Economic Party",
    "haskel": "Israel First", "hakahal": "HaKahal", "tzibur": "The Haredi Public",
    "tzomet": "Tzomet Beit Israel", "batach": "Betach Social Security",
    "ahi": "Achi", "shutafut": "Partnership for Everyone",
    "orot": "Orot HaShachar", "byn": "Together We Will Succeed",
    "sharshar": "Chain for Love and Unity", "mishpat": "Justice Court",
    "shema": "Shema", "seder": "New Order", "britolam": "World Covenant",
    "hatikun": "The Fix", "gush": "Biblical Bloc", "bitachon": "Personal Security",
    "tzeva": "Black Color", "tkuma": "Tekuma", "pirates": "The Pirates",
    "ganeden": "Garden of Eden", "kolhanashim": "All Women", "aniveata": "Me and You",
}


def main() -> None:
    parties = fetch("parties.json")
    issues = fetch("issues.json")
    positions = fetch("positions.json")
    assert parties["version"] == issues["version"] == positions["version"] == "2026-09-30"
    assert len(issues["issues"]) == 20
    selected = issues["issues"][:18]
    assert len(EN) == len(AR) == len(selected) == 18
    snapshot = {
        "schemaVersion": 1,
        "electionId": "il-knesset-26",
        "revision": 1,
        "sourceVersion": parties["version"],
        "updatedAt": "2026-09-30",
        "electionDate": parties["election_date"],
        "attribution": {
            "name": "מצפן הבחירה 2026",
            "url": "https://bhirot26.online",
            "repository": "https://github.com/dangelm/bhirot26-election-data",
            "license": "CC BY 4.0",
            "licenseUrl": "https://creativecommons.org/licenses/by/4.0/",
            "upstreamCommit": UPSTREAM_SHA,
            "adaptation": "18 of 20 issues; coalition-strategy questions excluded; reported-only positions excluded from scoring; English and Arabic question translations added."
        },
        "questions": [
            {
                "id": item["slug"], "topicHe": item["topic"],
                "he": item["question"], "en": EN[i], "ar": AR[i],
                "sourceUrl": item["page"]
            } for i, item in enumerate(selected)
        ],
        "lists": [
            {
                "id": item["id"], "he": item["name"],
                "en": EN_PARTIES.get(item["id"], item["name"]),
                "ballot": item["ballot_letter"],
                "status": "court_review" if item["id"] in ("joint", "raam") else "committee_approved",
                "sourceUrl": item["page"],
            } for item in parties["parties"] if item.get("ballot_letter")
        ],
        "positions": []
    }
    issue_ids = {item["slug"] for item in selected}
    list_ids = {item["id"] for item in snapshot["lists"]}
    for item in positions["positions"]:
        if item["issue"] not in issue_ids or item["party"] not in list_ids:
            continue
        source = item.get("strongest_source")
        if item.get("evidence") != "sourced" or not source or source.get("tier") == "reported":
            continue
        snapshot["positions"].append({
            "listId": item["party"], "questionId": item["issue"],
            "value": round((item["stance"] - 0.5) * 4),
            "sourceType": source["type"], "sourceTitle": source["title"],
            "sourceDate": source.get("date"), "sourceUrl": source["url"],
            "verifiedAt": source.get("verified"),
        })
    assert len(snapshot["lists"]) == 38
    assert len(snapshot["questions"]) == 18
    assert len({(p["listId"], p["questionId"]) for p in snapshot["positions"]}) == len(snapshot["positions"])
    target = ROOT / "app" / "src" / "main" / "assets" / "content.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(snapshot, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Wrote {target}: {len(snapshot['lists'])} lists, {len(snapshot['positions'])} sourced positions")


if __name__ == "__main__":
    main()
