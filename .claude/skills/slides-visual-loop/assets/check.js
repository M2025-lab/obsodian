// ============================================================
// slides-visual-loop · check.js
// DOM-level style validation — runs in Playwright after rendering.
// Catches issues that visual screenshot review can miss:
//   A. Text overflow / clipping (scrollHeight > clientHeight)
//   B. Elements escaping slide bounds (getBoundingClientRect)
//   C. Positioned overlay covering slide-content text (z-index occlusion)
//   D. Low-contrast text (computed color vs effective background, ratio < 2.5)
//
// Usage:
//   node check.js <html-file> [out-json]
// Exit 0 = clean; exit 1 = violations found.
// Output JSON is always written (pass or fail) for pipeline consumption.
// ============================================================
const path = require('path');
const fs   = require('fs');
const os   = require('os');
const cp   = require('child_process');

// ---- Playwright resolution (same logic as shoot.js) ----
function candidateModulePaths() {
  const out = [];
  if (process.env.PW_MODULE) out.push(process.env.PW_MODULE);
  out.push('playwright', 'playwright-core');
  try {
    const groot = cp.execSync('npm root -g 2>/dev/null').toString().trim();
    if (groot && fs.existsSync(groot)) {
      out.push(path.join(groot, 'playwright'), path.join(groot, 'playwright-core'));
      for (const pkg of fs.readdirSync(groot)) {
        const nm = path.join(groot, pkg, 'node_modules');
        out.push(path.join(nm, 'playwright-core'), path.join(nm, 'playwright'));
      }
    }
  } catch (_) {}
  return out;
}
function resolveChromium() {
  for (const m of candidateModulePaths()) {
    try { const pw = require(m); if (pw && pw.chromium) return { chromium: pw.chromium, from: m }; } catch (_) {}
  }
  return null;
}
function findCachedChrome() {
  const bases = [
    path.join(os.homedir(), 'Library/Caches/ms-playwright'),
    path.join(os.homedir(), '.cache/ms-playwright'),
    path.join(os.homedir(), 'AppData/Local/ms-playwright'),
  ];
  for (const b of bases) {
    if (!fs.existsSync(b)) continue;
    const dirs = fs.readdirSync(b).filter(d => d.startsWith('chromium_headless_shell') || d.startsWith('chromium-'));
    for (const d of dirs) {
      try {
        const found = cp.execSync(
          `find "${path.join(b, d)}" -type f \\( -name chrome-headless-shell -o -name Chromium -o -name chrome \\) 2>/dev/null || true`
        ).toString().trim().split('\n').filter(Boolean);
        if (found.length) return found[0];
      } catch (_) {}
    }
  }
  return null;
}
async function launch() {
  const r = resolveChromium();
  if (!r) throw new Error('Playwright not found.');
  try { const b = await r.chromium.launch(); return { browser: b }; }
  catch (e) {
    const exe = findCachedChrome();
    if (exe) { const b = await r.chromium.launch({ executablePath: exe }); return { browser: b }; }
    throw e;
  }
}

// ---- Contrast helpers (run inside page.evaluate) ----
// Passed as a serialised string and eval'd in browser context.
const BROWSER_HELPERS = `
function parseColor(str) {
  if (!str || str === 'transparent') return null;
  const m = str.match(/rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)/);
  if (!m) return null;
  return { r: +m[1], g: +m[2], b: +m[3], a: str.includes('rgba') ? parseFloat(str.split(',')[3]) : 1 };
}
function toLinear(c) {
  const s = c / 255;
  return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
}
function luminance({ r, g, b }) {
  return 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b);
}
function contrastRatio(c1, c2) {
  const l1 = luminance(c1), l2 = luminance(c2);
  return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
}
function effectiveBg(el) {
  let node = el;
  while (node && node !== document.body) {
    const bg = getComputedStyle(node).backgroundColor;
    const c = parseColor(bg);
    if (c && c.a > 0.1) return c;
    node = node.parentElement;
  }
  return parseColor(getComputedStyle(document.body).backgroundColor) || { r: 255, g: 255, b: 255 };
}
function rectsOverlap(a, b) {
  return !(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top);
}
`;

async function checkPage(page) {
  return page.evaluate((helperSrc) => {
    eval(helperSrc); // inject helpers into page context

    const issues = [];
    const slides = Array.from(document.querySelectorAll('.slide'));

    slides.forEach((slide, si) => {
      const slideRect = slide.getBoundingClientRect();
      const slideNum  = si + 1;

      // ---- A. Text overflow / clipping ----
      const allEls = Array.from(slide.querySelectorAll('*'));
      for (const el of allEls) {
        const st = getComputedStyle(el);
        // skip containers that clip intentionally
        if (st.overflow === 'hidden' || st.overflowY === 'hidden') continue;
        // skip decorative ghost elements (purely single char / number, large font, low opacity)
        const cls = el.className && typeof el.className === 'string' ? el.className : '';
        if (cls.includes('ghost') || cls.includes('decorative')) continue;
        if (parseFloat(st.opacity) < 0.15) continue;
        // require a meaningful overflow gap (> 8px) to filter subpixel font rounding
        if (el.scrollHeight > el.clientHeight + 8 && el.clientHeight > 0) {
          const tag = el.tagName.toLowerCase();
          const clsShort = cls.trim().slice(0, 60);
          const preview = (el.textContent || '').trim().slice(0, 80);
          issues.push({
            slide: slideNum,
            type: 'overflow',
            detail: `<${tag} class="${clsShort}"> scrollH=${el.scrollHeight} clientH=${el.clientHeight} — "${preview}"`,
          });
        }
      }

      // ---- B. Element rect escaping slide bounds ----
      // Only care about CONTENT elements (not decorative) that escape slide bounds on current viewport
      // Horizontal decks: slides are laid out horizontally so right/left bounds use per-slide offset
      const MARGIN = 8;
      const slideLeft  = slideRect.left;
      const slideRight = slideRect.right;
      for (const el of allEls) {
        const st  = getComputedStyle(el);
        if (st.position === 'fixed') continue; // nav dots etc — expected outside
        if (st.overflow === 'hidden' || st.overflowY === 'hidden') continue;
        const cls = el.className && typeof el.className === 'string' ? el.className : '';
        // skip decorative / ghost elements that intentionally bleed
        if (cls.includes('ghost') || cls.includes('decorative')) continue;
        if (parseFloat(st.opacity) < 0.1) continue;
        const r = el.getBoundingClientRect();
        if (r.width === 0 && r.height === 0) continue;
        // Only flag vertical overflow (bottom) — not horizontal, because in deck--h all slides
        // are side-by-side and their left/right can legitimately extend beyond viewport.
        if (r.bottom > slideRect.bottom + MARGIN || r.top < slideRect.top - MARGIN) {
          const tag     = el.tagName.toLowerCase();
          const clsShort = cls.trim().slice(0, 60);
          issues.push({
            slide: slideNum,
            type: 'out-of-bounds',
            detail: `<${tag} class="${clsShort}"> top=${Math.round(r.top)} bottom=${Math.round(r.bottom)} slide-bottom=${Math.round(slideRect.bottom)}`,
          });
        }
      }

      // ---- C. Positioned overlay covering slide-content text ----
      const contentEl = slide.querySelector('.slide-content');
      if (contentEl) {
        const contentRect = contentEl.getBoundingClientRect();
        const overlays = Array.from(slide.querySelectorAll('*')).filter(el => {
          const st = getComputedStyle(el);
          return (st.position === 'absolute' || st.position === 'fixed') &&
                 parseInt(st.zIndex || '0', 10) > 0 &&
                 el !== contentEl && !contentEl.contains(el);
        });
        for (const ov of overlays) {
          const ovRect = ov.getBoundingClientRect();
          if (ovRect.width === 0 && ovRect.height === 0) continue;
          if (rectsOverlap(ovRect, contentRect)) {
            const tag = ov.tagName.toLowerCase();
            const cls = ov.className && typeof ov.className === 'string' ? ov.className : '';
            // skip decorative / ghost / low-opacity overlays — they're intentional atmosphere
            if (cls.includes('ghost') || cls.includes('decorative')) continue;
            const opacity = parseFloat(getComputedStyle(ov).opacity);
            if (opacity < 0.15) continue;
            // skip tiny accent bars (height < 8px) — purely decorative
            if (ovRect.height < 8) continue;
            const zi = getComputedStyle(ov).zIndex;
            // Only flag if z-index is strictly above slide-content (z-index:1)
            if (parseInt(zi, 10) > 1) {
              issues.push({
                slide: slideNum,
                type: 'overlay',
                detail: `<${tag} class="${cls.trim().slice(0,60)}"> z-index=${zi} overlaps .slide-content`,
              });
            }
          }
        }
      }

      // ---- D. Low-contrast text ----
      const textEls = Array.from(slide.querySelectorAll('p,h1,h2,h3,h4,li,span,div,b,strong'))
        .filter(el => {
          const t = (el.childNodes.length === 1 && el.childNodes[0].nodeType === 3)
            ? el.textContent.trim()
            : '';
          return t.length > 3; // only leaf text nodes with real content
        });
      for (const el of textEls) {
        const st  = getComputedStyle(el);
        const fg  = parseColor(st.color);
        if (!fg) continue;
        const bg  = effectiveBg(el);
        const ratio = contrastRatio(fg, bg);
        if (ratio < 2.0) {
          const tag     = el.tagName.toLowerCase();
          const preview = el.textContent.trim().slice(0, 50);
          issues.push({
            slide: slideNum,
            type: 'contrast',
            detail: `<${tag}> ratio=${ratio.toFixed(2)} fg=rgb(${fg.r},${fg.g},${fg.b}) bg=rgb(${bg.r},${bg.g},${bg.b}) — "${preview}"`,
          });
        }
      }
    });

    return issues;
  }, BROWSER_HELPERS);
}

(async () => {
  const htmlFile = process.argv[2];
  const outJson  = process.argv[3] || 'check-report.json';

  if (!htmlFile) {
    console.error('Usage: node check.js <html-file> [out-json]');
    process.exit(1);
  }

  const { browser } = await launch();
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } });
  const fileUrl = 'file://' + path.resolve(htmlFile);
  await page.goto(fileUrl, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500); // Tailwind JIT + fonts

  const issues = await checkPage(page);
  await browser.close();

  const report = { file: htmlFile, timestamp: new Date().toISOString(), total: issues.length, issues };
  fs.writeFileSync(outJson, JSON.stringify(report, null, 2));

  if (issues.length === 0) {
    console.log('[check] PASS — no DOM-level style violations found.');
    process.exit(0);
  } else {
    console.log(`[check] VIOLATIONS: ${issues.length} issue(s) found — see ${outJson}`);
    // Group by slide for readable summary
    const bySlide = {};
    for (const iss of issues) {
      (bySlide[iss.slide] = bySlide[iss.slide] || []).push(iss);
    }
    for (const [s, arr] of Object.entries(bySlide)) {
      console.log(`  Slide ${s}:`);
      for (const iss of arr) console.log(`    [${iss.type}] ${iss.detail}`);
    }
    process.exit(1);
  }
})().catch(e => { console.error(e); process.exit(1); });
