// ============================================================
// slides-visual-loop · shoot.js
// Headless render + per-slide full-viewport screenshots for visual QA.
//
// Usage:
//   node shoot.js <html-file> <out-dir> [width] [height]   # screenshot every .slide
//   node shoot.js --check                                  # doctor: resolve Playwright + test launch
//
// Playwright resolution is PREFER-EXISTING (no install unless nothing works):
//   1. $PW_MODULE (explicit path override)
//   2. local  require('playwright') / require('playwright-core')
//   3. global (npm root -g)/playwright  and  /playwright-core
//   4. global nested: (npm root -g)/<pkg>/node_modules/playwright-core|playwright
//      (this is how a bundled/built-in playwright-core is found)
// Browser: try the module's default browser; if that build is missing, fall back
//   to any chromium / chrome-headless-shell already in the ms-playwright cache.
// ============================================================
const path = require('path');
const fs = require('fs');
const os = require('os');
const cp = require('child_process');

function candidateModulePaths() {
  const out = [];
  if (process.env.PW_MODULE) out.push(process.env.PW_MODULE);
  // skill-bundled playwright — highest priority after explicit override
  const skillDir = path.resolve(__dirname, '..', 'node_modules');
  out.push(path.join(skillDir, 'playwright'), path.join(skillDir, 'playwright-core'));
  out.push('playwright', 'playwright-core'); // local project resolution
  try {
    const groot = cp.execSync('npm root -g 2>/dev/null').toString().trim();
    if (groot && fs.existsSync(groot)) {
      out.push(path.join(groot, 'playwright'), path.join(groot, 'playwright-core'));
      // one level of nesting: a global tool that bundles playwright-core
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
    try {
      const pw = require(m);
      if (pw && pw.chromium) return { chromium: pw.chromium, from: m };
    } catch (_) {}
  }
  return null;
}

function findCachedChrome() {
  if (process.env.PW_CHROME && fs.existsSync(process.env.PW_CHROME)) return process.env.PW_CHROME;
  const bases = [
    path.join(os.homedir(), 'Library/Caches/ms-playwright'),  // macOS
    path.join(os.homedir(), '.cache/ms-playwright'),          // linux
    path.join(os.homedir(), 'AppData/Local/ms-playwright'),   // windows
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
  if (!r) throw new Error('Playwright not found (looked for local & global playwright / playwright-core).');
  try {
    const b = await r.chromium.launch();
    return { browser: b, module: r.from, browserPath: '(module default)' };
  } catch (e) {
    const exe = findCachedChrome();
    if (exe) { const b = await r.chromium.launch({ executablePath: exe }); return { browser: b, module: r.from, browserPath: exe }; }
    throw e;
  }
}

async function doctor() {
  const r = resolveChromium();
  if (!r) { console.error('[check] NO Playwright module found'); process.exit(1); }
  console.log('[check] module:', r.from);
  try {
    const { browser, browserPath } = await launch();
    await browser.close();
    console.log('[check] browser OK:', browserPath);
    console.log('[check] PASS — existing Playwright is usable, no install needed');
    process.exit(0);
  } catch (e) {
    console.error('[check] module found but browser NOT launchable:', e.message);
    process.exit(2);
  }
}

async function shoot() {
  const htmlFile = process.argv[2] || 'index.html';
  const outDir = process.argv[3] || 'shots';
  const width = parseInt(process.argv[4] || '1600', 10);
  const height = parseInt(process.argv[5] || '900', 10);

  fs.mkdirSync(outDir, { recursive: true });
  const fileUrl = 'file://' + path.resolve(htmlFile);

  const { browser, module: mod, browserPath } = await launch();
  console.log(`[shoot] playwright: ${mod}`);
  console.log(`[shoot] browser:    ${browserPath}`);
  const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 2 });
  await page.goto(fileUrl, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200); // Tailwind JIT + fonts + first paint

  const count = await page.locator('.slide').count();
  console.log(`Found ${count} slides. Rendering at ${width}x${height}...`);
  for (let i = 0; i < count; i++) {
    const slide = page.locator('.slide').nth(i);
    await slide.scrollIntoViewIfNeeded();
    await page.waitForTimeout(700); // let reveal animations settle
    const num = String(i + 1).padStart(2, '0');
    await slide.screenshot({ path: path.join(outDir, `slide-${num}.png`) });
    console.log(`  saved slide-${num}.png`);
  }
  await browser.close();
  console.log('Done.');
}

(async () => {
  if (process.argv[2] === '--check') return doctor();
  return shoot();
})().catch(e => { console.error(e); process.exit(1); });
