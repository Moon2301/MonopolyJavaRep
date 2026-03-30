document.addEventListener("DOMContentLoaded", async () => {
    const accountId = localStorage.getItem("accountId");
    const urlParams = new URLSearchParams(window.location.search);
    const gameId = urlParams.get("gameId");
    /** Phòng private — dùng khi ván kết thúc: nút «Chơi lại» về phòng chờ. */
    const roomIdParam = urlParams.get("roomId");
    const layoutMapId = urlParams.get("mapId") || "1";
    const isLiveGame = Boolean(gameId);
    const boardElement = document.getElementById("monopolyBoard");
    const tokensElement = document.getElementById("playerTokens");
    const chatMessagesElement = document.getElementById("chatMessages");
    const chatForm = document.getElementById("chatForm");
    const chatInput = document.getElementById("chatInput");
    const die1 = document.getElementById("die1");
    const die2 = document.getElementById("die2");
    const diceTotal = document.getElementById("diceTotal");
    const rollDiceButton = document.getElementById("rollDiceButton");
    const endTurnButton = document.getElementById("endTurnButton");
    const buyPropertyButton = document.getElementById("buyPropertyButton");
    const upgradePropertyButton = document.getElementById("upgradePropertyButton");
    const payOppRentButton = document.getElementById("payOppRentButton");
    const buybackOppButton = document.getElementById("buybackOppButton");
    const gameActionStatus = document.getElementById("gameActionStatus");
    const currentCellName = document.getElementById("currentCellName");
    const currentCellRent = document.getElementById("currentCellRent");
    const currentCellPriceTax = document.getElementById("currentCellPriceTax");
    const currentCellUpgrade = document.getElementById("currentCellUpgrade");
    const currentCellOwnerBlock = document.getElementById("currentCellOwnerBlock");
    const turnTimerLabel = document.getElementById("turnTimerLabel");
    const turnBannerCurrent = document.getElementById("turnBannerCurrent");
    const turnBannerNext = document.getElementById("turnBannerNext");
    const diceArea = document.getElementById("diceArea");
    const yourTurnToast = document.getElementById("yourTurnToast");
    const turnTopBanner = document.getElementById("turnTopBanner");
    const moneyFxLayer = document.getElementById("moneyFxLayer");
    const boardStageEl = document.querySelector(".board-stage");
    const debtCrisisModal = document.getElementById("debtCrisisModal");
    const debtCrisisSummary = document.getElementById("debtCrisisSummary");
    const debtAssetList = document.getElementById("debtAssetList");
    const debtBankruptButton = document.getElementById("debtBankruptButton");
    const surrenderButton = document.getElementById("surrenderButton");
    const surrenderConfirmModal = document.getElementById("surrenderConfirmModal");
    const surrenderConfirmTextEl = document.getElementById("surrenderConfirmText");
    const surrenderCancelButton = document.getElementById("surrenderCancelButton");
    const surrenderConfirmButton = document.getElementById("surrenderConfirmButton");
    const duelDiceModal = document.getElementById("duelDiceModal");
    const duelDiceSelect1 = document.getElementById("duelDiceSelect1");
    const duelDiceSelect2 = document.getElementById("duelDiceSelect2");
    const duelDiceOkButton = document.getElementById("duelDiceOkButton");
    const duelDiceCancelButton = document.getElementById("duelDiceCancelButton");
    if (debtCrisisModal) {
        debtCrisisModal.hidden = true;
        debtCrisisModal.setAttribute("aria-hidden", "true");
    }
    if (surrenderConfirmModal) {
        surrenderConfirmModal.hidden = true;
        surrenderConfirmModal.setAttribute("aria-hidden", "true");
    }
    if (duelDiceModal) {
        duelDiceModal.hidden = true;
        duelDiceModal.setAttribute("aria-hidden", "true");
    }
    if (duelDiceSelect1 && duelDiceSelect2) {
        for (let v = 1; v <= 6; v += 1) {
            const o1 = document.createElement("option");
            o1.value = String(v);
            o1.textContent = String(v);
            duelDiceSelect1.appendChild(o1);
            const o2 = document.createElement("option");
            o2.value = String(v);
            o2.textContent = String(v);
            duelDiceSelect2.appendChild(o2);
        }
    }

    let turnTimerInterval = null;
    let livePollIntervalId = null;
    let lastLiveState = null;
    let diceRollingTimer = null;
    let yourTurnHideTimer = null;
    let yourTurnHideTimer2 = null;
    let prevMyTurnCouldRoll = false;
    let tokenStepAnimRunning = false;
    let tokenStepAnimGen = 0;
    /** Đỏ, xanh dương, vàng, xanh lá — theo turnOrder 1..4 */
    const PLAYER_PALETTE = ["#e53935", "#1e88e5", "#fdd835", "#43a047"];
    let stateApplyChain = Promise.resolve();
    /** null = hiển thị ô của bạn / ô chính; số = đang hover ô đó */
    let hoverBoardIndex = null;
    let debtActionInFlight = false;
    let surrenderActionInFlight = false;
    /** { skillId: string } khi đang chọn ô mục tiêu */
    let pendingSkillActivation = null;
    let pendingDuelSkillId = null;
    const skillTargetBanner = document.getElementById("skillTargetBanner");
    const gameEndRankingOverlay = document.getElementById("gameEndRankingOverlay");
    const gameEndRankingList = document.getElementById("gameEndRankingList");
    const gameEndRankingPlayAgain = document.getElementById("gameEndRankingPlayAgain");
    const gameEndRankingExitHome = document.getElementById("gameEndRankingExitHome");

    const prefersReducedMotion = () =>
        typeof window.matchMedia === "function" &&
        window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    /** Ô bàn: { name, type, price, color } — nạp từ API / file tĩnh */
    let cells = [];

    const escapeAttr = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/"/g, "&quot;")
            .replace(/</g, "&lt;");

    const escapeHtml = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

    const safePortraitSrc = (url) => {
        if (!url || typeof url !== "string") return "";
        const t = url.trim();
        if (t.startsWith("/") || t.startsWith("http://") || t.startsWith("https://")) return t;
        return "";
    };

    const portraitForPlayer = (player) =>
        safePortraitSrc(player.heroImageUrl) || safePortraitSrc(player.avatarUrl) || "";

    const PIP_SLOTS = {
        1: [4],
        2: [0, 8],
        3: [0, 4, 8],
        4: [0, 2, 6, 8],
        5: [0, 2, 4, 6, 8],
        6: [0, 2, 3, 5, 6, 8]
    };

    const clampDie = (n) => {
        const v = Number(n);
        if (v >= 1 && v <= 6) return v;
        return 1;
    };

    const buildPipFaceHtml = (n) => {
        const v = clampDie(n);
        const on = PIP_SLOTS[v] || PIP_SLOTS[1];
        let html = '<div class="die-pip-grid">';
        for (let i = 0; i < 9; i += 1) {
            const dot = on.includes(i) ? '<span class="pip-dot"></span>' : "";
            html += `<span class="pip-cell">${dot}</span>`;
        }
        html += "</div>";
        return html;
    };

    const setDieVisual = (dieEl, value) => {
        if (!dieEl) return;
        const v = clampDie(value);
        let root = dieEl.querySelector(".die-pip-root");
        if (!root) {
            root = document.createElement("div");
            root.className = "die-pip-root";
            root.setAttribute("aria-hidden", "true");
            dieEl.appendChild(root);
        }
        root.innerHTML = buildPipFaceHtml(v);
        dieEl.setAttribute("data-face", String(v));
    };

    const restartTokenHeroIdle = () => {
        if (!window.HeroSystem) return;
        players.forEach((player) => {
            window.HeroSystem.stopIdle(`tok-${player.id}`);
        });
    };

    const animateTokenSteps = (plans) => {
        const L = cells.length;
        if (!plans.length || !L) return Promise.resolve();

        if (prefersReducedMotion()) {
            plans.forEach((plan) => {
                const p = players.find((x) => x.id === plan.playerId);
                if (p) p.position = plan.to;
            });
            renderTokens();
            restartTokenHeroIdle();
            return Promise.resolve();
        }

        const runGen = tokenStepAnimGen;
        tokenStepAnimRunning = true;
        document.querySelectorAll(".player-token").forEach((el) => el.classList.add("is-step-moving"));

        /** Mỗi ô một bước: cố định thời gian chuyển sang ô kế — nhìn rõ tiến từng ô. */
        const perStepMs = 500;
        const stepDelayMs = perStepMs;
        const stepTransitionMs = Math.max(280, perStepMs - 45);

        const run = async () => {
            for (const plan of plans) {
                if (runGen !== tokenStepAnimGen) break;
                await new Promise((resolve) => {
                    let n = 0;
                    const tick = () => {
                        if (runGen !== tokenStepAnimGen) {
                            resolve();
                            return;
                        }
                        if (n >= plan.steps) {
                            const p = players.find((x) => x.id === plan.playerId);
                            if (p) p.position = plan.to;
                            renderTokens();
                            resolve();
                            return;
                        }
                        n += 1;
                        const p = players.find((x) => x.id === plan.playerId);
                        if (p) p.position = (plan.from + n + L) % L;
                        renderTokens({
                            stepTransitionMs,
                            movingPlayerId: plan.playerId
                        });
                        window.setTimeout(tick, stepDelayMs);
                    };
                    tick();
                });
            }
        };

        return run()
            .catch(() => {})
            .finally(() => {
                document
                    .querySelectorAll(".player-token.is-step-moving")
                    .forEach((el) => el.classList.remove("is-step-moving"));
                tokenStepAnimRunning = false;
                restartTokenHeroIdle();
            });
    };

    const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

    const waitUntilAnimationsIdle = async () => {
        const deadline = Date.now() + 45000;
        while (Date.now() < deadline) {
            if (!tokenStepAnimRunning && !diceRollingTimer) return;
            await sleep(24);
        }
    };

    let boardCoinAnimChain = Promise.resolve();

    const queueBoardCoinAnim = (fn) => {
        boardCoinAnimChain = boardCoinAnimChain.then(
            () =>
                new Promise((resolve) => {
                    fn(() => resolve());
                })
        );
    };

    const getPlayerAnchorEl = (playerId) => {
        const tok = tokensElement?.querySelector(`[data-player-order="${playerId}"]`);
        if (tok) return tok;
        const idx = players.findIndex((x) => x.id === playerId);
        if (idx >= 0) return document.getElementById(`playerCard${idx + 1}`);
        return null;
    };

    const getCellAnchoredEl = (cellIndex) => {
        if (!boardElement || cellIndex == null || !cells.length) return null;
        const L = cells.length;
        const i = ((Number(cellIndex) % L) + L) % L;
        return boardElement.querySelector(`[data-cell-index="${i}"]`);
    };

    const bronzeFlowBase = (amount) => ({
        type: "bronze",
        count: Math.min(36, Math.max(6, Math.floor(Math.abs(amount) / 70) + 6)),
        arcHeight: -85,
        stagger: 48,
        speed: 0.024
    });

    /**
     * Tiền ván: đồng đỏ (bronze). Trả cho người khác: quân cờ → ô giao dịch → người nhận.
     * Ngân hàng (thuế/mua/…): quân cờ → ô. Nhận từ ngân hàng/thẻ: ô → quân cờ.
     */
    const runBoardCoinAnimations = (prevBal, list, landingPos) => {
        if (prefersReducedMotion()) return;
        const canvas = document.getElementById("coinCanvas");
        if (!canvas || !window.CoinSystem) {
            legacyMoneyParticleFallback(prevBal, list);
            return;
        }

        const deltas = [];
        list.forEach((p) => {
            const ob = prevBal[p.id];
            if (ob == null) return;
            const d = p.money - ob;
            if (d === 0) return;
            deltas.push({
                id: p.id,
                d,
                amt: Math.abs(d),
                pos: landingPos[p.id] ?? p.position
            });
        });
        if (!deltas.length) return;

        const losers = deltas.filter((x) => x.d < 0);
        const gainers = deltas.filter((x) => x.d > 0);
        const usedG = new Set();
        const usedL = new Set();

        losers.forEach((L, li) => {
            if (usedL.has(li)) return;
            const gi = gainers.findIndex((G, j) => !usedG.has(j) && G.amt === L.amt && G.id !== L.id);
            if (gi >= 0) {
                usedL.add(li);
                usedG.add(gi);
                const G = gainers[gi];
                queueBoardCoinAnim((done) => {
                    const fromEl = getPlayerAnchorEl(L.id);
                    const cellEl = getCellAnchoredEl(L.pos);
                    const toEl = getPlayerAnchorEl(G.id);
                    if (!fromEl || !cellEl || !toEl) {
                        done();
                        return;
                    }
                    CoinSystem.flow(canvas, fromEl, cellEl, {
                        ...bronzeFlowBase(L.amt),
                        onDone: () => {
                            CoinSystem.flow(canvas, cellEl, toEl, {
                                ...bronzeFlowBase(L.amt),
                                onDone: () => {
                                    CoinSystem.floatText(toEl, `+${L.amt}`, "#E6C200");
                                    done();
                                }
                            });
                        }
                    });
                });
            }
        });

        losers.forEach((L, li) => {
            if (usedL.has(li)) return;
            queueBoardCoinAnim((done) => {
                const fromEl = getPlayerAnchorEl(L.id);
                const cellEl = getCellAnchoredEl(L.pos);
                if (!fromEl || !cellEl) {
                    done();
                    return;
                }
                CoinSystem.flow(canvas, fromEl, cellEl, {
                    ...bronzeFlowBase(L.amt),
                    onDone: () => {
                        CoinSystem.floatText(fromEl, `-${L.amt}`, "#8B4513");
                        done();
                    }
                });
            });
        });

        gainers.forEach((G, gi) => {
            if (usedG.has(gi)) return;
            queueBoardCoinAnim((done) => {
                const cellEl = getCellAnchoredEl(G.pos);
                const toEl = getPlayerAnchorEl(G.id);
                if (!cellEl || !toEl) {
                    done();
                    return;
                }
                CoinSystem.flow(canvas, cellEl, toEl, {
                    ...bronzeFlowBase(G.amt),
                    onDone: () => {
                        CoinSystem.floatText(toEl, `+${G.amt}`, "#E6C200");
                        done();
                    }
                });
            });
        });
    };

    const legacyMoneyParticleFallback = (prevBal, list) => {
        if (!moneyFxLayer || !boardStageEl || prefersReducedMotion()) return;
        const stageRect = boardStageEl.getBoundingClientRect();
        list.forEach((p) => {
            const ob = prevBal[p.id];
            if (ob == null) return;
            const diff = p.money - ob;
            if (diff === 0) return;
            const cardIdx = players.findIndex((x) => x.id === p.id);
            if (cardIdx < 0) return;
            const card = document.getElementById(`playerCard${cardIdx + 1}`);
            if (!card) return;
            const crect = card.getBoundingClientRect();
            const centerX = stageRect.left + stageRect.width / 2;
            const centerY = stageRect.top + stageRect.height * 0.42;
            const fromX = crect.left + crect.width / 2;
            const fromY = crect.top + crect.height / 2;
            const spend = diff < 0;
            const startX = spend ? fromX : centerX;
            const startY = spend ? fromY : centerY;
            const endX = spend ? centerX : fromX;
            const endY = spend ? centerY : fromY;
            const count = Math.min(16, Math.max(5, Math.floor(Math.abs(diff) / 180) + 5));
            for (let i = 0; i < count; i += 1) {
                const coin = document.createElement("span");
                coin.className = "money-coin-particle";
                coin.textContent = "Đ";
                const ox = (Math.random() - 0.5) * 40;
                const oy = (Math.random() - 0.5) * 28;
                coin.style.left = `${startX - stageRect.left + ox}px`;
                coin.style.top = `${startY - stageRect.top + oy}px`;
                moneyFxLayer.appendChild(coin);
                window.requestAnimationFrame(() => {
                    coin.style.setProperty("--tx", `${endX - startX - ox}px`);
                    coin.style.setProperty("--ty", `${endY - startY - oy}px`);
                    coin.classList.add("money-coin-particle--fly");
                });
                window.setTimeout(() => coin.remove(), 720);
            }
        });
    };

    const stopTurnTimer = () => {
        if (turnTimerInterval) {
            clearInterval(turnTimerInterval);
            turnTimerInterval = null;
        }
    };

    const updateTurnTimerDisplay = (seconds, show) => {
        if (!turnTimerLabel) return;
        const timerRow = turnTimerLabel.closest(".turn-top-timer");
        if (!show || seconds == null) {
            turnTimerLabel.textContent = "";
            turnTimerLabel.classList.remove("urgent");
            if (timerRow) timerRow.hidden = true;
            return;
        }
        const s = Math.max(0, seconds);
        if (timerRow) timerRow.hidden = false;
        turnTimerLabel.textContent = s <= 0 ? "Hết giờ" : `Còn ${s} giây`;
        turnTimerLabel.classList.toggle("urgent", s <= 5);
    };

    const mapRowTypeToDisplay = (t) => {
        if (!t) return "property";
        const u = String(t).toUpperCase();
        if (u.includes("START") || u === "GO") return "start";
        if (u.includes("CHANCE") || u.includes("COMMUNITY")) return "chance";
        if (u.includes("TAX")) return "tax";
        if (u.includes("JAIL")) return "jail";
        if (u.includes("FREE")) return "free";
        return "property";
    };

    const layoutItemToCell = (item) => {
        const displayType = item.displayType || mapRowTypeToDisplay(item.type);
        const price =
            item.priceLabel != null && item.priceLabel !== ""
                ? item.priceLabel
                : item.price != null
                  ? `$${item.price}`
                  : "";
        const cid =
            item.cellId != null && item.cellId !== ""
                ? Number(item.cellId)
                : item.cell_id != null
                  ? Number(item.cell_id)
                  : null;
        return {
            name: item.name,
            type: displayType,
            price,
            cellId: Number.isFinite(cid) ? cid : null,
            baseRent: item.baseRent != null && item.baseRent !== "" ? Number(item.baseRent) : null,
            color: item.colorHex || ""
        };
    };

    const loadBoardLayout = async () => {
        try {
            const api = await fetch(`/api/board/maps/${layoutMapId}/layout`);
            if (api.ok) {
                const data = await api.json();
                if (Array.isArray(data) && data.length > 0) {
                    cells = data.map(layoutItemToCell);
                    return;
                }
            }
        } catch (e) {
            /* fallback */
        }
        try {
            const st = await fetch("/seed/board-classic.json");
            if (st.ok) {
                const raw = await st.json();
                if (Array.isArray(raw) && raw.length > 0) {
                    cells = raw.map((row) => layoutItemToCell(row));
                }
            }
        } catch (e2) {
            /* ignore */
        }
    };

    const defaultPlayers = [
        {
            id: 1,
            username: "Player1",
            money: 1000,
            color: PLAYER_PALETTE[0],
            avatar: "P1",
            position: 0,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        },
        {
            id: 2,
            username: "Player2",
            money: 1000,
            color: PLAYER_PALETTE[1],
            avatar: "P2",
            position: 7,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        },
        {
            id: 3,
            username: "Player3",
            money: 1000,
            color: PLAYER_PALETTE[2],
            avatar: "P3",
            position: 18,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        },
        {
            id: 4,
            username: "Player4",
            money: 1000,
            color: PLAYER_PALETTE[3],
            avatar: "P4",
            position: 28,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        }
    ];
    const isBotMode = urlParams.get("vsBot") === "1";
    const difficulty = (urlParams.get("difficulty") || localStorage.getItem("aiDifficulty") || "easy").toLowerCase();
    const botLabel = difficulty === "hard" ? "Bot khó" : "Bot dễ";

    const players = isBotMode
        ? [
            {
                id: 1,
                username: "You",
                money: 1000,
                color: PLAYER_PALETTE[0],
                avatar: "ME",
                position: 0,
                avatarUrl: "/images/avatar-default.png",
                heroImageUrl: null,
                heroName: null,
                isBot: false
            },
            {
                id: 2,
                username: botLabel,
                money: difficulty === "hard" ? 1200 : 1000,
                color: PLAYER_PALETTE[1],
                avatar: difficulty === "hard" ? "H" : "E",
                position: 7,
                avatarUrl: null,
                heroImageUrl: "/images/heroes/bot.png",
                heroName: null,
                isBot: true
            }
        ]
        : defaultPlayers;

    const chatMessages = [];

    let activePlayerIndex = 0;

    const setActionStatus = (message, isError = false) => {
        if (!gameActionStatus) return;
        gameActionStatus.textContent = message;
        gameActionStatus.style.color = isError ? "#ffd2d2" : "#fff8ed";
    };

    const authHeaders = (includeJson = false) => {
        const headers = {};
        if (accountId) {
            headers["X-Account-Id"] = accountId;
        }
        if (includeJson) {
            headers["Content-Type"] = "application/json";
        }
        return headers;
    };

    const formatMoney = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const applyBoardOwnership = (state) => {
        if (!boardElement) return;
        boardElement.querySelectorAll(".board-cell[data-cell-index]").forEach((el) => {
            el.classList.remove("board-cell--owned");
            el.style.removeProperty("--owner-tint");
        });
        if (!state?.ownedCells?.length) return;
        const byIdx = new Map(state.ownedCells.map((o) => [o.boardIndex, o.ownerTurnOrder]));
        boardElement.querySelectorAll(".board-cell[data-cell-index]").forEach((el) => {
            const idx = parseInt(el.getAttribute("data-cell-index"), 10);
            const turn = byIdx.get(idx);
            if (turn == null) return;
            const c = PLAYER_PALETTE[Math.max(0, turn - 1) % PLAYER_PALETTE.length];
            el.classList.add("board-cell--owned");
            el.style.setProperty("--owner-tint", c);
        });
    };

    const updateActiveTurnHighlight = (state) => {
        for (let i = 1; i <= 4; i += 1) {
            const el = document.getElementById(`playerCard${i}`);
            if (!el) continue;
            el.classList.remove("player-info--active-turn");
            el.style.removeProperty("--turn-ring");
        }
        const playing = String(state?.status || "").toUpperCase() === "PLAYING";
        if (!playing || state?.currentPlayerOrder == null) return;
        const idx = players.findIndex((p) => p.id === state.currentPlayerOrder);
        if (idx < 0) return;
        const card = document.getElementById(`playerCard${idx + 1}`);
        if (!card) return;
        const p = players[idx];
        card.classList.add("player-info--active-turn");
        card.style.setProperty("--turn-ring", p.color || PLAYER_PALETTE[idx % PLAYER_PALETTE.length]);
    };

    const getPlayerLabelByOrder = (order) => {
        const p = players.find((x) => x.id === order);
        if (p) return p.username;
        return `Player ${order}`;
    };

    /** Người “bạn” trên client: ưu tiên người không phải bot. */
    const getMyPlayer = () => players.find((p) => !p.isBot) || players[0];

    const parsePriceFromLayout = (priceStr) => {
        if (priceStr == null) return null;
        const m = String(priceStr).replace(/,/g, "").match(/(\d+)/);
        return m ? parseInt(m[1], 10) : null;
    };

    const ownerTurnAtIndex = (state, boardIdx) => {
        const hit = state?.ownedCells?.find((o) => o.boardIndex === boardIdx);
        return hit?.ownerTurnOrder ?? null;
    };

    /**
     * currentCell API chỉ mô tả đúng ô này khi trùng vị trí người đang tới lượt (server).
     */
    const apiCellMatchesBoardIndex = (state, boardIdx) => {
        if (!state?.currentCell) return false;
        const cur = players.find((p) => p.id === state.currentPlayerOrder);
        if (!cur || cur.position !== boardIdx) return false;
        return true;
    };

    const isTaxLike = (layout, api) => {
        if (api?.type && String(api.type).toUpperCase().includes("TAX")) return true;
        return layout?.type === "tax";
    };

    const isCellPurchasable = (layout, api, useApi) => {
        if (!layout) return false;
        if (useApi && api && typeof api.purchasable === "boolean") {
            return api.purchasable;
        }
        return layout.type === "property";
    };

    const skillShortLabel = (name) => {
        const s = String(name || "?").trim();
        return s.length <= 2 ? s : s.slice(0, 2);
    };

    const refreshCellInfoPanel = (state) => {
        const s = state ?? lastLiveState;
        const L = cells.length;
        const turnP =
            isLiveGame && s?.currentPlayerOrder != null
                ? players.find((p) => p.id === s.currentPlayerOrder)
                : null;
        const idx =
            hoverBoardIndex != null && hoverBoardIndex >= 0
                ? hoverBoardIndex
                : isLiveGame && turnP
                  ? turnP.position
                  : Math.max(0, players[activePlayerIndex]?.position ?? 0);
        const safeIdx = L ? ((idx % L) + L) % L : 0;
        const layout = cells[safeIdx];

        if (!layout) {
            if (currentCellName) currentCellName.textContent = "—";
            if (currentCellRent) currentCellRent.textContent = "—";
            if (currentCellPriceTax) currentCellPriceTax.textContent = "—";
            if (currentCellUpgrade) currentCellUpgrade.textContent = "—";
            if (currentCellOwnerBlock) currentCellOwnerBlock.textContent = "—";
            return;
        }

        const useApi = apiCellMatchesBoardIndex(s, safeIdx);
        const api = useApi ? s.currentCell : null;
        const purchasable = isCellPurchasable(layout, api, useApi);

        if (currentCellName) {
            currentCellName.textContent = api?.name || layout.name || "—";
        }

        if (currentCellRent) {
            if (api) {
                if (isTaxLike(layout, api)) {
                    currentCellRent.textContent = "Thuê ô: không áp dụng";
                } else if (api.estimatedRent != null) {
                    currentCellRent.textContent = `Thuê (ước): ${formatMoney(api.estimatedRent)}`;
                } else {
                    currentCellRent.textContent = "Thuê: —";
                }
            } else if (layout.type === "property" && layout.baseRent != null) {
                currentCellRent.textContent = `Thuê (cơ sở): ${formatMoney(layout.baseRent)}`;
            } else if (layout.type === "tax") {
                currentCellRent.textContent = "Thuê ô: không áp dụng";
            } else {
                currentCellRent.textContent = "Thuê: không áp dụng";
            }
        }

        if (currentCellPriceTax) {
            if (api) {
                if (isTaxLike(layout, api)) {
                    currentCellPriceTax.textContent = `Thuế / phí khi vào ô: ${formatMoney(api.price)}`;
                } else if (api.price != null && api.price > 0) {
                    currentCellPriceTax.textContent = `Giá mua: ${formatMoney(api.price)}`;
                } else {
                    currentCellPriceTax.textContent = layout.price || "Không áp dụng giá mua";
                }
            } else if (layout.type === "tax") {
                currentCellPriceTax.textContent = layout.price
                    ? `Thuế / phí (ước tính): ${layout.price}`
                    : "Thuế — xem chi tiết khi chơi online";
            } else {
                const n = parsePriceFromLayout(layout.price);
                currentCellPriceTax.textContent =
                    n != null ? `Giá (bố cục): ${formatMoney(n)}` : layout.price || "—";
            }
        }

        if (currentCellUpgrade) {
            if (api && layout.type === "property") {
                const hv =
                    api.houseValue != null
                        ? `Giá nhà: ${formatMoney(api.houseValue)} · `
                        : "";
                currentCellUpgrade.textContent = `${hv}Cấp: ${api.houseLevel ?? 0} · Nâng cấp kế: ${formatMoney(
                    api.upgradeCost ?? 0
                )}`;
            } else if (layout.type === "property") {
                const n = parsePriceFromLayout(layout.price);
                const guess = n != null ? Math.max(100, Math.floor(n * 0.5)) : null;
                currentCellUpgrade.textContent =
                    guess != null
                        ? `Cấp: — · Ước phí nâng cấp: ${formatMoney(guess)} (khi tới lượt)`
                        : "Chi tiết cấp khi bạn đứng trên ô này trong lượt online";
            } else {
                currentCellUpgrade.textContent = "Không nâng cấp";
            }
        }

        if (currentCellOwnerBlock) {
            if (!purchasable) {
                currentCellOwnerBlock.textContent = "không thể mua";
            } else {
                const ot = api?.ownerTurnOrder ?? ownerTurnAtIndex(s, safeIdx);
                currentCellOwnerBlock.textContent = ot ? getPlayerLabelByOrder(ot) : "Chưa có";
            }
        }
    };

    const setSkillTargetPickMode = (active) => {
        if (!boardElement) return;
        boardElement.querySelectorAll(".board-cell").forEach((el) => {
            el.classList.toggle("board-cell--skill-pick", Boolean(active));
        });
        if (skillTargetBanner) {
            skillTargetBanner.hidden = !active;
            skillTargetBanner.setAttribute("aria-hidden", active ? "false" : "true");
        }
    };

    const attachBoardCellHover = () => {
        if (!boardElement) return;
        boardElement.querySelectorAll(".board-cell[data-cell-index]").forEach((el) => {
            if (el.dataset.hoverBound === "1") return;
            el.dataset.hoverBound = "1";
            el.addEventListener("mouseenter", () => {
                const v = parseInt(el.getAttribute("data-cell-index"), 10);
                hoverBoardIndex = Number.isFinite(v) ? v : null;
                refreshCellInfoPanel(lastLiveState);
            });
            el.addEventListener("mouseleave", () => {
                hoverBoardIndex = null;
                refreshCellInfoPanel(lastLiveState);
            });
        });
    };

    const updateTurnBanner = (state) => {
        if (!turnBannerCurrent || !turnBannerNext) return;
        const s = state !== undefined && state !== null ? state : lastLiveState;
        if (s && String(s.status || "").toUpperCase() === "FINISHED") {
            turnBannerCurrent.textContent = "Ván kết thúc";
            turnBannerNext.textContent = "—";
            return;
        }
        const curOrder =
            s?.currentPlayerOrder ??
            (players.length ? players[Math.min(activePlayerIndex, players.length - 1)]?.id : null);
        if (curOrder == null || !players.length) {
            turnBannerCurrent.textContent = "—";
            turnBannerNext.textContent = "—";
            return;
        }
        const orders = [...new Set(players.map((p) => p.id))].sort((a, b) => a - b);
        const idx = Math.max(0, orders.indexOf(curOrder));
        const nextOrder = orders.length ? orders[(idx + 1) % orders.length] : curOrder;
        turnBannerCurrent.textContent = getPlayerLabelByOrder(curOrder);
        turnBannerNext.textContent = getPlayerLabelByOrder(nextOrder);
    };

    /**
     * Đúng 40 ô viền lưới 11×11 (hàng 1 = trên, cột 1 = trái, hàng 11 = dưới).
     * Đi ngược chiều kim đồng hồ từ GO (11,11): cạnh dưới đủ 11 ô | trái 10 | trên 10 | phải 9 → 11+10+10+9 = 40.
     */
    const getBoardPlacement = (index) => {
        const i = ((index % 40) + 40) % 40;
        if (i <= 10) {
            return { row: 11, col: 11 - i };
        }
        if (i <= 20) {
            return { row: 21 - i, col: 1 };
        }
        if (i <= 30) {
            return { row: 1, col: i - 19 };
        }
        return { row: i - 29, col: 11 };
    };

    const getCellClass = (cell) => {
        if (cell.type === "property") return "cell-property";
        if (cell.type === "chance") return "cell-chance";
        if (cell.type === "tax") return "cell-tax";
        if (cell.type === "jail") return "cell-jail";
        if (cell.type === "free") return "cell-free";
        return "cell-start";
    };

    const buildBoard = () => {
        if (!boardElement) return;
        if (!cells.length) {
            boardElement.classList.add("board-is-empty");
            boardElement.querySelectorAll(".board-cell").forEach((n) => n.remove());
            if (!boardElement.querySelector(".board-empty-msg")) {
                const msg = document.createElement("div");
                msg.className = "board-empty-msg";
                msg.textContent = "Không tải được dữ liệu bàn cờ. Hãy chạy backend và seed map.";
                boardElement.appendChild(msg);
            }
            if (tokensElement) boardElement.appendChild(tokensElement);
            return;
        }
        boardElement.classList.remove("board-is-empty");
        boardElement.querySelector(".board-empty-msg")?.remove();
        boardElement.querySelectorAll(".board-cell").forEach((n) => n.remove());

        cells.forEach((cell, index) => {
            const placement = getBoardPlacement(index);
            const div = document.createElement("div");
            div.className = `board-cell ${getCellClass(cell)}`;
            div.setAttribute("data-cell-index", String(index));
            if (cell.cellId != null && Number.isFinite(cell.cellId)) {
                div.setAttribute("data-cell-id", String(cell.cellId));
            }
            div.style.gridRow = String(placement.row);
            div.style.gridColumn = String(placement.col);
            const sub = cell.price || String(cell.type || "").toUpperCase();
            div.innerHTML = `
                <strong>${escapeHtml(cell.name)}</strong>
                <small>${escapeHtml(sub)}</small>
            `;
            boardElement.appendChild(div);
        });

        if (tokensElement) boardElement.appendChild(tokensElement);
        attachBoardCellHover();
        refreshCellInfoPanel(lastLiveState);
    };

    const buildPlayerCards = () => {
        for (let index = 0; index < 4; index += 1) {
            const card = document.getElementById(`playerCard${index + 1}`);
            if (!card) continue;
            if (index >= players.length) {
                card.innerHTML = "";
                card.classList.remove("player-info--active-turn");
                card.classList.remove("player-info--bankrupt");
                card.style.removeProperty("--turn-ring");
                card.style.visibility = "hidden";
                continue;
            }
            card.style.visibility = "";
            const player = players[index];
            card.classList.toggle("player-info--bankrupt", Boolean(player.isBankrupt));
            const src = portraitForPlayer(player);
            let chipInner;
            if (src) {
                chipInner = `<img class="player-chip-img" src="${escapeAttr(src)}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'"><span class="player-chip-fallback" style="display:none">${escapeHtml(player.avatar)}</span>`;
            } else if (window.HeroSystem && player.heroName) {
                chipInner = `<div class="player-chip-svg">${window.HeroSystem.getSVG(player.heroName, `card-${player.id}`)}</div>`;
            } else {
                chipInner = `<span class="player-chip-fallback">${escapeHtml(player.avatar)}</span>`;
            }

            const myTurnNow = lastLiveState?.myTurn === true;
            const skillsLines = (player.skills || [])
                .map((s) => {
                    const passive =
                        s.passiveActive ||
                        (s.triggerType && String(s.triggerType).toUpperCase().includes("PASSIVE"));
                    const title = `${s.name || "Skill"}${s.description ? " — " + s.description : ""}`;
                    if (passive) {
                        return `<span class="player-skill-chip player-skill-chip--passive" title="${escapeAttr(
                            title
                        )}">${escapeHtml(skillShortLabel(s.name))}</span>`;
                    }
                    const canClick =
                        isLiveGame &&
                        myTurnNow &&
                        !player.isBot &&
                        player.id === lastLiveState?.currentPlayerOrder &&
                        s.readyToActivate &&
                        s.skillId != null;
                    const dis = canClick ? "" : " disabled";
                    const reqT = s.requiresTarget === true ? "1" : "";
                    const reqDice = s.requiresDiceChoice === true ? "1" : "";
                    return `<button type="button" class="player-skill-chip player-skill-chip--active"${dis} data-skill-id="${escapeAttr(
                        String(s.skillId ?? "")
                    )}" data-requires-target="${reqT}" data-requires-dice="${reqDice}" title="${escapeAttr(title)}">${escapeHtml(skillShortLabel(s.name))}</button>`;
                })
                .join("");
            const jailBadge = player.inJail
                ? `<div class="player-jail-badge" title="Đang trong tù">Tù · ${player.jailFailedRolls ?? 0}/3</div>`
                : "";

            card.innerHTML = `
                <div class="player-card-main">
                    <div class="player-chip-wrap player-chip-wrap--animated">
                        <div class="player-chip player-chip--portrait" style="--chip-ring:${player.color}">
                            ${chipInner}
                        </div>
                    </div>
                    <div class="player-meta">
                        <strong>${escapeHtml(player.username)}</strong>
                        <span class="player-board-money" title="Tiền mặt trong ván (đồng đỏ)">
                            <span class="player-board-money__icon-host" data-board-money-icon="${escapeAttr(
                                String(player.id)
                            )}"></span>
                            <strong class="player-board-money__value">${formatMoney(player.money)}</strong>
                        </span>
                        ${jailBadge}
                    </div>
                </div>
                <div class="player-skills" aria-label="Kỹ năng">${skillsLines || '<span class="player-skill-empty">—</span>'}</div>
            `;
        }

        if (window.HeroSystem) {
            window.requestAnimationFrame(() => {
                players.forEach((player, idx) => {
                    if (!portraitForPlayer(player) && player.heroName) {
                        window.HeroSystem.startIdle(`card-${player.id}`, idx * 0.08);
                    }
                });
            });
        }
        if (window.CoinSystem) {
            window.requestAnimationFrame(() => {
                players.forEach((player) => {
                    const host = document.querySelector(`[data-board-money-icon="${player.id}"]`);
                    if (host) {
                        host.innerHTML = "";
                        CoinSystem.renderCoin(host, "bronze", "sm", { spin: false });
                    }
                });
            });
        }
    };

    const renderChat = () => {
        if (!chatMessagesElement) return;
        chatMessagesElement.innerHTML = chatMessages
            .map((item) => {
                const sys = item.system ? " chat-bubble--system" : "";
                return `
            <article class="chat-bubble${sys}">
                <strong>${escapeHtml(item.user)}</strong>
                <p>${escapeHtml(item.message)}</p>
            </article>`;
            })
            .join("");
        chatMessagesElement.scrollTop = chatMessagesElement.scrollHeight;
    };

    const appendRentNoticesToChat = (state) => {
        const list = state?.rentNotices;
        if (!list || !list.length) return;
        for (const n of list) {
            const payer = n.payerName || "?";
            const owner = n.ownerName || "?";
            const cell = n.cellName || "ô";
            const amt = n.amountPaid ?? 0;
            const msg = `${payer} đã tiêu ${formatMoney(amt)} tiền ở ${cell} (trả cho ${owner}).`;
            chatMessages.push({ user: "Hệ thống", message: msg, system: true });
        }
        renderChat();
    };

    const appendGameLogsToChat = (state) => {
        const lines = state?.gameLogLines;
        if (!lines || !lines.length) return;
        for (const line of lines) {
            if (typeof line === "string" && line.trim()) {
                chatMessages.push({ user: "Hệ thống", message: line.trim(), system: true });
            }
        }
        renderChat();
    };

    /** Tọa độ layout (px) từ góc trên-trái của ancestor (bỏ qua transform màn hình). */
    const offsetRelToAncestor = (el, ancestor) => {
        let left = 0;
        let top = 0;
        let n = el;
        while (n && n !== ancestor) {
            left += n.offsetLeft;
            top += n.offsetTop;
            n = n.offsetParent;
        }
        return n === ancestor ? { left, top } : null;
    };

    /** Tọa độ ô trong lớp #playerTokens (cùng hệ với left/top của quân). */
    const getCellRect = (index) => {
        const cell = boardElement.querySelector(`[data-cell-index="${index}"]`);
        if (!cell || !tokensElement) {
            return null;
        }
        const cellRel = offsetRelToAncestor(cell, boardElement);
        const tokRel = offsetRelToAncestor(tokensElement, boardElement);
        if (cellRel && tokRel) {
            return {
                left: cellRel.left - tokRel.left,
                top: cellRel.top - tokRel.top,
                width: cell.offsetWidth,
                height: cell.offsetHeight
            };
        }
        const tr = tokensElement.getBoundingClientRect();
        const cr = cell.getBoundingClientRect();
        return {
            left: cr.left - tr.left,
            top: cr.top - tr.top,
            width: cr.width,
            height: cr.height
        };
    };

    const buildTokenLayouts = () => {
        const byPos = {};
        players.forEach((p) => {
            byPos[p.position] = byPos[p.position] || [];
            byPos[p.position].push(p);
        });
        const out = [];
        for (const player of players) {
            const rect = getCellRect(player.position);
            if (!rect) continue;
            const stack = byPos[player.position] || [player];
            const stackIdx = stack.indexOf(player);
            const n = stack.length;
            const cellCx = rect.left + rect.width / 2;
            const cellCy = rect.top + rect.height / 2;
            let offX = 0;
            let offY = 0;
            if (n === 2) {
                const d = Math.min(16, rect.width * 0.12);
                if (stackIdx === 0) {
                    offX = -d;
                    offY = -d;
                } else {
                    offX = d;
                    offY = d;
                }
            } else if (n > 2) {
                const r = Math.min(18, rect.width * 0.14);
                const angle = (stackIdx / n) * Math.PI * 2 - Math.PI / 2;
                offX = Math.cos(angle) * r;
                offY = Math.sin(angle) * r;
            }
            out.push({
                player,
                cx: cellCx + offX,
                cy: cellCy + offY,
                bobDelay: (player.id % 6) * 0.11
            });
        }
        return out;
    };

    const mountPlayerTokenContent = (el, player, bobDelay) => {
        let inner;
        if (window.HeroSystem && player.heroName) {
            inner = `<div class="token-pawn__svg token-pawn__svg--hero">${window.HeroSystem.getSVG(player.heroName, `tok-${player.id}`)}</div>`;
        } else {
            const src = portraitForPlayer(player);
            if (src) {
                inner = `<img class="token-pawn__img" src="${escapeAttr(src)}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'"><span class="token-pawn__fallback" style="display:none">${escapeHtml(
                    player.avatar
                )}</span>`;
            } else {
                inner = `<span class="token-pawn__fallback">${escapeHtml(player.avatar)}</span>`;
            }
        }
        el.innerHTML = `<div class="token-pawn__body">${inner}</div><div class="token-pawn__shadow" aria-hidden="true"></div>`;
        el.style.setProperty("--bob-delay", `${bobDelay}s`);
        el.style.setProperty("--token-color", player.color);
    };

    const renderTokens = (opts = {}) => {
        if (!tokensElement) return;
        const stepTransitionMs = opts.stepTransitionMs;
        const movingPlayerId = opts.movingPlayerId;

        const layouts = buildTokenLayouts();
        const wanted = new Set(layouts.map((l) => String(l.player.id)));

        tokensElement.querySelectorAll(".player-token").forEach((el) => {
            const id = el.getAttribute("data-player-order");
            if (!wanted.has(id)) el.remove();
        });

        for (const { player, cx, cy, bobDelay } of layouts) {
            let el = tokensElement.querySelector(`[data-player-order="${player.id}"]`);
            if (!el) {
                el = document.createElement("div");
                el.setAttribute("data-player-order", String(player.id));
                el.className = "player-token player-token--pawn";
                mountPlayerTokenContent(el, player, bobDelay);
                tokensElement.appendChild(el);
            }
            if (stepTransitionMs != null && movingPlayerId === player.id) {
                el.style.transition = `left ${stepTransitionMs}ms linear, top ${stepTransitionMs}ms linear`;
            } else {
                el.style.removeProperty("transition");
            }
            el.style.left = `${cx}px`;
            el.style.top = `${cy}px`;
            el.style.setProperty("--token-color", player.color);
            el.style.setProperty("--bob-delay", `${bobDelay}s`);
        }

        if (!tokenStepAnimRunning) {
            restartTokenHeroIdle();
        }
    };

    const randomDieFace = () => Math.floor(Math.random() * 6) + 1;

    const stopDiceRolling = () => {
        if (diceRollingTimer) {
            clearInterval(diceRollingTimer);
            diceRollingTimer = null;
        }
        die1?.classList.remove("rolling", "just-landed");
        die2?.classList.remove("rolling", "just-landed");
        diceArea?.classList.remove("is-rolling");
    };

    const startDiceRolling = () => {
        stopDiceRolling();
        diceArea?.classList.add("is-rolling");
        die1?.classList.add("rolling");
        die2?.classList.add("rolling");
        diceRollingTimer = window.setInterval(() => {
            setDieVisual(die1, randomDieFace());
            setDieVisual(die2, randomDieFace());
        }, 68);
    };

    const playDiceLandEffect = () => {
        if (!die1 || !die2) return;
        die1.classList.remove("just-landed");
        die2.classList.remove("just-landed");
        void die1.offsetWidth;
        void die2.offsetWidth;
        die1.classList.add("just-landed");
        die2.classList.add("just-landed");
        diceTotal?.classList.remove("pop");
        void diceTotal?.offsetWidth;
        diceTotal?.classList.add("pop");
        window.setTimeout(() => {
            die1?.classList.remove("just-landed");
            die2?.classList.remove("just-landed");
            diceTotal?.classList.remove("pop");
        }, 620);
    };

    const showYourTurnNotice = () => {
        if (!yourTurnToast || !turnTopBanner) return;
        if (yourTurnHideTimer) clearTimeout(yourTurnHideTimer);
        if (yourTurnHideTimer2) clearTimeout(yourTurnHideTimer2);
        yourTurnHideTimer = null;
        yourTurnHideTimer2 = null;
        yourTurnToast.hidden = false;
        yourTurnToast.setAttribute("aria-hidden", "false");
        requestAnimationFrame(() => {
            yourTurnToast.classList.add("is-visible");
        });
        turnTopBanner.classList.add("your-turn-spotlight");
        yourTurnHideTimer = window.setTimeout(() => {
            yourTurnToast.classList.remove("is-visible");
            turnTopBanner.classList.remove("your-turn-spotlight");
            yourTurnHideTimer = null;
            yourTurnHideTimer2 = window.setTimeout(() => {
                yourTurnToast.hidden = true;
                yourTurnToast.setAttribute("aria-hidden", "true");
                yourTurnHideTimer2 = null;
            }, 420);
        }, 2600);
    };

    const animateDiceOffline = () => {
        const L = cells.length;
        if (!L) return;
        if (rollDiceButton) rollDiceButton.disabled = true;
        startDiceRolling();
        window.setTimeout(() => {
            stopDiceRolling();
            const value1 = 3;
            const value2 = 5;
            setDieVisual(die1, value1);
            setDieVisual(die2, value2);
            diceTotal.textContent = `Tổng: ${value1 + value2}`;
            playDiceLandEffect();
            const activePlayer = players[activePlayerIndex];
            const from = activePlayer.position;
            const steps = value1 + value2;
            const to = (from + steps) % L;

            tokenStepAnimGen += 1;
            tokenStepAnimRunning = false;
            document.querySelectorAll(".player-token.is-step-moving").forEach((el) =>
                el.classList.remove("is-step-moving")
            );

            activePlayer.position = from;
            renderTokens();

            void animateTokenSteps([{ playerId: activePlayer.id, from, to, steps }]).then(() => {
                activePlayerIndex = (activePlayerIndex + 1) % players.length;
                updateTurnBanner(null);
                refreshCellInfoPanel(null);
                if (rollDiceButton) rollDiceButton.disabled = false;
            });
        }, 1050);
    };

    const updateDebtCrisisUi = (state) => {
        if (!debtCrisisModal) return;
        if (!state || !state.players) {
            debtCrisisModal.hidden = true;
            debtCrisisModal.setAttribute("aria-hidden", "true");
            return;
        }
        const playing = String(state.status || "").toUpperCase() === "PLAYING";
        const myTurn = state.myTurn === true;
        const ts = state.turnState || "";
        const ds = state.debtSituation;
        const owed = ds != null ? Number(ds.amountOwed) : NaN;
        const debtOk =
            ds != null &&
            Number.isFinite(owed) &&
            owed > 0 &&
            (ds.creditorName != null || ds.creditorTurnOrder != null);
        const show = isLiveGame && playing && myTurn && ts === "INSOLVENT" && debtOk;
        if (!show) {
            debtCrisisModal.hidden = true;
            debtCrisisModal.setAttribute("aria-hidden", "true");
            if (debtAssetList) debtAssetList.innerHTML = "";
            return;
        }
        debtCrisisModal.hidden = false;
        debtCrisisModal.setAttribute("aria-hidden", "false");
        if (debtCrisisSummary) {
            debtCrisisSummary.textContent = `Bạn nợ ${formatMoney(ds.amountOwed)} khi vào ô «${
                ds.causeCellName || "Ô"
            }». Chủ nợ: ${ds.creditorName || "—"}.`;
        }
        if (debtAssetList) {
            debtAssetList.innerHTML = "";
            const assetArr = ds.assets || [];
            if (!assetArr.length) {
                const li = document.createElement("li");
                li.className = "debt-asset-item debt-asset-item--empty";
                li.textContent =
                    "Không còn tài sản để bán / thế chấp — chỉ có thể phá sản hoặc đợi hết giờ lượt.";
                debtAssetList.appendChild(li);
            }
            assetArr.forEach((a) => {
                const li = document.createElement("li");
                li.className = "debt-asset-item";
                const actionLabel = a.suggestedAction === "SELL_HOUSE" ? "Bán 1 nhà" : "Thế chấp ô";
                const meta = document.createElement("div");
                meta.className = "debt-asset-meta";
                meta.innerHTML = `<div class="debt-asset-name">${escapeHtml(a.name)}</div>
                    <div class="debt-asset-detail">${actionLabel} · +${formatMoney(a.cashIfSold)}</div>`;
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "debt-asset-sell";
                btn.textContent = actionLabel;
                if (a.cellId != null) btn.dataset.cellId = String(a.cellId);
                btn.disabled = debtActionInFlight;
                li.appendChild(meta);
                li.appendChild(btn);
                debtAssetList.appendChild(li);
            });
        }
        if (debtBankruptButton) debtBankruptButton.disabled = debtActionInFlight;
    };

    async function applyStateInternal(state, opts = {}) {
        stopTurnTimer();
        lastLiveState = state;
        const playing = String(state.status || "").toUpperCase() === "PLAYING";
        const myTurn = state.myTurn === true;
        const ts = state.turnState || "";

        const oldBalances = {};
        const oldPositions = {};
        players.forEach((p) => {
            oldBalances[p.id] = p.money;
            oldPositions[p.id] = p.position;
        });

        const nextPlayers = [];
        state.players.forEach((p) => {
            const label =
                p.username ||
                (p.isBot ? `Bot ${p.turnOrder}` : `Player ${p.turnOrder}`);
            const orderIdx = Math.max(0, (p.turnOrder || 1) - 1);
            nextPlayers.push({
                id: p.turnOrder,
                username: label,
                money: p.balance || 0,
                color: PLAYER_PALETTE[orderIdx % PLAYER_PALETTE.length],
                avatar: p.isBot ? "BOT" : `P${p.turnOrder}`,
                position: p.position || 0,
                avatarUrl: p.avatarUrl || null,
                heroImageUrl: p.heroImageUrl || null,
                heroName: p.heroName || null,
                isBot: Boolean(p.isBot),
                isBankrupt: Boolean(p.isBankrupt),
                inJail: Boolean(p.inJail),
                jailFailedRolls: p.jailFailedRolls ?? 0,
                skills: Array.isArray(p.skills)
                    ? p.skills.map((sk) => ({
                          ...sk,
                          requiresTarget: sk.requiresTarget === true,
                          requiresDiceChoice: sk.requiresDiceChoice === true,
                          effectType: sk.effectType ?? null
                      }))
                    : []
            });
        });

        const landingByPlayerId = {};
        nextPlayers.forEach((np) => {
            landingByPlayerId[np.id] = np.position;
        });

        const L = cells.length;
        const animPlans = [];
        if (isLiveGame && L) {
            const d1 = state.lastDice1;
            const d2 = state.lastDice2;
            const diceSum =
                d1 != null && d2 != null && d1 >= 1 && d1 <= 6 && d2 >= 1 && d2 <= 6 ? d1 + d2 : null;

            nextPlayers.forEach((np) => {
                const oldPos = oldPositions[np.id];
                if (oldPos == null) return;
                const to = np.position;
                let steps = (to - oldPos + L) % L;
                if (steps === 0) return;
                /* Nếu khớp tổng xúc xắc → chắc chắn là đi bộ theo đường vòng (tránh nhảy ô kiểu đi tù / thẻ) */
                if (diceSum != null && (oldPos + diceSum) % L === to) {
                    steps = diceSum;
                } else if (steps > 12) {
                    return;
                }
                if (steps >= 1 && steps < L) {
                    animPlans.push({ playerId: np.id, from: oldPos, to, steps });
                }
            });
        }

        players.length = 0;
        nextPlayers.forEach((np) => players.push({ ...np }));

        if (animPlans.length && isLiveGame) {
            animPlans.forEach((plan) => {
                const p = players.find((x) => x.id === plan.playerId);
                if (p) p.position = plan.from;
            });
        }

        activePlayerIndex = Math.max((state.currentPlayerOrder || 1) - 1, 0);
        if (state.lastDice1 != null && state.lastDice2 != null) {
            setDieVisual(die1, state.lastDice1);
            setDieVisual(die2, state.lastDice2);
            const isDouble = state.lastDice1 === state.lastDice2;
            diceTotal.textContent = `Tổng: ${state.lastDice1 + state.lastDice2}${
                isDouble && playing ? " · Đôi!" : ""
            }`;
        }

        const oppPending = state.opponentLandPending;
        const hasOppChoice = Boolean(oppPending && oppPending.rentAmount != null);

        if (isLiveGame) {
            if (rollDiceButton) {
                rollDiceButton.disabled = !myTurn || ts !== "WAIT_ROLL" || !playing;
            }
            if (endTurnButton) {
                endTurnButton.disabled =
                    hasOppChoice || !myTurn || ts !== "ACTION_REQUIRED" || !playing;
            }
            if (buyPropertyButton) {
                buyPropertyButton.disabled =
                    hasOppChoice ||
                    !myTurn ||
                    ts !== "ACTION_REQUIRED" ||
                    !playing ||
                    !state.currentCell?.canBuy;
            }
            if (upgradePropertyButton) {
                upgradePropertyButton.disabled =
                    hasOppChoice ||
                    !myTurn ||
                    ts !== "ACTION_REQUIRED" ||
                    !playing ||
                    !state.currentCell?.canUpgrade;
            }
            if (payOppRentButton) {
                const show = hasOppChoice && myTurn && playing && ts === "ACTION_REQUIRED";
                payOppRentButton.hidden = !show;
                payOppRentButton.disabled = !show;
                if (show && oppPending.rentAmount != null) {
                    payOppRentButton.textContent = `Trả thuê (${formatMoney(oppPending.rentAmount)})`;
                }
            }
            if (buybackOppButton) {
                const show = hasOppChoice && myTurn && playing && ts === "ACTION_REQUIRED";
                buybackOppButton.hidden = !show;
                buybackOppButton.disabled = !show;
                if (show && oppPending.buybackPrice != null) {
                    const pct = oppPending.buybackPercent != null ? oppPending.buybackPercent : 130;
                    buybackOppButton.textContent = `Mua lại (${pct}% · ${formatMoney(oppPending.buybackPrice)})`;
                }
            }

            const showTimer =
                myTurn &&
                playing &&
                typeof state.turnSecondsRemaining === "number" &&
                (ts === "WAIT_ROLL" || ts === "ACTION_REQUIRED" || ts === "INSOLVENT");
            updateTurnTimerDisplay(state.turnSecondsRemaining, showTimer);
            if (showTimer) {
                let localSec = state.turnSecondsRemaining;
                turnTimerInterval = window.setInterval(() => {
                    localSec -= 1;
                    if (localSec <= 0) {
                        stopTurnTimer();
                        updateTurnTimerDisplay(0, true);
                        loadLiveState();
                        return;
                    }
                    updateTurnTimerDisplay(localSec, true);
                }, 1000);
            }

            if (surrenderButton) {
                const myOrder = state.myPlayerTurnOrder;
                const myP = myOrder != null ? players.find((x) => x.id === myOrder) : null;
                const can =
                    isLiveGame &&
                    Boolean(accountId) &&
                    playing &&
                    myP &&
                    !myP.isBankrupt;
                surrenderButton.hidden = !can;
                surrenderButton.disabled = !can;
            }
        } else {
            if (rollDiceButton) rollDiceButton.disabled = false;
            if (endTurnButton) endTurnButton.disabled = false;
            if (buyPropertyButton) buyPropertyButton.disabled = false;
            if (upgradePropertyButton) upgradePropertyButton.disabled = false;
            if (payOppRentButton) payOppRentButton.hidden = true;
            if (buybackOppButton) buybackOppButton.hidden = true;
            if (surrenderButton) surrenderButton.hidden = true;
            updateTurnTimerDisplay(null, false);
        }

        if (isLiveGame && playing) {
            const canRollNow = myTurn && ts === "WAIT_ROLL";
            if (canRollNow && !prevMyTurnCouldRoll) {
                showYourTurnNotice();
            }
            prevMyTurnCouldRoll = canRollNow;
        } else {
            prevMyTurnCouldRoll = false;
        }

        buildPlayerCards();
        applyBoardOwnership(state);
        updateActiveTurnHighlight(state);
        renderTokens();
        if (opts.diceLand) {
            playDiceLandEffect();
        }
        if (animPlans.length && isLiveGame) {
            await animateTokenSteps(animPlans);
        }
        renderTokens();
        runBoardCoinAnimations(oldBalances, players, landingByPlayerId);
        updateTurnBanner(state);
        refreshCellInfoPanel(state);
        updateDebtCrisisUi(state);
        appendGameLogsToChat(state);
        updateGameEndRanking(state);
        if (String(state?.status || "").toUpperCase() !== "PLAYING" && pendingSkillActivation) {
            pendingSkillActivation = null;
            setSkillTargetPickMode(false);
        }

        if (isLiveGame) {
            if (livePollIntervalId != null) {
                window.clearInterval(livePollIntervalId);
                livePollIntervalId = null;
            }
            const playing = String(state.status || "").toUpperCase() === "PLAYING";
            const im = state.myTurn === true;
            const pollMs = playing && !im ? 450 : 2800;
            livePollIntervalId = window.setInterval(loadLiveState, pollMs);
        } else if (livePollIntervalId != null) {
            window.clearInterval(livePollIntervalId);
            livePollIntervalId = null;
        }
    }

    const syncFromState = (state, opts = {}) => {
        if (!state || !state.players) return Promise.resolve();
        stateApplyChain = stateApplyChain
            .then(() => waitUntilAnimationsIdle())
            .then(() => applyStateInternal(state, opts))
            .catch(() => {});
        return stateApplyChain;
    };

    const callAction = async (actionPath) => {
        const response = await fetch(`/api/gameplay/${gameId}/${actionPath}`, {
            method: "POST",
            headers: authHeaders(true)
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Thao tác thất bại");
        }
        return response.json();
    };

    const callDebtSell = async (cellId) => {
        const response = await fetch(`/api/gameplay/${gameId}/debt/sell`, {
            method: "POST",
            headers: authHeaders(true),
            body: JSON.stringify({ cellId: Number(cellId) })
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Không bán được tài sản");
        }
        return response.json();
    };

    const callDebtBankrupt = async () => {
        const response = await fetch(`/api/gameplay/${gameId}/debt/bankrupt`, {
            method: "POST",
            headers: authHeaders(true),
            body: "{}"
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Không thực hiện được phá sản");
        }
        return response.json();
    };

    const callSurrender = async () => {
        const response = await fetch(`/api/gameplay/${gameId}/surrender`, {
            method: "POST",
            headers: authHeaders(true),
            body: "{}"
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Không đầu hàng được");
        }
        return response.json();
    };

    const openSurrenderModal = () => {
        if (!surrenderConfirmModal) return;
        const bot = urlParams.get("vsBot") === "1";
        if (surrenderConfirmTextEl) {
            surrenderConfirmTextEl.textContent = bot
                ? "Trong ván đấu máy: đầu hàng sẽ kết thúc ván ngay và xếp hạng theo số tiền hiện có (không thưởng xu)."
                : "Bạn sẽ bị loại khỏi ván. Ô đang sở hữu trả về thị trường (không còn chủ). Nếu đang nợ tiền thuê, tài sản và tiền mặt sẽ chuyển cho chủ nợ như khi phá sản.";
        }
        surrenderConfirmModal.hidden = false;
        surrenderConfirmModal.setAttribute("aria-hidden", "false");
    };

    const closeSurrenderModal = () => {
        if (!surrenderConfirmModal) return;
        surrenderConfirmModal.hidden = true;
        surrenderConfirmModal.setAttribute("aria-hidden", "true");
    };

    const openDuelDiceModal = (skillId) => {
        pendingDuelSkillId = skillId != null ? String(skillId) : null;
        if (!duelDiceModal) return;
        duelDiceModal.hidden = false;
        duelDiceModal.setAttribute("aria-hidden", "false");
    };

    const closeDuelDiceModal = () => {
        pendingDuelSkillId = null;
        if (!duelDiceModal) return;
        duelDiceModal.hidden = true;
        duelDiceModal.setAttribute("aria-hidden", "true");
    };

    const callSkillActivate = async (skillId, targetCellId, dice1, dice2) => {
        const body = { skillId: Number(skillId) };
        if (targetCellId != null && Number.isFinite(Number(targetCellId))) {
            body.targetCellId = Number(targetCellId);
        }
        if (dice1 != null && dice2 != null && Number.isFinite(Number(dice1)) && Number.isFinite(Number(dice2))) {
            body.dice1 = Number(dice1);
            body.dice2 = Number(dice2);
        }
        const response = await fetch(`/api/gameplay/${gameId}/skill/activate`, {
            method: "POST",
            headers: authHeaders(true),
            body: JSON.stringify(body)
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Không kích hoạt được kỹ năng");
        }
        return response.json();
    };

    const callOpponentLandResolve = async (buyback) => {
        const response = await fetch(`/api/gameplay/${gameId}/opponent-land`, {
            method: "POST",
            headers: authHeaders(true),
            body: JSON.stringify({ buyback })
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Thao tác thất bại");
        }
        return response.json();
    };

    const updateGameEndRanking = (state) => {
        if (!gameEndRankingOverlay || !gameEndRankingList) return;
        const fin = String(state?.status || "").toUpperCase() === "FINISHED";
        const rows = state?.finalRanking;
        if (!fin || !Array.isArray(rows) || rows.length === 0) {
            gameEndRankingOverlay.hidden = true;
            gameEndRankingOverlay.setAttribute("aria-hidden", "true");
            return;
        }
        gameEndRankingOverlay.hidden = false;
        gameEndRankingOverlay.setAttribute("aria-hidden", "false");
        gameEndRankingList.innerHTML = rows
            .map((r) => {
                const botLabel = r.isBot ? " · Bot" : "";
                const medal =
                    r.rank === 1 ? "🥇" : r.rank === 2 ? "🥈" : r.rank === 3 ? "🥉" : "🏅";
                const tier =
                    r.rank === 1 ? "rank-t1" : r.rank === 2 ? "rank-t2" : r.rank === 3 ? "rank-t3" : "rank-t4";
                const heroLine =
                    r.heroName && String(r.heroName).trim()
                        ? `<span class="game-end-ranking-hero">${escapeHtml(r.heroName)}</span>`
                        : "";
                const reward = r.coinReward;
                const moneyLine = r.isBot
                    ? `<span class="game-end-ranking-money">Không nhận xu</span>`
                    : reward != null && reward !== undefined
                      ? `<span class="game-end-ranking-money">+${formatMoney(reward)} xu <span class="game-end-ranking-money-note">(vào tài khoản)</span></span>`
                      : `<span class="game-end-ranking-money">Số dư trong ván: ${formatMoney(r.balance)}</span>`;
                return `<div class="game-end-ranking-row ${tier}" role="listitem">
            <div class="game-end-ranking-medal" aria-label="Hạng ${r.rank}">${medal}</div>
            <div class="game-end-ranking-body">
              <span class="game-end-ranking-name">${escapeHtml(r.displayName)}${botLabel}</span>
              ${heroLine}
              ${moneyLine}
            </div>
          </div>`;
            })
            .join("");
    };

    document.addEventListener("click", async (e) => {
        const btn = e.target.closest(".player-skill-chip--active");
        if (!btn || btn.disabled || !isLiveGame || !gameId) return;
        const sid = btn.dataset.skillId;
        if (!sid) return;
        e.preventDefault();
        const needsTarget = btn.dataset.requiresTarget === "1";
        const needsDice = btn.dataset.requiresDice === "1";
        if (needsTarget) {
            pendingSkillActivation = { skillId: sid };
            setSkillTargetPickMode(true);
            setActionStatus("Chọn một ô trên bàn làm mục tiêu (ưu tiên bàn tải từ API).");
            return;
        }
        if (needsDice) {
            openDuelDiceModal(sid);
            setActionStatus("Chọn hai mặt xúc xắc rồi bấm Tung.");
            return;
        }
        try {
            const data = await callSkillActivate(sid);
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã kích hoạt kỹ năng");
        } catch (error) {
            setActionStatus(error.message || "Kỹ năng thất bại", true);
        }
    });

    boardElement?.addEventListener("click", async (e) => {
        if (!pendingSkillActivation || !isLiveGame || !gameId) return;
        const cell = e.target.closest(".board-cell");
        if (!cell) return;
        e.preventDefault();
        e.stopPropagation();
        const idx = parseInt(cell.getAttribute("data-cell-index"), 10);
        const fromData = cell.getAttribute("data-cell-id");
        let cid =
            fromData != null && fromData !== ""
                ? Number(fromData)
                : Number.isFinite(idx) && cells[idx]?.cellId != null
                  ? cells[idx].cellId
                  : null;
        if (cid == null || !Number.isFinite(cid)) {
            setActionStatus("Không xác định được ô (cần bàn từ API có cellId).", true);
            return;
        }
        const sid = pendingSkillActivation.skillId;
        pendingSkillActivation = null;
        setSkillTargetPickMode(false);
        try {
            const data = await callSkillActivate(sid, cid);
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã kích hoạt kỹ năng");
        } catch (error) {
            setActionStatus(error.message || "Kỹ năng thất bại", true);
        }
    });

    gameEndRankingPlayAgain?.addEventListener("click", () => {
        if (roomIdParam != null && String(roomIdParam).trim() !== "") {
            window.location.href = `/private-table?roomId=${encodeURIComponent(String(roomIdParam).trim())}`;
            return;
        }
        if (isBotMode) {
            window.location.href = "/play-vs-ai";
            return;
        }
        window.location.href = "/home";
    });

    gameEndRankingExitHome?.addEventListener("click", () => {
        window.location.href = "/home";
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && pendingSkillActivation) {
            pendingSkillActivation = null;
            setSkillTargetPickMode(false);
            setActionStatus("Đã hủy chọn mục tiêu kỹ năng.");
        }
        if (e.key === "Escape" && pendingDuelSkillId && duelDiceModal && !duelDiceModal.hidden) {
            closeDuelDiceModal();
            setActionStatus("Đã hủy Thần xúc xắc.");
        }
    });

    duelDiceCancelButton?.addEventListener("click", () => {
        closeDuelDiceModal();
        setActionStatus("Đã hủy Thần xúc xắc.");
    });
    duelDiceModal?.addEventListener("click", (e) => {
        if (e.target === duelDiceModal) closeDuelDiceModal();
    });
    duelDiceOkButton?.addEventListener("click", async () => {
        const sid = pendingDuelSkillId;
        if (!sid || !isLiveGame || !gameId) return;
        const d1 = Number(duelDiceSelect1?.value);
        const d2 = Number(duelDiceSelect2?.value);
        if (!Number.isFinite(d1) || !Number.isFinite(d2)) return;
        closeDuelDiceModal();
        try {
            const data = await callSkillActivate(sid, null, d1, d2);
            await syncFromState(data.state, { diceLand: true });
            setActionStatus(data.message || "Đã kích hoạt kỹ năng");
        } catch (error) {
            setActionStatus(error.message || "Kỹ năng thất bại", true);
        }
    });

    const loadLiveState = async () => {
        const response = await fetch(`/api/gameplay/${gameId}/state`, {
            headers: authHeaders()
        });
        if (!response.ok) return;
        const state = await response.json();
        syncFromState(state);
    };

    rollDiceButton.addEventListener("click", async () => {
        if (!isLiveGame) {
            animateDiceOffline();
            return;
        }
        if (rollDiceButton.disabled) return;
        const startedAt = Date.now();
        const minRollMs = 720;
        startDiceRolling();
        rollDiceButton.disabled = true;
        try {
            const data = await callAction("roll");
            const wait = Math.max(0, minRollMs - (Date.now() - startedAt));
            await new Promise((r) => setTimeout(r, wait));
            stopDiceRolling();
            await syncFromState(data.state, { diceLand: true });
            setActionStatus(data.message || "Đã tung xúc xắc");
        } catch (error) {
            stopDiceRolling();
            if (lastLiveState) {
                await syncFromState(lastLiveState);
            }
            setActionStatus(error.message || "Tung xúc xắc thất bại", true);
        }
    });
    endTurnButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callAction("end-turn");
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã kết thúc lượt");
        } catch (error) {
            setActionStatus(error.message || "Kết thúc lượt thất bại", true);
        }
    });
    buyPropertyButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callAction("buy");
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã mua đất");
        } catch (error) {
            setActionStatus(error.message || "Mua đất thất bại", true);
        }
    });
    upgradePropertyButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callAction("upgrade");
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã nâng cấp");
        } catch (error) {
            setActionStatus(error.message || "Nâng cấp thất bại", true);
        }
    });

    payOppRentButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callOpponentLandResolve(false);
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã trả thuê");
        } catch (error) {
            setActionStatus(error.message || "Thao tác thất bại", true);
        }
    });

    buybackOppButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callOpponentLandResolve(true);
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã mua lại");
        } catch (error) {
            setActionStatus(error.message || "Mua lại thất bại", true);
        }
    });

    surrenderButton?.addEventListener("click", () => {
        if (!isLiveGame || !accountId || surrenderActionInFlight) return;
        openSurrenderModal();
    });
    surrenderCancelButton?.addEventListener("click", () => closeSurrenderModal());
    surrenderConfirmModal?.addEventListener("click", (e) => {
        if (e.target === surrenderConfirmModal) closeSurrenderModal();
    });
    surrenderConfirmButton?.addEventListener("click", async () => {
        if (!isLiveGame || surrenderActionInFlight) return;
        surrenderActionInFlight = true;
        if (surrenderConfirmButton) surrenderConfirmButton.disabled = true;
        if (surrenderCancelButton) surrenderCancelButton.disabled = true;
        try {
            const data = await callSurrender();
            closeSurrenderModal();
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã đầu hàng");
        } catch (error) {
            setActionStatus(error.message || "Đầu hàng thất bại", true);
            if (lastLiveState) await syncFromState(lastLiveState);
        } finally {
            surrenderActionInFlight = false;
            if (surrenderConfirmButton) surrenderConfirmButton.disabled = false;
            if (surrenderCancelButton) surrenderCancelButton.disabled = false;
        }
    });

    debtAssetList?.addEventListener("click", async (e) => {
        const btn = e.target.closest(".debt-asset-sell");
        if (!btn || btn.disabled || debtActionInFlight) return;
        const cid = btn.dataset.cellId;
        if (cid == null || cid === "") return;
        debtActionInFlight = true;
        btn.disabled = true;
        if (debtBankruptButton) debtBankruptButton.disabled = true;
        try {
            const data = await callDebtSell(cid);
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã xử lý tài sản");
        } catch (error) {
            setActionStatus(error.message || "Thao tác thất bại", true);
            if (lastLiveState) await syncFromState(lastLiveState);
        } finally {
            debtActionInFlight = false;
        }
    });

    debtBankruptButton?.addEventListener("click", async () => {
        if (!isLiveGame || debtActionInFlight) return;
        debtActionInFlight = true;
        debtAssetList?.querySelectorAll(".debt-asset-sell").forEach((b) => {
            b.disabled = true;
        });
        debtBankruptButton.disabled = true;
        try {
            const data = await callDebtBankrupt();
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã phá sản");
        } catch (error) {
            setActionStatus(error.message || "Thao tác thất bại", true);
            if (lastLiveState) await syncFromState(lastLiveState);
        } finally {
            debtActionInFlight = false;
        }
    });

    chatForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const message = chatInput.value.trim();
        if (!message) {
            return;
        }

        chatMessages.push({
            user: players[activePlayerIndex].username,
            message
        });
        chatInput.value = "";
        renderChat();
    });

    try {
        if (window.HeroSystem?.preloadCharacterSvgs) {
            await window.HeroSystem.preloadCharacterSvgs();
        }
    } catch (e) {
        console.warn("Hero SVG preload failed", e);
    }

    await loadBoardLayout();
    buildBoard();
    refreshCellInfoPanel(null);
    setDieVisual(die1, 3);
    setDieVisual(die2, 5);
    if (diceTotal) diceTotal.textContent = "Tổng: 8";
    buildPlayerCards();
    if (!isLiveGame && players.length) {
        updateActiveTurnHighlight({
            status: "PLAYING",
            currentPlayerOrder: players[Math.min(activePlayerIndex, players.length - 1)].id
        });
    }
    renderChat();
    window.setTimeout(renderTokens, 50);
    window.addEventListener("resize", renderTokens);
    updateTurnBanner(null);
    if (isLiveGame) {
        void loadLiveState();
    }
});
