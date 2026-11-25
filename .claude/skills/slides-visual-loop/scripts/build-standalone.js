// ============================================================
// slides-visual-loop · build-standalone.js
// Inline tailwind.play.js into the HTML to produce ONE self-contained,
// fully offline file.
//
// CRITICAL LESSON (do not "fix" back to .replace):
//   Never use String.prototype.replace() with the Tailwind source as the
//   replacement string. Minified Tailwind is full of `$` (`$&`, `$'`, `` $` ``,
//   `$$`) which replace() interprets as special patterns — it silently
//   corrupts the output and the page renders as raw text in the browser.
//   Use split(marker).join(literal) instead (no $-interpretation).
//
// Usage: node build-standalone.js <in.html> <out.html> [tailwind.play.js]
// ============================================================
const fs = require('fs');
const [, , inPath, outPath, twPathArg] = process.argv;
if (!inPath || !outPath) { console.error('usage: node build-standalone.js <in.html> <out.html> [tailwind.play.js]'); process.exit(1); }

const twPath = twPathArg || 'tailwind.play.js';
const html = fs.readFileSync(inPath, 'utf8');
const tw = fs.readFileSync(twPath, 'utf8');

// Safety 1: a </script substring in the bundle would close the tag early.
if (/<\/script/i.test(tw)) { console.error('REFUSE: tailwind bundle contains "</script" — inlining would break. Ship the two-file bundle instead.'); process.exit(2); }

const marker = '<script src="tailwind.play.js"></script>';
if (!html.includes(marker)) { console.error('REFUSE: marker not found in HTML:', marker); process.exit(3); }

// Literal concatenation — split/join does NOT interpret `$` in the joined string.
const out = html.split(marker).join('<script>\n/* Tailwind Play CDN (JIT) inlined for offline use */\n' + tw + '\n</script>');

// Safety 2: structural validation (this check would have caught the $-corruption bug).
const openTags = (out.match(/<script/g) || []).length;
const closeTags = (out.match(/<\/script>/g) || []).length;
const slides = (out.match(/class="slide"/g) || []).length;
if (openTags !== closeTags) { console.error(`REFUSE: script tag mismatch ${openTags} open / ${closeTags} close`); process.exit(4); }
if (!out.startsWith('<!DOCTYPE') || !out.trimEnd().endsWith('</html>')) { console.error('REFUSE: output is not a well-formed HTML document'); process.exit(5); }

fs.writeFileSync(outPath, out);
console.log(`OK -> ${outPath} (${Buffer.byteLength(out)} bytes; ${openTags}/${closeTags} script tags; ${slides} slides)`);
