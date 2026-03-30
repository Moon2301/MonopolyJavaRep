/**
 * HeroSystem — SVG chibi 72×88, idle bob, token màu theo hero.
 * Key template = _normalize(hero.name) hoặc alias (xem SVG_ALIASES).
 */
const HeroSystem = (() => {
    const SVG_TEMPLATES = {
        "dai gia": (id) => `
<svg width="72" height="88" viewBox="0 0 72 88" fill="none" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="36" cy="83" rx="13" ry="3.5" fill="#00000018" id="shadow-${id}"/>
  <g id="body-${id}">
    <rect x="22" y="9" width="28" height="7" rx="3.5" fill="#2C2C2A"/>
    <rect x="18" y="14" width="36" height="5" rx="2.5" fill="#2C2C2A"/>
    <circle cx="36" cy="27" r="15" fill="#F5C89A" stroke="#D3D1C7" stroke-width="0.5"/>
    <ellipse cx="30" cy="26" rx="2.5" ry="2.5" fill="#2C2C2A"/>
    <ellipse cx="42" cy="26" rx="2.5" ry="2.5" fill="#2C2C2A"/>
    <ellipse cx="30.8" cy="25.3" rx="1" ry="1" fill="white"/>
    <ellipse cx="42.8" cy="25.3" rx="1" ry="1" fill="white"/>
    <path d="M30 33 Q36 37 42 33" stroke="#888780" stroke-width="1.2" fill="none" stroke-linecap="round"/>
    <rect x="22" y="43" width="28" height="26" rx="7" fill="#185FA5"/>
    <rect x="26" y="44" width="4" height="10" rx="2" fill="#B5D4F4" opacity="0.5"/>
    <polygon points="36,45 33,58 36,60 39,58" fill="#0C447C" opacity="0.8"/>
    <rect x="10" y="45" width="13" height="7" rx="3.5" fill="#F5C89A"/>
    <rect x="49" y="45" width="13" height="7" rx="3.5" fill="#F5C89A"/>
    <ellipse cx="14" cy="59" rx="8" ry="9" fill="#FAC775" stroke="#BA7517" stroke-width="0.5"/>
    <text x="10" y="63" font-size="10" fill="#412402" font-weight="500" font-family="sans-serif">$</text>
    <rect x="22" y="66" width="11" height="14" rx="5" fill="#0C447C"/>
    <rect x="39" y="66" width="11" height="14" rx="5" fill="#0C447C"/>
  </g>
</svg>`,

        "chien binh": (id) => `
<svg width="72" height="88" viewBox="0 0 72 88" fill="none" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="36" cy="83" rx="13" ry="3.5" fill="#00000018" id="shadow-${id}"/>
  <g id="body-${id}">
    <path d="M16 42 Q10 68 20 76 L36 72 L52 76 Q62 68 56 42Z" fill="#A32D2D" opacity="0.9"/>
    <rect x="20" y="40" width="32" height="22" rx="6" fill="#E24B4A"/>
    <polygon points="36,44 31,52 36,55 41,52" fill="#FAC775" opacity="0.9"/>
    <rect x="7" y="44" width="15" height="8" rx="4" fill="#FAD0A0"/>
    <rect x="50" y="44" width="15" height="8" rx="4" fill="#FAD0A0"/>
    <rect x="24" y="59" width="9" height="15" rx="4.5" fill="#791F1F"/>
    <rect x="39" y="59" width="9" height="15" rx="4.5" fill="#791F1F"/>
    <circle cx="36" cy="25" r="14" fill="#FAD0A0" stroke="#D3D1C7" stroke-width="0.5"/>
    <rect x="27" y="22" width="6" height="3" rx="1.5" fill="#2C2C2A"/>
    <rect x="39" y="22" width="6" height="3" rx="1.5" fill="#2C2C2A"/>
    <path d="M29 31 Q36 29 43 31" stroke="#888780" stroke-width="1.2" fill="none" stroke-linecap="round"/>
    <path d="M19 18 Q36 10 53 18" stroke="#F09595" stroke-width="4" fill="none" stroke-linecap="round"/>
  </g>
</svg>`,

        "phu thuy": (id) => `
<svg width="72" height="88" viewBox="0 0 72 88" fill="none" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="36" cy="83" rx="13" ry="3.5" fill="#00000018" id="shadow-${id}"/>
  <g id="body-${id}">
    <path d="M20 42 Q14 72 22 78 L36 74 L50 78 Q58 72 52 42Z" fill="#2C2C2A" opacity="0.92"/>
    <text x="24" y="65" font-size="10" fill="#FAC775" font-family="sans-serif">★</text>
    <text x="39" y="72" font-size="8"  fill="#FAC775" font-family="sans-serif">★</text>
    <rect x="22" y="40" width="28" height="20" rx="5" fill="#444441"/>
    <rect x="8"  y="44" width="16" height="7" rx="3.5" fill="#E8C49A"/>
    <rect x="48" y="44" width="16" height="7" rx="3.5" fill="#E8C49A"/>
    <rect x="44" y="42" width="4" height="22" rx="2" fill="#5F5E5A"/>
    <circle cx="46" cy="40" r="5" fill="#1D9E75" opacity="0.9"/>
    <rect x="24" y="57" width="9" height="15" rx="4.5" fill="#2C2C2A"/>
    <rect x="39" y="57" width="9" height="15" rx="4.5" fill="#2C2C2A"/>
    <circle cx="36" cy="24" r="14" fill="#E8C49A" stroke="#D3D1C7" stroke-width="0.5"/>
    <path d="M27 23 Q29.5 21 32 23" stroke="#2C2C2A" stroke-width="1.5" fill="none"/>
    <path d="M40 23 Q42.5 21 45 23" stroke="#2C2C2A" stroke-width="1.5" fill="none"/>
    <circle cx="29.5" cy="23.5" r="1.2" fill="#2C2C2A"/>
    <circle cx="42.5" cy="23.5" r="1.2" fill="#2C2C2A"/>
    <path d="M29 31 Q33 34 43 29" stroke="#888780" stroke-width="1.2" fill="none" stroke-linecap="round"/>
    <polygon points="36,3 26,20 46,20" fill="#2C2C2A"/>
    <rect x="22" y="19" width="28" height="5" rx="2.5" fill="#2C2C2A"/>
    <rect x="24" y="19" width="24" height="3.5" rx="1.5" fill="#EF9F27" opacity="0.7"/>
  </g>
</svg>`,

        "dai gia neon": (id) => `
<svg width="72" height="88" viewBox="0 0 72 88" fill="none" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="36" cy="83" rx="13" ry="3.5" fill="#00000018" id="shadow-${id}"/>
  <g id="body-${id}">
    <rect x="21" y="42" width="30" height="27" rx="7" fill="#534AB7"/>
    <polygon points="30,43 27,55 33,55" fill="#3C3489" opacity="0.9"/>
    <polygon points="42,43 45,55 39,55" fill="#3C3489" opacity="0.9"/>
    <rect x="9"  y="45" width="13" height="7" rx="3.5" fill="#F5C89A"/>
    <rect x="50" y="45" width="13" height="7" rx="3.5" fill="#F5C89A"/>
    <rect x="50" y="54" width="14" height="10" rx="3" fill="#FAC775" stroke="#BA7517" stroke-width="0.5"/>
    <rect x="55" y="52" width="4" height="3" rx="1" fill="#BA7517"/>
    <text x="52" y="62" font-size="8" fill="#412402" font-weight="500" font-family="sans-serif">$$</text>
    <rect x="23" y="66" width="10" height="14" rx="5" fill="#3C3489"/>
    <rect x="39" y="66" width="10" height="14" rx="5" fill="#3C3489"/>
    <circle cx="36" cy="26" r="15" fill="#F5C89A" stroke="#D3D1C7" stroke-width="0.5"/>
    <rect x="24" y="22" width="10" height="6" rx="3" fill="#2C2C2A" opacity="0.85"/>
    <rect x="38" y="22" width="10" height="6" rx="3" fill="#2C2C2A" opacity="0.85"/>
    <line x1="34" y1="25" x2="38" y2="25" stroke="#2C2C2A" stroke-width="1.5"/>
    <path d="M28 33 Q34 37 44 32" stroke="#888780" stroke-width="1.2" fill="none" stroke-linecap="round"/>
    <rect x="23" y="8"  width="26" height="10" rx="2" fill="#2C2C2A"/>
    <rect x="19" y="16" width="34" height="5"  rx="2.5" fill="#2C2C2A"/>
    <rect x="23" y="15" width="26" height="3" fill="#7F77DD"/>
  </g>
</svg>`,

        "chien binh xuc xac": (id) => `
<svg width="72" height="88" viewBox="0 0 72 88" fill="none" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="36" cy="83" rx="13" ry="3.5" fill="#00000018" id="shadow-${id}"/>
  <g id="body-${id}">
    <rect x="19" y="40" width="34" height="26" rx="6" fill="#5F5E5A"/>
    <rect x="22" y="43" width="28" height="6" rx="2" fill="#888780" opacity="0.6"/>
    <rect x="22" y="51" width="28" height="6" rx="2" fill="#888780" opacity="0.5"/>
    <rect x="30" y="57" width="12" height="12" rx="3" fill="#F1EFE8" stroke="#D3D1C7" stroke-width="0.5"/>
    <circle cx="33" cy="60" r="1.2" fill="#2C2C2A"/>
    <circle cx="39" cy="60" r="1.2" fill="#2C2C2A"/>
    <circle cx="36" cy="63" r="1.2" fill="#2C2C2A"/>
    <circle cx="33" cy="66" r="1.2" fill="#2C2C2A"/>
    <circle cx="39" cy="66" r="1.2" fill="#2C2C2A"/>
    <rect x="7"  y="43" width="14" height="9" rx="4" fill="#888780"/>
    <rect x="51" y="43" width="14" height="9" rx="4" fill="#888780"/>
    <rect x="7"  y="51" width="14" height="6" rx="3" fill="#5F5E5A"/>
    <rect x="51" y="51" width="14" height="6" rx="3" fill="#5F5E5A"/>
    <rect x="22" y="63" width="12" height="14" rx="5" fill="#5F5E5A"/>
    <rect x="38" y="63" width="12" height="14" rx="5" fill="#5F5E5A"/>
    <circle cx="36" cy="25" r="14" fill="#FAD0A0" stroke="#D3D1C7" stroke-width="0.5"/>
    <rect x="26" y="22" width="7" height="3.5" rx="1.5" fill="#2C2C2A"/>
    <rect x="39" y="22" width="7" height="3.5" rx="1.5" fill="#2C2C2A"/>
    <line x1="40" y1="20" x2="43" y2="27" stroke="#D85A30" stroke-width="1" stroke-linecap="round"/>
    <path d="M28 31 Q36 33 44 31" stroke="#888780" stroke-width="1.5" fill="none" stroke-linecap="round"/>
    <path d="M22 22 Q22 10 36 9 Q50 10 50 22" fill="#5F5E5A"/>
    <rect x="24" y="20" width="24" height="4" rx="2" fill="#888780" opacity="0.6"/>
  </g>
</svg>`,

        "nu hoang tien thue": (id) => `
<svg width="72" height="88" viewBox="0 0 72 88" fill="none" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="36" cy="83" rx="13" ry="3.5" fill="#00000018" id="shadow-${id}"/>
  <g id="body-${id}">
    <path d="M18 42 Q12 75 22 80 L36 77 L50 80 Q60 75 54 42Z" fill="#D4537E"/>
    <path d="M24 42 Q21 68 26 74 L36 72 L46 74 Q51 68 48 42Z" fill="#ED93B1" opacity="0.6"/>
    <rect x="22" y="38" width="28" height="16" rx="6" fill="#D4537E"/>
    <ellipse cx="36" cy="40" rx="8" ry="3" fill="none" stroke="#FAC775" stroke-width="1.5"/>
    <rect x="9"  y="41" width="14" height="7" rx="3.5" fill="#FAD0A0"/>
    <rect x="49" y="41" width="14" height="7" rx="3.5" fill="#FAD0A0"/>
    <circle cx="36" cy="23" r="14" fill="#FAD0A0" stroke="#D3D1C7" stroke-width="0.5"/>
    <path d="M27 21 Q30 19 33 21" stroke="#2C2C2A" stroke-width="1.8" fill="none" stroke-linecap="round"/>
    <path d="M39 21 Q42 19 45 21" stroke="#2C2C2A" stroke-width="1.8" fill="none" stroke-linecap="round"/>
    <circle cx="30" cy="22" r="1.3" fill="#2C2C2A"/>
    <circle cx="42" cy="22" r="1.3" fill="#2C2C2A"/>
    <path d="M29 30 Q36 34 43 30" stroke="#D4537E" stroke-width="1.2" fill="none" stroke-linecap="round"/>
    <polygon points="20,16 24,8 28,14 36,6 44,14 48,8 52,16" fill="#FAC775" stroke="#BA7517" stroke-width="0.5"/>
    <rect x="20" y="15" width="32" height="5" rx="2" fill="#FAC775" stroke="#BA7517" stroke-width="0.5"/>
    <circle cx="28" cy="12" r="2" fill="#E24B4A"/>
    <circle cx="36" cy="9"  r="2" fill="#1D9E75"/>
    <circle cx="44" cy="12" r="2" fill="#185FA5"/>
  </g>
</svg>`
    };

    /** SVG tĩnh trong `/character/*.svg` — placeholder `__UID__` thay bằng id instance (animation). */
    const CHARACTER_FILE_BY_KEY = {
        "phu loc tho": "phu-loc-tho.svg",
        "than xuc xac": "than-xuc-xac.svg",
        "kien truc su": "kien-truc-su.svg",
        "bong ma toc do": "bong-ma-toc-do.svg",
        "tham tu den": "tham-tu-den.svg",
        "hu vo su": "hu-vo-su.svg",
        "nu hoang hop dong": "nu-hoang-hop-dong.svg",
        "nha tai tro khoi dau": "nha-tai-tro-khoi-dau.svg"
    };

    const _characterSvgCache = new Map();
    let _characterPreloadPromise = null;

    function preloadCharacterSvgs() {
        if (_characterPreloadPromise) {
            return _characterPreloadPromise;
        }
        const files = [...new Set(Object.values(CHARACTER_FILE_BY_KEY))];
        _characterPreloadPromise = Promise.all(
            files.map((file) =>
                fetch(`/character/${file}`, { cache: "force-cache" })
                    .then((r) => (r.ok ? r.text() : null))
                    .then((text) => {
                        if (text) {
                            _characterSvgCache.set(file, text);
                        }
                    })
                    .catch(() => {})
            )
        );
        return _characterPreloadPromise;
    }

    function _svgFromCharacterFile(heroName, uid) {
        const tkey = _templateKeyForName(heroName);
        const file = CHARACTER_FILE_BY_KEY[tkey];
        if (!file || !_characterSvgCache.has(file)) {
            return null;
        }
        return _characterSvgCache.get(file).replace(/__UID__/g, uid);
    }

    /** Alias tùy chọn (để trỏ tên cũ / demo → template khác). */
    const SVG_ALIASES = {};

    const TOKEN_COLORS = {
        "dai gia": { bg: "#185FA5", text: "#E6F1FB" },
        "chien binh": { bg: "#A32D2D", text: "#FCEBEB" },
        "phu thuy": { bg: "#085041", text: "#E1F5EE" },
        "dai gia neon": { bg: "#534AB7", text: "#EEEDFE" },
        "chien binh xuc xac": { bg: "#5F5E5A", text: "#F1EFE8" },
        "nu hoang tien thue": { bg: "#993556", text: "#FBEAF0" },
        "phu loc tho": { bg: "#C62828", text: "#FFEBEE" },
        "than xuc xac": { bg: "#5E35B1", text: "#EDE7F6" },
        "kien truc su": { bg: "#1565C0", text: "#E3F2FD" },
        "bong ma toc do": { bg: "#00838F", text: "#E0F7FA" },
        "tham tu den": { bg: "#37474F", text: "#ECEFF1" },
        "hu vo su": { bg: "#4A148C", text: "#F3E5F5" },
        "nu hoang hop dong": { bg: "#212121", text: "#FFD54F" },
        "nha tai tro khoi dau": { bg: "#2E7D32", text: "#E8F5E9" }
    };
    const DEFAULT_TOKEN = { bg: "#378ADD", text: "#E6F1FB" };

    const IDLE_CSS = `
@keyframes _hero_bob{0%,100%{transform:translateY(0)}50%{transform:translateY(-5px)}}
@keyframes _hero_shadow{0%,100%{transform:scaleX(1);opacity:.18}50%{transform:scaleX(.8);opacity:.10}}
@keyframes _token_land{0%{transform:translateY(-10px);opacity:.6}60%{transform:translateY(2px)}100%{transform:translateY(0);opacity:1}}
@keyframes _trail_fade{0%{opacity:.55;transform:scale(1)}100%{opacity:0;transform:scale(.5)}}
`;

    let _cssInjected = false;
    function _injectCSS() {
        if (_cssInjected) return;
        const style = document.createElement("style");
        style.textContent = IDLE_CSS;
        document.head.appendChild(style);
        _cssInjected = true;
    }

    let _heroes = [];
    let _selectedId = null;
    let _onSelect = null;
    let _tokenPos = 0;
    let _moving = false;

    function heroIdOf(hero) {
        if (!hero) return null;
        return hero.heroId != null ? hero.heroId : hero.characterId;
    }

    function _normalize(name) {
        return String(name || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/đ/g, "d")
            .replace(/\s+/g, " ")
            .trim();
    }

    function _templateKeyForName(heroName) {
        const key = _normalize(heroName);
        return SVG_ALIASES[key] || key;
    }

    function _getSVG(heroName, uid) {
        const fromFile = _svgFromCharacterFile(heroName, uid);
        if (fromFile) {
            return fromFile;
        }
        const tkey = _templateKeyForName(heroName);
        const fn = SVG_TEMPLATES[tkey];
        return fn ? fn(uid) : _fallbackSVG(heroName, uid);
    }

    function _fallbackSVG(name, uid) {
        const letter = String(name || "?")
            .trim()
            .slice(0, 1)
            .toUpperCase();
        return `
<svg width="72" height="88" viewBox="0 0 72 88" fill="none" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="36" cy="83" rx="13" ry="3.5" fill="#00000018" id="shadow-${uid}"/>
  <g id="body-${uid}">
    <circle cx="36" cy="25" r="16" fill="#B5D4F4" stroke="#85B7EB" stroke-width="0.5"/>
    <text x="36" y="31" font-size="18" font-weight="500" text-anchor="middle"
          font-family="sans-serif" fill="#0C447C">${letter}</text>
    <rect x="22" y="42" width="28" height="28" rx="8" fill="#85B7EB"/>
    <rect x="22" y="67" width="11" height="14" rx="5" fill="#185FA5"/>
    <rect x="39" y="67" width="11" height="14" rx="5" fill="#185FA5"/>
  </g>
</svg>`;
    }

    function _getTokenColor(heroName) {
        const key = _normalize(heroName);
        if (TOKEN_COLORS[key]) return TOKEN_COLORS[key];
        const tkey = _templateKeyForName(heroName);
        if (TOKEN_COLORS[tkey]) return TOKEN_COLORS[tkey];
        return DEFAULT_TOKEN;
    }

    function startIdle(uid, delay = 0) {
        _injectCSS();
        const body = document.getElementById(`body-${uid}`);
        const shadow = document.getElementById(`shadow-${uid}`);
        const dur = 1.8 + delay * 0.3;
        if (body) {
            body.style.animation = "none";
            void body.offsetWidth;
            body.style.animation = `_hero_bob ${dur}s ${delay}s ease-in-out infinite`;
        }
        if (shadow) {
            shadow.style.animation = "none";
            void shadow.offsetWidth;
            shadow.style.animation = `_hero_shadow ${dur}s ${delay}s ease-in-out infinite`;
        }
    }

    function stopIdle(uid) {
        const body = document.getElementById(`body-${uid}`);
        const shadow = document.getElementById(`shadow-${uid}`);
        if (body) body.style.animation = "none";
        if (shadow) shadow.style.animation = "none";
    }

    function renderPicker(containerId, onSelect, heroListOverride, options) {
        _onSelect = onSelect || null;
        _injectCSS();
        const opts = options || {};
        const autoFirst = opts.autoSelectFirst !== false;

        const wrap = document.getElementById(containerId);
        if (!wrap) return;

        if (Array.isArray(heroListOverride) && heroListOverride.length) {
            _heroes = heroListOverride;
        }

        const rarityColor = {
            COMMON: "#639922",
            RARE: "#185FA5",
            EPIC: "#534AB7",
            LEGENDARY: "#BA7517",
            MYTHIC: "#9B4DCA"
        };

        wrap.innerHTML = "";
        wrap.style.cssText = "display:flex;gap:10px;flex-wrap:wrap;align-items:flex-start";

        _heroes.forEach((hero, idx) => {
            const hid = heroIdOf(hero);
            const uid = `picker-${hid}`;
            const card = document.createElement("div");
            card.id = `hcard-${hid}`;
            card.style.cssText = [
                "display:flex;flex-direction:column;align-items:center;gap:4px",
                "padding:10px 8px;border-radius:12px;cursor:pointer",
                "border:1.5px solid transparent",
                "transition:border-color .18s,background .18s",
                "min-width:88px"
            ].join(";");

            const rarity = hero.rarity || "";
            card.innerHTML = `
                <div style="line-height:0">${_getSVG(hero.name, uid)}</div>
                <span style="font-size:12px;font-weight:600;color:#2a2438;text-align:center">${hero.name || ""}</span>
                <span style="font-size:10px;color:${rarityColor[rarity] || "#888780"};text-align:center">${rarity}</span>
            `;

            card.addEventListener("click", () => _selectHero(hero));
            wrap.appendChild(card);

            window.setTimeout(() => startIdle(uid, idx * 0.38), 60);
        });

        if (autoFirst && _heroes.length > 0) {
            _selectHero(_heroes[0]);
        }
    }

    function _selectHero(hero) {
        const hid = heroIdOf(hero);
        _selectedId = hid;

        _heroes.forEach((h) => {
            const id = heroIdOf(h);
            const card = document.getElementById(`hcard-${id}`);
            if (!card) return;
            if (id === _selectedId) {
                card.style.borderColor = "#3ea9ff";
                card.style.background = "rgba(62, 169, 255, 0.12)";
            } else {
                card.style.borderColor = "transparent";
                card.style.background = "transparent";
            }
        });

        if (_onSelect) _onSelect(hero);
    }

    function renderToken(heroOrId, cellEl, animate = true) {
        _injectCSS();
        if (!cellEl) return;

        const hero =
            typeof heroOrId === "object"
                ? heroOrId
                : _heroes.find((h) => heroIdOf(h) === heroOrId);
        const name = hero ? hero.name : "";
        const col = _getTokenColor(name);
        const letter = name ? name.slice(0, 1).toUpperCase() : "?";

        const old = cellEl.querySelector(".hero-token");
        if (old) old.remove();

        const tok = document.createElement("div");
        tok.className = "hero-token";
        tok.textContent = letter;
        tok.style.cssText = [
            `background:${col.bg}`,
            `color:${col.text}`,
            "width:22px;height:22px;border-radius:50%",
            "display:flex;align-items:center;justify-content:center",
            "font-size:10px;font-weight:600",
            "border:2px solid #fff",
            "position:absolute;top:4px;left:4px;z-index:10",
            "pointer-events:none"
        ].join(";");

        if (animate) {
            tok.style.animation = "_token_land .35s ease-out forwards";
        }

        cellEl.style.position = "relative";
        cellEl.appendChild(tok);
    }

    function animateMove(heroOrId, steps, getCellEl, onDone, currentPos, totalCells = 40, msPerStep = 260) {
        if (_moving) return;
        _moving = true;
        _injectCSS();

        const startPos = currentPos !== undefined ? currentPos : _tokenPos;
        let done = 0;

        function step() {
            if (done >= steps) {
                _tokenPos = (startPos + steps) % totalCells;
                _moving = false;
                if (onDone) onDone(_tokenPos);
                return;
            }

            const fromIdx = (startPos + done) % totalCells;
            const toIdx = (startPos + done + 1) % totalCells;

            const fromCell = getCellEl(fromIdx);
            if (fromCell) {
                const existing = fromCell.querySelector(".hero-token");
                if (existing) {
                    const trail = existing.cloneNode(true);
                    trail.style.animation = "_trail_fade .4s ease-out forwards";
                    trail.style.zIndex = "8";
                    fromCell.appendChild(trail);
                    window.setTimeout(() => trail.remove(), 450);
                    existing.remove();
                }
            }

            const toCell = getCellEl(toIdx);
            if (toCell) {
                renderToken(heroOrId, toCell, true);
                toCell.scrollIntoView &&
                    toCell.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "center" });
            }

            done += 1;
            window.setTimeout(step, msPerStep);
        }

        step();
    }

    async function init(opts) {
        const headers = { Accept: "application/json" };
        const accountId =
            (opts && opts.accountId) || (typeof localStorage !== "undefined" && localStorage.getItem("accountId"));
        if (accountId) headers["X-Account-Id"] = accountId;
        const url = opts?.ownedOnly ? "/api/heroes/owned" : "/api/heroes";
        try {
            const res = await fetch(url, { headers });
            if (!res.ok) throw new Error("Cannot load heroes");
            _heroes = await res.json();
        } catch (e) {
            console.warn("[HeroSystem]", e.message);
            _heroes = _DEFAULT_HEROES;
        }
        return _heroes;
    }

    function setHeroes(list) {
        _heroes = Array.isArray(list) ? list : [];
    }

    const _DEFAULT_HEROES = [
        { heroId: 1, name: "Dai Gia", rarity: "RARE", baseHp: 80, baseIncomeBonus: 50 },
        { heroId: 2, name: "Chien Binh", rarity: "EPIC", baseHp: 120, baseIncomeBonus: 10 },
        { heroId: 3, name: "Phu Thuy", rarity: "COMMON", baseHp: 70, baseIncomeBonus: 30 },
        { heroId: 4, name: "Dai Gia Neon", rarity: "LEGENDARY", baseHp: 90, baseIncomeBonus: 70 },
        { heroId: 5, name: "Chien Binh Xuc Xac", rarity: "EPIC", baseHp: 130, baseIncomeBonus: 5 },
        { heroId: 6, name: "Nu Hoang Tien Thue", rarity: "LEGENDARY", baseHp: 85, baseIncomeBonus: 60 }
    ];

    function getHeroes() {
        return _heroes;
    }
    function getSelectedId() {
        return _selectedId;
    }
    function getTokenPos() {
        return _tokenPos;
    }
    function isMoving() {
        return _moving;
    }
    function getSVG(name, uid) {
        return _getSVG(name, uid);
    }
    function getTokenColor(name) {
        return _getTokenColor(name);
    }

    return {
        init,
        setHeroes,
        renderPicker,
        renderToken,
        animateMove,
        startIdle,
        stopIdle,
        getSVG,
        getHeroes,
        getSelectedId,
        getTokenPos,
        isMoving,
        getTokenColor,
        heroIdOf,
        normalizeName: _normalize,
        preloadCharacterSvgs
    };
})();

window.HeroSystem = HeroSystem;
window.HeroSystem.preloadCharacterSvgs();
