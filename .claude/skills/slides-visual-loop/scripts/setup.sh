#!/usr/bin/env bash
# ============================================================
# slides-visual-loop · setup.sh
# Prepares a working directory for offline slide generation + QA.
#   1. vendors Tailwind Play CDN locally (offline, no external CDN)
#   2. copies shoot.js in
#   3. PREFERS an existing Playwright (built-in / global / local); installs
#      locally ONLY if none is usable
# Usage: bash setup.sh <work-dir>
# ============================================================
set -e
WORK="${1:-.}"
SKILL_DIR="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$WORK"
cd "$WORK"

echo "== [1/3] Vendoring Tailwind Play CDN (offline) =="
if [ ! -f tailwind.play.js ]; then
  curl -sL https://cdn.tailwindcss.com -o tailwind.play.js
fi
echo "   tailwind.play.js: $(wc -c < tailwind.play.js) bytes"

echo "== [2/3] Copying shoot.js + check.js =="
cp "$SKILL_DIR/assets/shoot.js" ./shoot.js
cp "$SKILL_DIR/assets/check.js" ./check.js

echo "== [3/3] Playwright: skill-bundled first, then existing, install only if all else fails =="
# shoot.js --check resolves: skill node_modules -> local -> global -> global-nested.
# The skill ships playwright in its own node_modules, so this should always PASS
# without any download. Install fallback is a last resort.
if node shoot.js --check; then
  echo "   -> Playwright resolved (skill-bundled or existing), no install performed"
else
  echo "   -> no usable Playwright found; installing locally as fallback"
  npm init -y >/dev/null 2>&1 || true
  PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm i playwright >/dev/null 2>&1 || npm i playwright
  # re-check; if the module now resolves but no browser is launchable, fetch one
  if ! node shoot.js --check; then
    echo "   -> downloading chromium-headless-shell"
    npx playwright install chromium-headless-shell
  fi
fi

echo "Setup complete in: $(pwd)"
