# Changelog

## 1.7.0 — 2026-10-02

- Added Russian to the in-app language selector and translated the home screen, questionnaire, results, list browser, details, dialogs, and About screen.
- Added Russian wording for all 18 bundled questions and Russian names for all 38 submitted lists while retaining the original source links.
- Applied left-to-right layout and search by Russian list name; saved answers, signing identity, and election scoring remain compatible with 1.6.1.

## 1.6.1 — 2026-10-01

- Fixed a crash when launching directly from the installer’s Open button on devices that provide no splash icon view.
- Kept the splash fade and animated the compass icon when Android supplies it.

## 1.6.0 — 2026-10-01

- Added a visible compass-logo exit animation to the system splash screen before the home screen appears.
- Made navigation transitions clearer with a longer fade and gentle movement; questionnaire screens enter horizontally.
- Kept splash and screen motion disabled when Android's animation-duration setting is off.

## 1.5.0 — 2026-10-01

- Enlarged the header and updated the visible app name to “מצפן בחירות” (Election Compass).
- Placed the language picker and three compact, icon-only theme controls on one toolbar row.
- Replaced the home headline with a two-line invitation to take the matching questionnaire.

## 1.4.0 — 2026-10-01

- Replaced the combined brightness toggle and palette toggle with three explicit, mutually exclusive theme buttons: sun for Light, moon for Dark, and Star of David for Blue & white.
- Migrated saved color preferences into the new single theme setting without changing questionnaire answers.
- Shortened main-screen copy and separated statements to avoid awkward mid-sentence wrapping on phone screens.

## 1.3.0 — 2026-10-01

- Added a branded launch splash screen using Android's splash screen compatibility library.
- Reworked the launcher and header mark into a blue compass ring around a white Star of David with a small central needle.
- Kept package and signing continuity for an in-place update from 1.2.0.

## 1.2.0 — 2026-10-01

- Added Quick (10), Balanced (14), and Full (18) questionnaire depths. Switching depth preserves answers and adds questions to the existing comparison.
- Added an independently saved Israeli blue-and-white palette, selected with a Star of David control beside the sun/moon button.
- Added subtle touch ripples and screen transitions that honor Android's animation-duration setting.

## 1.1.0 — 2026-10-01

- Redesigned welcome, questionnaire, match results, list browsing, and evidence comparison screens.
- Added explicit language selection, question progress and navigation, ranked/unranked result sections, search and filters, and a matching-method explainer.
- Improved contrast and RTL text handling, kept questionnaire position across restarts, and added confirmation before deleting saved answers.

## 1.0.0 — 2026-10-01

- Initial Android questionnaire, on-device scoring, list browsing, source inspection, and signed feed support.
- Bundled adapted 2026-09-30 election snapshot: 38 submitted lists, 18 questions, 265 direct or documentary sourced positions.
- Hebrew, Arabic, and English interface; dark and light modes.
