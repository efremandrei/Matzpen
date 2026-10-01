# Matzpen / מצפן

An Android voter guide for Israel's 26th Knesset election. It compares a voter's answers to **documented policy positions** of submitted lists. It does not endorse a list, predict results, or use polls in scoring.

## What it does

- Three nested questionnaire depths: Quick (10 questions), Balanced (14), and Full (18). All questions support Hebrew, Arabic, and English, five answer levels, skips, and up to three double-weight priorities. Answers carry forward when changing depth.
- Ranks lists after eight answers when supported positions cover at least 70% of the voter's weighted answers. Every list remains browseable, including lists without enough evidence to rank.
- Displays the source, source date, and evidence coverage for each result. Lists under court review are identified.
- Keeps answers on the device. There is no account, analytics, or answer upload.
- Loads a bundled election snapshot offline and accepts only newer, correctly signed public feed revisions.
- Defaults to dark mode. The sun/moon control switches brightness, and the adjacent Star of David switches to a blue-and-white Israeli palette; both choices are saved independently.
- Opens with a branded splash screen and a matching Star of David compass launcher icon; the same mark appears in the app header.
- Uses subtle screen transitions and touch ripples, following Android's animation-duration setting.
- Provides a question index, saved progress, searchable list browsing, compact ranked and unranked results, and a source-backed comparison of each answer.

The [design overview](DESIGN_OVERVIEW.md) documents the visual direction and UX principles behind the app.

## Data provenance

The bundled snapshot is adapted from [מצפן הבחירה 2026](https://bhirot26.online), [open dataset](https://github.com/dangelm/bhirot26-election-data), revision `4b304f9983f981992ecaf5a66cfc597155745d85` (dataset version 2026-09-30), under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). See [DATA_LICENSE.md](DATA_LICENSE.md) for changes and attribution.

The app includes the first 18 policy questions from the upstream set and excludes its two coalition-strategy questions. Positions supported only by third-party reporting are excluded from scoring. A sourced party position can be against (-2), partial/conditional (0), or for (2), while voter answers run from -2 to 2. Unknown and excluded positions do not contribute to the score or coverage. The score is the weighted average of `100 × (1 − |voter − party| / 4)` over supported answered questions. Each priority gets weight 2; all others get weight 1. The score is shown only after eight answers and at least 70% weighted evidence coverage. Equal scores share a rank.

This is a dated snapshot. Ballot status and party positions can change. The source links and current official election information should be checked before voting.

## Build

Requires JDK 21 and Android SDK 35. On Windows:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug testDebugUnitTest
```

To refresh the source snapshot intentionally, update the pinned upstream commit and translations in `scripts/build_content.py`, review the changes, increment both dataset revision and app version, run `python scripts/build_content.py`, then `python scripts/sign_feed.py`. Never publish an unsigned or unreviewed feed.

For a locally signed release, run `scripts/build_release.ps1`. The release keystore and DPAPI-protected password live outside the repository in `%USERPROFILE%\.android\keystores`. **Back up the keystore and its password securely**. All app updates must retain `com.efremandrei.matzpen`, the same signing key, and an increasing `versionCode`.

## Public feed

GitHub Pages serves `docs/feed/manifest.json`, `manifest.sig`, and `content.json`. The app pins the feed public key, verifies ECDSA P-256 signatures and content SHA-256, rejects older revisions, and keeps the last valid copy. The private feed key lives outside the repository.

## Project status

Version 1.3.0 is a source-backed pre-election preview. A physical Samsung device has not been tested. Question translations and election updates should receive independent editorial review before broad promotion.
