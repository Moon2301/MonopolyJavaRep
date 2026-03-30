/**
 * coin-system.js
 * Drop vào: src/main/resources/static/js/coin-system.js
 *
 * Cung cấp:
 *   CoinSystem.renderCoin(el, type, size)        — render đồng xu 3D vào element
 *   CoinSystem.renderPile(el, config)             — render chồng xu
 *   CoinSystem.flow(canvas, fromEl, toEl, opts)  — hiệu ứng dòng chảy xu
 *   CoinSystem.burst(canvas, el, type, count)    — hiệu ứng bùng phát xu khi click
 *   CoinSystem.floatText(el, text, color)        — text +200G bay lên
 *
 * Dùng trong game-board.js:
 *   const canvas = document.getElementById('coinCanvas');
 *   const fromEl = document.querySelector('[data-cell-index="0"]');   // ô GO
 *   const toEl   = document.getElementById('playerCard1');
 *   CoinSystem.flow(canvas, fromEl, toEl, { type:'gold', count:8, onDone: () => {} });
 */

const CoinSystem = (() => {

    const COLORS = {
        gold: {
            body:  '#D4A017',
            dark:  '#A07800',
            edge:  '#7A5200',
            shine: 'rgba(255,220,100,0.55)',
            text:  '#7A5200',
            label: 'Vàng',
        },
        silver: {
            body:  '#B8BEC9',
            dark:  '#8B929E',
            edge:  '#3D4552',
            shine: 'rgba(220,224,235,0.55)',
            text:  '#3D4552',
            label: 'Bạc',
        },
        bronze: {
            body:  '#A0673A',
            dark:  '#7A4A22',
            edge:  '#4A2A0E',
            shine: 'rgba(200,160,110,0.5)',
            text:  '#4A2A0E',
            label: 'Đồng',
        },
    };

    const SIZE_MAP = { sm: 32, md: 52, lg: 72, xl: 96 };
    const SYMBOL   = { gold: 'G', silver: 'S', bronze: 'B' };

    let _animRunning = {};

    // ─── Vẽ 1 đồng xu lên Canvas2D ───────────────────────────────────────
    function _drawCoin(ctx, x, y, r, type, spinPhase, opacity = 1) {
        const col    = COLORS[type] || COLORS.gold;
        const scaleX = Math.abs(Math.cos(spinPhase));
        if (scaleX < 0.02) return;

        ctx.save();
        ctx.globalAlpha = opacity;
        ctx.translate(x, y);
        ctx.scale(scaleX, 1);

        ctx.beginPath();
        ctx.ellipse(0, 0, r, r, 0, 0, Math.PI * 2);
        ctx.fillStyle = col.body;
        ctx.fill();
        ctx.strokeStyle = col.dark;
        ctx.lineWidth = Math.max(1, r * 0.1);
        ctx.stroke();

        ctx.beginPath();
        ctx.ellipse(-r * 0.18, -r * 0.15, r * 0.26, r * 0.7, 0, 0, Math.PI * 2);
        ctx.fillStyle = col.shine;
        ctx.fill();

        ctx.restore();
    }

    // ─── renderCoin: inject HTML đồng xu 3D vào el ───────────────────────
    /**
     * @param {Element} el       container element
     * @param {'gold'|'silver'|'bronze'} type
     * @param {'sm'|'md'|'lg'|'xl'} size
     * @param {object} opts      { spin: bool, onClick: fn }
     */
    function renderCoin(el, type = 'gold', size = 'md', opts = {}) {
        const px  = SIZE_MAP[size] || SIZE_MAP.md;
        const col = COLORS[type] || COLORS.gold;
        const sym = SYMBOL[type] || 'G';
        const dur = { gold: 1.6, silver: 1.9, bronze: 2.1 }[type];
        const fs  = Math.round(px * 0.32);

        el.innerHTML = `
<div style="
  position:relative;width:${px}px;height:${px}px;border-radius:50%;
  background:${col.body};border:${Math.max(1.5,px*0.025)}px solid ${col.dark};
  display:flex;align-items:center;justify-content:center;
  overflow:hidden;cursor:${opts.onClick ? 'pointer' : 'default'};
  ${opts.spin !== false ? `animation:_coin_spin ${dur}s ease-in-out infinite;` : ''}
  transform-origin:center;
" class="_coin_el _coin_${type}">
  <div style="
    position:absolute;width:28%;height:86%;border-radius:50%;
    background:rgba(255,255,255,0.22);left:18%;top:7%;pointer-events:none;
  "></div>
  <div style="
    position:absolute;inset:0;border-radius:50%;
    border-bottom:${Math.max(2,px*0.045)}px solid rgba(0,0,0,0.18);
    border-right:1.5px solid rgba(0,0,0,0.1);pointer-events:none;
  "></div>
  <span style="font-size:${fs}px;font-weight:500;color:${col.edge};position:relative;z-index:2;user-select:none;">${sym}</span>
</div>`;

        if (opts.onClick) {
            el.querySelector('._coin_el').addEventListener('click', opts.onClick);
        }

        _injectSpinCSS();
        return el.querySelector('._coin_el');
    }

    // ─── renderPile: chồng xu nhiều lớp ──────────────────────────────────
    /**
     * @param {Element} el
     * @param {object}  config  { layers: [{type, count}], width: px, bob: bool }
     *   layers ví dụ: [{type:'gold',count:4},{type:'silver',count:2},{type:'bronze',count:1}]
     */
    function renderPile(el, config = {}) {
        const layers  = config.layers || [{ type: 'gold', count: 4 }];
        const width   = config.width  || 80;
        const coinH   = Math.round(width * 0.22);
        const bob     = config.bob !== false;

        let allCoins = [];
        layers.forEach(l => {
            for (let i = 0; i < (l.count || 1); i++) allCoins.push(l.type);
        });

        const totalH = coinH * allCoins.length + Math.round(width * 0.12);

        el.style.cssText += `position:relative;width:${width}px;height:${totalH}px;display:inline-block;`;
        if (bob) el.style.animation = '_coin_bob 2.4s ease-in-out infinite';

        el.innerHTML = allCoins.map((type, i) => {
            const col   = COLORS[type] || COLORS.gold;
            const bottom = i * coinH;
            const rot   = ((Math.random() - 0.5) * 6).toFixed(1);
            return `<div style="
  position:absolute;left:0;bottom:${bottom}px;
  width:${width}px;height:${coinH}px;border-radius:50%;
  background:${col.body};border:1.5px solid ${col.dark};
  box-shadow:0 ${Math.round(coinH*0.55)}px 0 ${col.edge};
  transform:rotate(${rot}deg);
"></div>`;
        }).join('');

        _injectSpinCSS();
    }

    // ─── flow: dòng chảy xu từ element này đến element kia ──────────────
    /**
     * @param {HTMLCanvasElement} canvas  canvas overlay (position:absolute, inset:0)
     * @param {Element}           fromEl  element nguồn (dùng để tính tọa độ)
     * @param {Element}           toEl    element đích
     * @param {object}            opts
     *   type        — 'gold'|'silver'|'bronze'  (default 'gold')
     *   count       — số xu bay                 (default 10)
     *   radius      — bán kính xu               (default 9)
     *   speed       — tốc độ [0.01–0.05]         (default 0.022)
     *   arcHeight   — độ cong quỹ đạo            (default -80)
     *   spread      — độ tản xu                  (default 35)
     *   stagger     — ms delay giữa các xu        (default 55)
     *   onDone      — callback() sau khi tất cả đến đích
     */
    function flow(canvas, fromEl, toEl, opts = {}) {
        const type      = opts.type      || 'gold';
        const count     = Math.min(opts.count  || 10, 40);
        const r         = opts.radius    || 9;
        const speed     = opts.speed     || 0.022;
        const arcH      = opts.arcHeight || -80;
        const spread    = opts.spread    || 35;
        const stagger   = opts.stagger   || 55;
        const onDone    = opts.onDone    || null;
        const col       = COLORS[type]   || COLORS.gold;

        const canvasRect = canvas.getBoundingClientRect();
        canvas.width     = canvas.offsetWidth;
        canvas.height    = canvas.offsetHeight;
        const ctx        = canvas.getContext('2d');

        function center(el) {
            const rect = el.getBoundingClientRect();
            return {
                x: rect.left - canvasRect.left + rect.width  / 2,
                y: rect.top  - canvasRect.top  + rect.height / 2,
            };
        }

        const particles = [];
        let arrived = 0;

        for (let i = 0; i < count; i++) {
            setTimeout(() => {
                const from = center(fromEl);
                const to   = center(toEl);
                particles.push({
                    x:         from.x,
                    y:         from.y,
                    startX:    from.x,
                    startY:    from.y,
                    endX:      to.x,
                    endY:      to.y,
                    t:         0,
                    speed:     speed + (Math.random() - 0.5) * 0.008,
                    wobble:    (Math.random() - 0.5) * spread,
                    arcH:      arcH - Math.random() * 30,
                    r:         r + (Math.random() - 0.5) * 3,
                    spin:      Math.random() * Math.PI * 2,
                    spinSpeed: 0.12 + Math.random() * 0.1,
                    done:      false,
                });
            }, i * stagger);
        }

        const id = 'flow_' + Date.now();
        _animRunning[id] = true;

        function tick() {
            if (!_animRunning[id]) return;
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            let anyAlive = false;
            particles.forEach(p => {
                if (p.done) return;
                anyAlive = true;
                p.t = Math.min(1, p.t + p.speed);
                const ease = p.t < 0.5 ? 2 * p.t * p.t : -1 + (4 - 2 * p.t) * p.t;
                p.x  = p.startX + (p.endX - p.startX) * ease;
                const dy = (p.endY - p.startY) * ease + p.arcH * 4 * p.t * (1 - p.t);
                p.y  = p.startY + dy + Math.sin(p.t * Math.PI * 2.5 + p.wobble) * 8;
                p.spin += p.spinSpeed;
                const opacity = p.t > 0.82 ? (1 - p.t) / 0.18 : 1;
                _drawCoin(ctx, p.x, p.y, p.r, type, p.spin, opacity);
                if (p.t >= 1) { p.done = true; arrived++; }
            });

            if (arrived >= count && !anyAlive) {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                delete _animRunning[id];
                if (onDone) onDone();
                return;
            }
            requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);

        return () => { delete _animRunning[id]; };
    }

    // ─── burst: xu bùng phát từ một element ──────────────────────────────
    /**
     * @param {HTMLCanvasElement} canvas
     * @param {Element}           el      element nguồn (tâm bùng phát)
     * @param {'gold'|'silver'|'bronze'} type
     * @param {number}            count   (default 12)
     * @param {object}            opts    { radius, speed, gravity }
     */
    function burst(canvas, el, type = 'gold', count = 12, opts = {}) {
        const canvasRect = canvas.getBoundingClientRect();
        canvas.width     = canvas.offsetWidth;
        canvas.height    = canvas.offsetHeight;
        const ctx        = canvas.getContext('2d');

        const rect = el.getBoundingClientRect();
        const cx = rect.left - canvasRect.left + rect.width  / 2;
        const cy = rect.top  - canvasRect.top  + rect.height / 2;

        const particles = Array.from({ length: count }, () => ({
            x: cx, y: cy,
            vx: (Math.random() - 0.5) * (opts.speed || 7),
            vy: -(1.5 + Math.random() * (opts.speed || 6)),
            r:   (opts.radius || 7) + Math.random() * 4,
            spin: Math.random() * Math.PI * 2,
            spinV: (Math.random() - 0.5) * 0.28,
            life: 1,
            decay: 0.022 + Math.random() * 0.018,
            gravity: opts.gravity || 0.22,
        }));

        const id = 'burst_' + Date.now();
        _animRunning[id] = true;

        function frame() {
            if (!_animRunning[id]) return;
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            let alive = false;
            particles.forEach(p => {
                if (p.life <= 0) return;
                alive = true;
                p.x    += p.vx;
                p.y    += p.vy;
                p.vy   += p.gravity;
                p.vx   *= 0.98;
                p.spin += p.spinV;
                p.life -= p.decay;
                _drawCoin(ctx, p.x, p.y, p.r, type, p.spin, Math.max(0, p.life));
            });
            if (!alive) { ctx.clearRect(0, 0, canvas.width, canvas.height); delete _animRunning[id]; return; }
            requestAnimationFrame(frame);
        }
        requestAnimationFrame(frame);
        return () => { delete _animRunning[id]; };
    }

    // ─── floatText: "+200G" bay lên rồi mờ dần ───────────────────────────
    /**
     * @param {Element} el        element nguồn (tính tọa độ)
     * @param {string}  text      ví dụ "+200G"
     * @param {string}  color     CSS color (default gold)
     */
    function floatText(el, text = '+10G', color = '#D4A017') {
        const rect = el.getBoundingClientRect();
        const span = document.createElement('div');
        span.textContent = text;
        span.style.cssText = [
            `position:fixed`,
            `left:${Math.round(rect.left + rect.width / 2)}px`,
            `top:${Math.round(rect.top)}px`,
            `transform:translateX(-50%)`,
            `color:${color}`,
            `font-size:15px`,
            `font-weight:500`,
            `font-family:sans-serif`,
            `pointer-events:none`,
            `z-index:9999`,
            `animation:_float_text 1.1s ease-out forwards`,
        ].join(';');
        document.body.appendChild(span);
        setTimeout(() => span.remove(), 1200);
        _injectSpinCSS();
    }

    // ─── Stop all running animations ─────────────────────────────────────
    function stopAll() {
        _animRunning = {};
    }

    // ─── CSS injection (once) ─────────────────────────────────────────────
    let _cssInjected = false;
    function _injectSpinCSS() {
        if (_cssInjected) return;
        _cssInjected = true;
        const style = document.createElement('style');
        style.textContent = `
@keyframes _coin_spin{0%,100%{transform:scaleX(1)}50%{transform:scaleX(0.12)}}
@keyframes _coin_bob{0%,100%{transform:translateY(0)}50%{transform:translateY(-4px)}}
@keyframes _float_text{
  0%{transform:translateX(-50%) translateY(0);opacity:1}
  100%{transform:translateX(-50%) translateY(-36px);opacity:0}
}
@media(prefers-reduced-motion:reduce){
  ._coin_el,._coin_pile{animation:none!important}
}`;
        document.head.appendChild(style);
    }

    /**
     * Gắn icon xu vào các ô chỉ định (sảnh / hồ sơ / shop).
     * @param {Array<{ elId: string, type: 'gold'|'silver'|'bronze', size?: string }>} slots
     */
    function initCurrencySlots(slots) {
        if (!Array.isArray(slots)) return;
        slots.forEach((s) => {
            const el = document.getElementById(s.elId);
            if (el) renderCoin(el, s.type || "gold", s.size || "sm", { spin: true });
        });
    }

    return {
        COLORS,
        renderCoin,
        renderPile,
        flow,
        burst,
        floatText,
        stopAll,
        initCurrencySlots,
    };

})();