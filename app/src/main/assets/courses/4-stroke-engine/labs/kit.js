// Blueprint lab kit: theme, a scaled canvas stage with an animation loop, and drawing helpers.
// Each lab draws in its own fixed design size (e.g. 360 x 340); the stage scales it to fit.
const Kit = (() => {
  const P = new URLSearchParams(location.search);
  const theme = P.get('theme') || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  document.documentElement.dataset.theme = theme;

  const style = getComputedStyle(document.documentElement);
  const C = new Proxy({}, { get: (_, k) => style.getPropertyValue('--' + k).trim() });

  function stage(canvas, W, H, draw) {
    const ctx = canvas.getContext('2d');
    let scale = 1;
    function size() {
      const r = canvas.getBoundingClientRect(), d = devicePixelRatio || 1;
      canvas.width = Math.max(1, Math.round(r.width * d));
      canvas.height = Math.max(1, Math.round(r.height * d));
      scale = Math.min(r.width / W, r.height / H);
      const ox = (r.width - W * scale) / 2, oy = (r.height - H * scale) / 2;
      ctx.setTransform(d * scale, 0, 0, d * scale, d * ox, d * oy);
    }
    new ResizeObserver(size).observe(canvas);
    size();
    let last = performance.now();
    function frame(t) {
      // rAF timestamps can be slightly older than performance.now() on the first frame; never go backwards.
      const dt = Math.max(0, Math.min(0.05, (t - last) / 1000));
      last = t;
      ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.restore();
      draw(ctx, dt);
      requestAnimationFrame(frame);
    }
    requestAnimationFrame(frame);
  }

  function rgb(hex) {
    const h = hex.replace('#', '');
    const n = parseInt(h.length === 3 ? h.split('').map(c => c + c).join('') : h, 16);
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
  }
  function mix(a, b, t) {
    const x = rgb(a), y = rgb(b), k = Math.max(0, Math.min(1, t));
    return `rgb(${x.map((v, i) => Math.round(v + (y[i] - v) * k)).join(',')})`;
  }
  function alpha(hex, a) { return `rgba(${rgb(hex).join(',')},${a})`; }

  function rr(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + h, r); ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath();
  }
  function txt(ctx, s, x, y, o = {}) {
    ctx.font = `${o.weight || 500} ${o.size || 11}px ${o.mono ? 'ui-monospace, monospace' : 'system-ui, sans-serif'}`;
    ctx.fillStyle = o.color || C.ink;
    ctx.textAlign = o.align || 'left';
    ctx.textBaseline = o.base || 'middle';
    ctx.fillText(s, x, y);
  }
  // A callout label with a thin leader line to the part it names.
  function label(ctx, s, ax, ay, lx, ly, align) {
    ctx.strokeStyle = C.muted; ctx.lineWidth = 0.8;
    ctx.beginPath(); ctx.moveTo(ax, ay); ctx.lineTo(lx + (align === 'right' ? 3 : -3), ly); ctx.stroke();
    ctx.fillStyle = C.muted; ctx.beginPath(); ctx.arc(ax, ay, 1.8, 0, 7); ctx.fill();
    txt(ctx, s, lx, ly, { align: align === 'right' ? 'right' : 'left', size: 11, weight: 600 });
  }
  const NB = '︎'; // text-style (not emoji) play/pause glyphs
  function playButton(btn, playing) { btn.textContent = playing ? '❚❚' : '▶' + NB; btn.setAttribute('aria-label', playing ? 'Pause' : 'Play'); }

  return { P, C, theme, stage, mix, alpha, rr, txt, label, playButton, NB };
})();
