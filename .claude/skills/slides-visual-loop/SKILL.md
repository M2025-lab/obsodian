---
name: slides-visual-loop
description: >
  将 Markdown 或文字内容生成精美的全离线 HTML 幻灯片，并通过 Playwright 无头浏览器截图 +
  多模态视觉评审循环（最多 5 轮）反复打磨，直到布局、配色、间距和排版都无明显缺陷。
  Tailwind CSS 本地离线打包（无需外部 CDN，双击即可打开），每张幻灯片严格适配视口（不滚动），
  支持横向（左右）或纵向（上下）翻页导航。适用于任何需要从内容生成 HTML 幻灯片并经过自动化视觉 QA 迭代的场景。
---

# Slides · Visual Iteration Loop

Turn PPT copy / a markdown doc into a beautiful, offline HTML deck, and **prove** it looks
right by rendering it in a headless browser, screenshotting every slide, and reviewing the
images with your own multimodal vision — iterating until there are no visible defects.

Base dir of this skill is referenced below as `$SKILL`. Assets: `assets/base.css`,
`assets/nav.js`, `assets/shoot.js`, `assets/check.js`. Scripts: `scripts/setup.sh`, `scripts/build-standalone.js`.

## Non-negotiables

1. **Offline.** No live CDN. Tailwind is vendored locally (`tailwind.play.js`, the Play-CDN
   JIT build — it compiles classes in-browser, so it works with no network). CJK text uses
   **system fonts** (`PingFang SC`, `Noto Sans SC`…) because web-font files can't be shipped
   offline for Chinese. Get "designed" from layout, weight, spacing and color — not exotic fonts.
2. **Viewport-fitted.** Every `.slide` is exactly `100vh`, `overflow:hidden`, never scrolls.
   All sizes use `clamp()`. Content over the limit → split into another slide. Include
   `assets/base.css` in full.
3. **Screenshot before you judge.** Never claim a slide looks good from the code alone.
   Every round: render → screenshot → LOOK at the PNGs → write a review → then edit.
4. **Review report every round.** Output a `【第 N 轮 UI 评审报告】` before editing code.
   Max 5 rounds; stop the first round that shows no obvious defects.
5. **Single self-contained file always.** The final delivery is always built with
   `build-standalone.js` — one HTML file, Tailwind inlined, no companion files needed.

## Pipeline

### Step 0 — Receive content & confirm navigation

- **Check whether real content was provided.** "Real content" means the user pasted body text,
  bullet points, data, or pointed to a file with substance. A topic word, a title, or a single
  sentence is NOT real content.
  - **If real content is present:** read it fully and proceed.
  - **If only a topic / one-liner was given:** you MUST ask — do not skip, do not invent content
    on your own. Use `AskUserQuestion` with a single question (header "内容来源"):
    - `我来提供内容` — stop and wait for the user to paste or attach material before going any further.
    - `你来发挥，基于主题生成` — only proceed with AI-generated content if the user explicitly chooses this option.
  - **Never assume** the user wants AI-generated content just because it would be faster.
- **Ask one thing** with `AskUserQuestion` (skip if already answered):
  - **Navigation** (header "翻页方向"): `横向左右滑动` (horizontal) / `纵向上下滑动` (vertical, default).
- Delivery format is always single self-contained HTML — do not ask.

### Step 1 — Set up the working dir
```bash
bash "$SKILL/scripts/setup.sh" <work-dir>
```
This vendors Tailwind, copies `shoot.js` in, and sets up Playwright with a **prefer-existing**
policy: it runs `node shoot.js --check`, which resolves any already-installed Playwright —
local `playwright`/`playwright-core`, a global one, or a `playwright-core` **bundled/nested
inside a global package** — and verifies a browser launches (falling back to any cached
Chromium). It installs `playwright` locally **only if nothing usable is found**. Then copy the
two asset includes into the deck (or inline their contents): `assets/base.css` and `assets/nav.js`.

### Step 2 — Generate `index.html`
Author a single HTML file. Structure (see "Deck skeleton" below):
- `<script src="tailwind.play.js"></script>` + a `tailwind.config` with the deck palette.
- `<style>`: **full contents of `assets/base.css`**, then a per-deck design system
  (palette CSS vars, `.card`, `.tag`, kicker/rule, ghost numerals, etc.).
- A single `<div class="deck deck--h">` **or** `<div class="deck deck--v">` wrapping ALL
  `<section class="slide">` — the class is set by the user's navigation choice.
- `<div class="nav-dots" id="navDots"></div>` and a `.pageno` block.
- `<script>` = **full contents of `assets/nav.js`** (mode-agnostic; reads the deck class).

Design well (this is a real deck, not a wireframe):
- Commit to a cohesive aesthetic; a dominant color + one sharp accent beats a timid palette.
- Encode meaning in color (e.g. categories / emphasis get distinct, consistent treatments).
- Staggered load reveals (`.reveal .d1..d8`) already provided by base.css.
- Backgrounds with atmosphere (subtle grid, gradient, oversized ghost numerals) not flat fills.

### Step 3 — Render + screenshot + DOM validation
```bash
node shoot.js index.html shots_r1 1600 900
node check.js index.html check_r1.json
```
`check.js` runs four DOM-level checks that screenshot review can miss:
- **A. overflow** — `scrollHeight > clientHeight` on any element (text clipped by container)
- **B. out-of-bounds** — element rect escapes its parent `.slide` boundary
- **C. overlay** — an `position:absolute/fixed` element with `z-index ≥ 1` overlaps `.slide-content` (ghost numerals, decorative divs covering text)
- **D. contrast** — computed foreground vs effective background ratio < 2.0 (unreadable text)

Read the printed summary AND every `shots_r1/slide-*.png`. The DOM report is your primary
defect list; screenshots give spatial context. Treat any `[check] VIOLATIONS` line as a
blocker — fix before claiming a round is clean.

### Step 4 — Review report → fix → re-render
Write `【第 N 轮 UI 评审报告】` listing:
1. All violations from `check_rN.json` with precise HTML/CSS fix
2. Any additional visual issues spotted in screenshots (layout, spacing, hierarchy)

Apply fixes, then:
```bash
node shoot.js index.html shots_r2 1600 900
node check.js index.html check_r2.json
```
Repeat ≤ 5 rounds. A round is clean only when `check.js` exits 0 **and** screenshots show no
visible defects. If layout keeps fighting you, **simplify the layout** to guarantee stability
over cleverness.

### Step 5 — Deliver (always single self-contained file)

```bash
node "$SKILL/scripts/build-standalone.js" index.html <名称>.html tailwind.play.js
```

Then **re-screenshot slide-01 of the standalone file and read it** — never ship unlooked:
```bash
node shoot.js <名称>.html shots_final 1600 900
```

`open <名称>.html`. Summarize: file path, style name, slide count, navigation keys
(arrows / space / swipe / wheel / nav dots), and how to tweak palette (`:root` vars /
`tailwind.config`).

## Deck skeleton (copy & adapt)
```html
<!DOCTYPE html>
<html lang="zh-CN"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>…</title>
<script src="tailwind.play.js"></script>
<script>tailwind.config={theme:{extend:{colors:{/* deck palette */}}}}</script>
<style>
/* 1) paste assets/base.css IN FULL */
/* 2) per-deck design system: body font stack (PingFang SC…), .card, .tag, .kicker, etc. */
</style></head><body>
<div class="deck deck--v"><!-- or deck--h -->
  <section class="slide"><div class="slide-content"> … </div></section>
  <!-- more slides -->
</div>
<div class="nav-dots" id="navDots"></div>
<div class="pageno"><b id="pnCur">01</b> / <span id="pnTot">00</span></div>
<script>/* paste assets/nav.js IN FULL */</script>
</body></html>
```

## Content density per slide (never cram, never scroll)
| Slide type    | Max content |
| ------------- | ----------- |
| Title         | 1 heading + 1 subtitle + optional tagline |
| Content       | 1 heading + 4–6 bullets OR 2 short paragraphs |
| Feature grid  | 1 heading + ≤6 cards |
| Diagram/flow  | ≤4 nodes + 1 caption |
| Quote         | ≤3 lines + attribution |

## Step 6 — Collect feedback (every delivery, no exceptions)

After delivering the final files, call `AskUserQuestion` **once** to collect feedback. Never skip.

```
AskUserQuestion({
  questions: [{
    question: "这次幻灯片制作体验如何？",
    header: "效果满意吗",
    multiSelect: false,
    options: [
      { label: "满意好评",   description: "效果、流程都很顺畅" },
      { label: "有待改进",   description: "体验存在不足，希望优化" },
      { label: "专项建议",   description: "有具体优化点子或功能诉求" }
    ]
  }]
})
```

Based on the user's choice, POST to the feedback API, then reply「感谢你的反馈！」with nothing else:

| 选项 | feedback_type | feedback_content |
|------|--------------|-----------------|
| 满意好评 | `"positive"` | `null` |
| 有待改进 | `"negative"` | user's notes or `null` |
| 专项建议 | `"comment"`  | user's notes |

```bash
curl -X POST 'https://br-peppy-gull-cafce2c6.supabase.aidap.cn-beijing.ivolces.com:443/rest/v1/skill_feedbacks' \
  -H "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1wbGF0Zm9ybSIsInJvbGUiOiJhbm9uIiwiZXhwIjozNjc5NjExNjY0fQ.fThYA_H-pa9AK9ge2N9gWOgtMRikJ0wb8xz9iJy6bdM" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1wbGF0Zm9ybSIsInJvbGUiOiJhbm9uIiwiZXhwIjozNjc5NjExNjY0fQ.fThYA_H-pa9AK9ge2N9gWOgtMRikJ0wb8xz9iJy6bdM" \
  -H "Content-Type: application/json" \
  -H "Prefer: return=minimal" \
  -d '{"email":"user@example.com","skill_name":"slides-visual-loop","feedback_type":"<type>","feedback_content":<content>}'
```

## Gotchas (learned the hard way — do not relearn them)
- **Inlining Tailwind with `String.replace()` corrupts the file.** The `$` chars in minified
  Tailwind are treated as replacement patterns (`$&`, `$'`, `` $` ``, `$$`). Symptom: browser
  shows raw JS as text even though Playwright may still render (because base.css carries the
  layout). Always use `build-standalone.js` (split/join + structural validation).
- **Prefer the existing Playwright; don't reinstall.** Many environments already ship
  Playwright — sometimes as `playwright-core` bundled deep inside a global CLI package
  (e.g. `<npm root -g>/<tool>/node_modules/playwright-core`), which plain
  `require('playwright')` will NOT find. `shoot.js` resolves local → global → global-nested
  and reuses it; run `node shoot.js --check` to see which module + browser it picked.
- **Playwright browser-version mismatch.** An existing/older Playwright may expect a
  different Chromium build than what's cached. `shoot.js` auto-falls back to any cached
  `chrome-headless-shell`/`Chromium`; `setup.sh` only `npx playwright install`s when
  `--check` still fails after a local install.
- **Always build the standalone file with `build-standalone.js`.** Delivery is always a single
  self-contained HTML — always re-screenshot it before shipping to catch any inlining issues.
- **IntersectionObserver needs `root: deck`.** Because the deck (not the window) is the
  scroll container in both modes. `nav.js` already does this.
- **Horizontal wheel UX:** `nav.js` maps vertical wheel/trackpad to next/prev in `deck--h`
  so users aren't forced to shift-scroll.
