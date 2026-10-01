# Matzpen design overview

Status: design direction, 1 October 2026. The 1.5.0 implementation places the compact theme icons beside the language picker, enlarges the “Election Compass” brand, and gives the home screen a two-line invitation. It also includes the branded splash, Star of David compass icon, questionnaire depths, and restrained motion. Independent editorial review remains future work.

## Product promise

Help a voter understand where their policy views overlap with documented positions of election lists, then make it easy to inspect the evidence. The experience should feel calm, impartial, and trustworthy. It must never imply that a match score is an instruction to vote for a list.

**Design idea: a modern civic compass.** Use the compass as a quiet wayfinding motif: a fine radial line, a small north-point mark, and directional transitions. The optional blue-and-white palette uses a Star of David control and applies to the whole interface; list cards remain visually neutral, without celebratory ranking effects or visuals that privilege a list.

## Questionnaire depth and skin controls

The welcome screen offers Quick (10), Balanced (14), and Full (18). The paths are nested so choosing a longer one keeps existing answers and adds four questions at a time. All scores use the answers saved across paths. More answers can reflect more of a voter's views, while documentation gaps still limit ranking. The interface describes this as comparison depth rather than a guarantee of accuracy.

The header gives the larger app name its own row. A second compact row holds the language picker and three icon-only, mutually exclusive theme choices. Sun selects the light palette, moon selects dark, and the Star of David selects a blue-and-white palette. The selected icon has a subtle circular fill and persists across launches; accessible names describe every icon. Blue is reserved for navigation and selection, never used to imply support for any list. Buttons and result cards use touch ripples; screen changes use a short fade and rise, disabled when Android's animation scale is zero.

Main-screen copy uses brief, separate statements and compact metadata. Complete questionnaire questions and source excerpts remain readable, with natural wrapping where their full wording needs more space.

The launch mark combines a light-blue compass ring, four bearing ticks, a white six-pointed star, and a small north/south needle on a deep-blue field. The same vector supplies the adaptive launcher foreground, system splash, and header brand mark. The splash closes as soon as the home screen is ready; no artificial delay is added.

## What the current app gets right

- It opens directly to a useful task, works offline, and keeps answers on the device.
- Dark and light skins already exist, with dark as the first-launch choice.
- The five-point answer scale, skips, three priority choices, and source links make the comparison inspectable.
- Hebrew, Arabic, and English are available, and ballot status and evidence coverage are disclosed.

## Main experience gaps

This review describes the 1.0.0 baseline that motivated the 1.1.0 redesign.

| Current experience | Effect on voters | Proposed change |
| --- | --- | --- |
| Home stacks an election card and several equally wide buttons. | The primary task competes with reference information and the screen feels utilitarian. | A compact, distinctive hero with one primary action; a small dated-data strip and two secondary destinations. |
| The language control cycles through `HE`, `AR`, and `EN`. | Its next state is unclear and switching can be accidental. | A labeled language picker showing all three names in their own scripts. |
| Each question repeats five full-width buttons plus skip, previous, next, and home. | High scrolling and weak sense of progress. | A compact segmented answer scale, visible progress track, one persistent next action, and a small skip link. |
| `Next / results` is shown on every question and works without an answer. | The action label and resulting state are ambiguous. | `Next question` until the last step, `See matches` on the last step; visibly mark skipped questions. |
| Results are a long stream of 38 similar cards. | The highest matches are hard to compare and evidence gaps are buried. | A short ranked summary, compact comparable rows, and a distinct unranked section. |
| Detail shows 18 raw evidence cards and numeric `Your answer: 2`. | The reason for a match is difficult to grasp at a glance. | A side-by-side issue comparison: **You** / **List** / **Evidence**, grouped by agreement, difference, and unknown. Use words instead of numeric codes. |
| Source titles can appear in Hebrew on an English screen. | The language shift is abrupt and some links wrap awkwardly. | Keep the original title for attribution, label its language, and show a short translated description where editorially reviewed. |
| Large titles and the header feel crowded in RTL screenshots. | Long labels can collide or wrap unpredictably. | Responsive header, direction-aware spacing, and explicit mixed-script handling. |

## Proposed journey

### 1. Welcome / resume

Top bar: compact compass mark and name, explicit language control, sun/moon skin control. The first screen leads with one sentence: **“Find where your views align.”** Below it, show “18 questions · about 5 minutes · private on this device.” Use one prominent **Start** button. If answers exist, replace it with **Continue** and show `8 of 18 answered`; keep **View matches** as a secondary action once the minimum is met.

Place election date, content date, and list-status caveat in a small **Election data** card lower on the page. Keep **Browse lists** and **About / sources** as quiet navigation items. The privacy and non-endorsement note remains visible but short.

### 2. Questionnaire

One issue per screen. Show a thin progress track and `Question 4 / 18`, followed by the issue in a spacious card. The original-context link should be a clear secondary row. Put the five answer choices in a compact vertical scale with a strong selected state (filled surface, checkmark, and text), preserving generous tap areas. Under it, show the priority control with `0 / 3 selected` and a short explanation of its effect.

Keep a single sticky primary action at the bottom and a text **Skip** action nearby. Previous is an icon plus label at the top or next to the primary action. Saving is automatic; returning must restore the last viewed question, selection, and scroll position. Skipped and answered states should be distinguishable in a question index sheet.

### 3. Matches

Start with `Answered 12 of 18` and a plain explanation of what a percentage means. Show the top three *eligible* matches as equal-sized comparison rows, then a sortable list of the remaining ranked lists. Do not use gold, trophies, victory language, oversized first-place treatment, or party branding. Each row shows name and ballot letters, alignment, evidence coverage, and a clear **Compare positions** action.

Put lists that cannot be ranked in a separate **Not enough evidence** section with the exact coverage and an explanation. A voter can open every list. A **How matching works** sheet explains the 8-answer minimum, priority weighting, 70% coverage threshold, unknown positions, and ties in plain language. Date and caveat remain reachable here.

### 4. List comparison

The first viewport answers the key question: **“Where do we agree or differ?”** Show the list name, ballot letters, status, score when available, and coverage. Then show a summary such as `5 aligned · 2 different · 3 without a documented position`, calculated only from answered issues and labeled carefully.

Each issue row uses three labeled lines: **Your view**, **Documented list position**, and **Source**. Use readable answer words, not `-2 … 2`. Show publication date and type before an external-link affordance. Let voters filter or jump between **All**, **Agreement**, **Differences**, and **Unknown**. Keep the source URL available for independent checking.

### 5. Browse / About

Browse needs search by list name or ballot letters and simple status filters. Cards should remain neutral and compact. About should keep author, email, repo URL, version/build, license, source dataset, data date, privacy, and answer deletion. Destructive answer deletion needs a confirmation step and a clear `This device only` explanation.

## Visual system

The signature composition is **large editorial question text + a very fine compass/progress line + quiet evidence metadata**. It should be recognizable without relying on party imagery. The first viewport of each screen has a single focal point.

Suggested starting tokens (validate in real screens and for all states):

| Token | Light | Dark |
| --- | --- | --- |
| Canvas | `#F5F7F8` | `#0B1424` |
| Surface | `#FFFFFF` | `#17263A` |
| Primary text | `#172435` | `#F3F7FA` |
| Secondary text | `#526273` | `#B9C9D6` |
| Link / active text | `#087C72` | `#52DBC6` |
| Primary button | `#2DD3BA` with `#0B1424` text | Same |
| Caution text | `#865400` | `#F7BA6A` |

The suggested light link on white is 5.08:1, dark link on dark surface is 8.95:1, and secondary light text on white is 6.26:1. These are starting pairings, not a substitute for checking disabled, selected, and overlay states.

| Element | Direction |
| --- | --- |
| Color | Retain deep ink and mint, but reduce mint to actions, progress, and links. Use warm amber only for dated-data or court-review notices. Use the same neutral treatment for every list. |
| Light skin | Soft off-white background, white or lightly tinted surfaces, dark ink text, subtle borders instead of heavy shadows. |
| Dark skin | Deep navy background with slightly lighter surfaces and high-contrast text. Avoid pure black and neon mint. |
| Typography | Strong, editorial headline; readable body; quiet metadata. Use fonts with excellent Hebrew and Arabic forms. Keep body text at least 16sp and allow Android font scaling. |
| Spacing | An 8dp rhythm, 20–24dp page margins, 16–20dp card padding, and consistent vertical intervals. Short screens should breathe without leaving essential actions stranded. |
| Shape | 20–24dp cards, 12–16dp controls, 1dp borders where needed. The compass motif is a detail, not a background pattern behind text. |
| Motion | 150–250ms transitions for selection, progress, and screen changes; respect reduced-motion settings. No confetti or rank animations. |
| Icon | Evolve the existing compass into a crisp adaptive launcher icon and a small in-app brand mark; retain recognition across dark and light backgrounds. |

## Trust and accessibility rules

- Preserve the non-endorsement statement and dated snapshot information at the point where scores are read.
- Never hide missing evidence or make an unranked list look like a low-scoring list.
- Support Hebrew and Arabic RTL layouts, including correctly isolated mixed-script names, dates, percentages, and ballot letters. Use direction-aware navigation icons.
- Give every interactive element a 48dp or larger touch target, visible focus/selected states, and a useful screen-reader label. Test at large font and display sizes.
- Maintain at least 4.5:1 contrast for normal text and 3:1 for large text and key graphics. Do not encode agreement or status by color alone.
- Validate copy and question translations with native Hebrew and Arabic readers before broad promotion.

## Build order

1. **Flow and information architecture:** welcome/resume, question progress and navigation, compact ranked/unranked results, comparison summary.
2. **Design system:** type, spacing, color tokens, cards, controls, RTL-safe header, accessible states, launcher icon refinement.
3. **Trust details:** matching explainer, evidence presentation, source language handling, dated-data notices, deletion confirmation.
4. **Visual QA:** small and large Samsung screens, dark/light, Hebrew/Arabic/English, large type, TalkBack, and high contrast.

## Definition of done for the redesign

A first-time voter can understand the task and start within one screen; answer or skip without confusion; resume after leaving; compare ranked results without mistaking the score for a recommendation; see why a list matched and open its original evidence; and complete these flows in Hebrew, Arabic, and English with dark or light skin and large text. The existing package ID, signing key, saved answers, and update path must remain intact.

Android guidance used for the accessibility and RTL criteria: [accessible Views and 48dp targets](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views), [contrast and accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility), and [bidirectional layout and text](https://developer.android.com/training/basics/supporting-devices/languages.html).
