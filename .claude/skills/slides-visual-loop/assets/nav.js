/* ============================================================
   slides-visual-loop · nav.js
   Works for BOTH directions. Reads mode from the .deck class:
     <div class="deck deck--v"> ... vertical  (↑/↓)
     <div class="deck deck--h"> ... horizontal (←/→ + wheel)
   Requires: a .deck wrapping all .slide sections, and optionally
     <div class="nav-dots" id="navDots"></div>
     <div class="pageno"><b id="pnCur">01</b> / <span id="pnTot">00</span></div>
   ============================================================ */
(function () {
  const deck = document.querySelector('.deck');
  if (!deck) { console.warn('[nav] no .deck found'); return; }
  const horizontal = deck.classList.contains('deck--h');
  const slides = Array.from(deck.querySelectorAll('.slide'));
  let cur = 0;

  // ---- build nav dots ----
  const dotsWrap = document.getElementById('navDots');
  if (dotsWrap) {
    dotsWrap.classList.add(horizontal ? 'deck-h-dots' : 'deck-v-dots');
    slides.forEach((s, i) => {
      const d = document.createElement('div');
      d.className = 'nav-dot'; d.title = 'Slide ' + (i + 1);
      d.onclick = () => goTo(i);
      dotsWrap.appendChild(d);
    });
  }
  const dots = dotsWrap ? Array.from(dotsWrap.children) : [];
  const pnCur = document.getElementById('pnCur');
  const pnTot = document.getElementById('pnTot');
  if (pnTot) pnTot.textContent = String(slides.length).padStart(2, '0');

  function goTo(i) {
    cur = Math.max(0, Math.min(slides.length - 1, i));
    slides[cur].scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'start' });
  }

  // ---- active-slide detection (root = the scrolling deck) ----
  const io = new IntersectionObserver((entries) => {
    entries.forEach((e) => {
      if (e.isIntersecting) {
        e.target.classList.add('in-view');
        const i = slides.indexOf(e.target);
        cur = i;
        dots.forEach((d, j) => d.classList.toggle('active', j === i));
        if (pnCur) pnCur.textContent = String(i + 1).padStart(2, '0');
      }
    });
  }, { root: deck, threshold: 0.55 });
  slides.forEach((s) => io.observe(s));

  // ---- keyboard: both arrow pairs work in either mode ----
  window.addEventListener('keydown', (e) => {
    if (['ArrowRight', 'ArrowDown', ' ', 'PageDown'].includes(e.key)) { e.preventDefault(); goTo(cur + 1); }
    if (['ArrowLeft', 'ArrowUp', 'PageUp'].includes(e.key)) { e.preventDefault(); goTo(cur - 1); }
    if (e.key === 'Home') { e.preventDefault(); goTo(0); }
    if (e.key === 'End') { e.preventDefault(); goTo(slides.length - 1); }
  });

  // ---- horizontal mode: map vertical wheel/trackpad to next/prev ----
  if (horizontal) {
    let lock = false;
    deck.addEventListener('wheel', (e) => {
      if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) return; // genuine horizontal scroll — let it pass
      e.preventDefault();
      if (lock) return; lock = true; setTimeout(() => (lock = false), 650);
      goTo(cur + (e.deltaY > 0 ? 1 : -1));
    }, { passive: false });
  }

  // ---- touch swipe (both modes) ----
  let sx = 0, sy = 0;
  deck.addEventListener('touchstart', (e) => { sx = e.touches[0].clientX; sy = e.touches[0].clientY; }, { passive: true });
  deck.addEventListener('touchend', (e) => {
    const dx = e.changedTouches[0].clientX - sx, dy = e.changedTouches[0].clientY - sy;
    const primary = horizontal ? dx : dy;
    if (Math.abs(primary) > 45) goTo(cur + (primary < 0 ? 1 : -1));
  }, { passive: true });
})();
