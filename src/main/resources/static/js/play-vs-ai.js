document.addEventListener("DOMContentLoaded", async () => {
    const accountId = localStorage.getItem("accountId");

    const refreshAiLobbyCurrencyIcons = () => {
        if (window.CoinSystem && typeof CoinSystem.initCurrencySlots === "function") {
            CoinSystem.initCurrencySlots([
                { elId: "aiMenuCoinSilver", type: "silver" },
                { elId: "aiMenuCoinGold", type: "gold" }
            ]);
        }
    };

    const playerAvatar = document.getElementById("aiPlayerAvatar");
    const playerName = document.getElementById("aiPlayerName");
    const playerCoins = document.getElementById("aiPlayerCoins");
    const playerTickets = document.getElementById("aiPlayerTickets");
    const selectedHeroImage = document.getElementById("aiSelectedHeroImage");
    const selectedHeroSvgHost = document.getElementById("aiSelectedHeroSvgHost");
    const selectedHeroName = document.getElementById("aiSelectedHeroName");
    const activeSlotHint = document.getElementById("aiActiveSlotHint");
    const heroSkillPanel = document.getElementById("aiHeroSkillPanel");
    const selectedHeroSkillName = document.getElementById("aiSelectedHeroSkillName");
    const selectedHeroSkillDesc = document.getElementById("aiSelectedHeroSkillDesc");
    const selectedHeroSkillCd = document.getElementById("aiSelectedHeroSkillCd");
    const heroEmptyState = document.getElementById("aiHeroEmptyState");
    const heroList = document.getElementById("aiHeroList");
    const playerSlots = document.getElementById("aiPlayerSlots");
    const startButton = document.getElementById("aiStartButton");
    const playerCountEl = document.getElementById("aiPlayerCount");
    const tableStatus = document.getElementById("aiTableStatus");

    let userSummary = null;
    let heroes = [];
    /** 'human' | 2 | 3 | 4 */
    let activeSlot = "human";
    let humanHeroId = null;
    /** { slotIndex, difficulty: 'easy'|'hard', heroId } */
    let bots = [];
    let openAddMenuSlot = null;
    let previewIdleUid = "ai-preview";

    const escapeHtml = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

    const escapeAttr = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/"/g, "&quot;")
            .replace(/</g, "&lt;");

    const setStatus = (message, isError = false) => {
        if (!tableStatus) return;
        tableStatus.textContent = message;
        tableStatus.style.color = isError ? "#ffd7d7" : "#fff8eb";
    };

    const fetchActiveGame = async () => {
        try {
            const response = await fetch("/api/user/me/active-game", { headers: getHeaders() });
            if (!response.ok) {
                return null;
            }
            return await response.json();
        } catch {
            return null;
        }
    };

    const resolveActiveGameGate = async () => {
        const ag = await fetchActiveGame();
        if (!ag?.hasActiveGame) {
            return "proceed";
        }
        const solo = ag.soloVsAi === true;
        const msg = solo
            ? "Bạn đang còn trong ván đấu máy. Có muốn quay lại không?"
            : "Bạn đang còn trong ván multiplayer. Có muốn quay lại không?";
        if (window.confirm(msg)) {
            const q = solo
                ? `gameId=${ag.gameId}&vsBot=1`
                : `gameId=${ag.gameId}&roomId=${ag.roomId != null ? ag.roomId : ""}`;
            window.location.href = `/game-board?${q}`;
            return "redirected";
        }
        return "blocked";
    };

    const getHeaders = (includeJson = false) => {
        const headers = {};
        if (accountId) headers["X-Account-Id"] = accountId;
        if (includeJson) headers["Content-Type"] = "application/json";
        return headers;
    };

    const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const getHeroById = (heroId) => {
        if (heroId == null || heroId === "") return null;
        const n = Number(heroId);
        return heroes.find((h) => Number(h.heroId) === n) || null;
    };

    const defaultHeroId = () => {
        const unlocked = heroes.find((h) => h.defaultUnlocked);
        return unlocked?.heroId ?? heroes[0]?.heroId ?? null;
    };

    const getTargetHeroId = () => {
        if (activeSlot === "human") return humanHeroId;
        const b = bots.find((x) => x.slotIndex === activeSlot);
        return b ? b.heroId : null;
    };

    const applyAvatar = (element, username, avatarUrl) => {
        if (!element) return;
        if (avatarUrl) {
            element.textContent = "";
            element.style.backgroundImage = `url('${avatarUrl}')`;
            element.style.backgroundSize = "cover";
            element.style.backgroundPosition = "center";
            return;
        }
        element.style.backgroundImage = "";
        element.textContent = (username || "P").slice(0, 2).toUpperCase();
    };

    const renderPreview = () => {
        const hid = getTargetHeroId();
        const hero = hid != null ? getHeroById(hid) : null;

        if (selectedHeroName) {
            if (hero?.name) {
                selectedHeroName.textContent = hero.name;
            } else if (activeSlot === "human") {
                selectedHeroName.textContent = "Chưa chọn nhân vật";
            } else {
                selectedHeroName.textContent = "Chưa chọn nhân vật cho bot";
            }
        }

        if (activeSlotHint) {
            if (activeSlot === "human") {
                activeSlotHint.textContent = "Đang chỉnh: Bạn (ô 1)";
            } else {
                const b = bots.find((x) => x.slotIndex === activeSlot);
                const lab = b?.difficulty === "hard" ? "Bot khó" : "Bot dễ";
                activeSlotHint.textContent = `Đang chỉnh: ${lab} · ô ${activeSlot}`;
            }
        }

        if (heroSkillPanel && selectedHeroSkillName && selectedHeroSkillDesc && selectedHeroSkillCd) {
            const hasSkill = Boolean(hero?.skillName || hero?.skillDescription);
            if (hero && hasSkill) {
                heroSkillPanel.hidden = false;
                selectedHeroSkillName.textContent = hero.skillName || "—";
                selectedHeroSkillDesc.textContent = hero.skillDescription || "Chưa có mô tả chi tiết.";
                const cd = hero.skillCooldown;
                selectedHeroSkillCd.textContent =
                    cd != null && Number(cd) > 0
                        ? `Hồi chiêu: ${cd} lượt`
                        : "Hồi chiêu: theo luật nhân vật / thụ động";
            } else {
                heroSkillPanel.hidden = true;
                selectedHeroSkillName.textContent = "";
                selectedHeroSkillDesc.textContent = "";
                selectedHeroSkillCd.textContent = "";
            }
        }

        if (!selectedHeroImage || !heroEmptyState) return;

        if (window.HeroSystem) {
            window.HeroSystem.stopIdle(previewIdleUid);
        }

        if (selectedHeroSvgHost) {
            selectedHeroSvgHost.innerHTML = "";
            selectedHeroSvgHost.hidden = true;
        }

        if (hero?.name && window.HeroSystem && selectedHeroSvgHost) {
            selectedHeroSvgHost.innerHTML = window.HeroSystem.getSVG(hero.name, previewIdleUid);
            selectedHeroSvgHost.hidden = false;
            selectedHeroImage.style.display = "none";
            heroEmptyState.style.display = "none";
            selectedHeroImage.removeAttribute("src");
            window.HeroSystem.startIdle(previewIdleUid, 0);
            return;
        }

        selectedHeroImage.removeAttribute("src");
        selectedHeroImage.style.display = "none";
        heroEmptyState.style.display = "grid";
    };

    const renderHeroList = () => {
        if (!heroList || !heroes.length) {
            if (heroList) heroList.innerHTML = "";
            return;
        }

        const currentId = getTargetHeroId();
        const curNum = currentId != null ? Number(currentId) : null;
        const useSvg = typeof window.HeroSystem !== "undefined";

        heroList.innerHTML = heroes
            .map((hero) => {
                const uid = `ai-list-${hero.heroId}`;
                const svgBlock =
                    useSvg && hero.name
                        ? `<div class="hero-option-media hero-option-media--svg">${window.HeroSystem.getSVG(hero.name, uid)}</div>`
                        : `<div class="hero-option-media"><span>${escapeHtml(hero.name)}</span></div>`;
                return `
            <button
                type="button"
                class="hero-option ${curNum != null && curNum === Number(hero.heroId) ? "active" : ""}"
                data-action="ai-select-hero"
                data-hero-id="${hero.heroId}"
            >
                ${svgBlock}
                <div class="hero-option-name">${escapeHtml(hero.name)}</div>
            </button>
        `;
            })
            .join("");

        if (useSvg) {
            window.requestAnimationFrame(() => {
                heroes.forEach((hero, idx) => {
                    window.HeroSystem.startIdle(`ai-list-${hero.heroId}`, idx * 0.08);
                });
            });
        }
    };

    const renderHumanSlot = () => {
        const hero = humanHeroId != null ? getHeroById(humanHeroId) : null;
        const heroName = hero?.name || "Chưa chọn nhân vật";
        const avatarMarkup = userSummary?.avatarUrl
            ? `<img class="slot-avatar-image" src="${escapeAttr(userSummary.avatarUrl)}" alt="">`
            : `<span>${(userSummary?.username || "P").slice(0, 2).toUpperCase()}</span>`;
        const useSvg = window.HeroSystem && hero?.name;
        const focusClass = activeSlot === "human" ? " slot-focus" : "";

        return `
            <article class="slot-card occupied combined-card ai-slot${focusClass}" data-action="ai-focus-slot" data-slot="1">
                <div class="slot-card-header">
                    <div class="slot-host-avatar">${avatarMarkup}</div>
                    <div class="slot-meta">
                        <strong>${escapeHtml(userSummary?.username || "Bạn")}</strong>
                        <span>Bạn</span>
                    </div>
                </div>
                <div class="slot-hero-media ${useSvg ? "slot-hero-media--svg" : ""}">
                    ${
                        useSvg
                            ? `<div class="slot-hero-svg-wrap">${window.HeroSystem.getSVG(hero.name, "ai-slot-human")}</div>`
                            : `<div class="slot-hero-placeholder">${escapeHtml(heroName)}</div>`
                    }
                </div>
                <div class="slot-hero-name">${escapeHtml(heroName)}</div>
                <div class="slot-ready ready-host">Người chơi</div>
            </article>
        `;
    };

    const renderBotSlot = (bot) => {
        const hero = bot.heroId != null ? getHeroById(bot.heroId) : null;
        const heroName = hero?.name || "Chưa chọn nhân vật";
        const hard = bot.difficulty === "hard";
        const badgeClass = hard ? "slot-bot-badge slot-bot-badge--hard" : "slot-bot-badge";
        const badgeText = hard ? "Bot khó" : "Bot dễ";
        const useSvg = window.HeroSystem && hero?.name;
        const focusClass = activeSlot === bot.slotIndex ? " slot-focus" : "";

        return `
            <article class="slot-card occupied combined-card ai-slot${focusClass}" data-action="ai-focus-slot" data-slot="${bot.slotIndex}">
                <div class="${badgeClass}">${badgeText}</div>
                <div class="slot-hero-media ${useSvg ? "slot-hero-media--svg" : ""}">
                    ${
                        useSvg
                            ? `<div class="slot-hero-svg-wrap">${window.HeroSystem.getSVG(hero.name, `ai-slot-bot-${bot.slotIndex}`)}</div>`
                            : `<div class="slot-hero-placeholder">${escapeHtml(heroName)}</div>`
                    }
                </div>
                <div class="slot-hero-name">${escapeHtml(heroName)}</div>
                <button type="button" class="slot-remove-bot" data-action="ai-remove-bot" data-slot="${bot.slotIndex}">Gỡ bot</button>
            </article>
        `;
    };

    const renderEmptySlot = (slotIndex) => {
        const menuOpen = openAddMenuSlot === slotIndex;
        return `
            <article class="slot-card empty ai-slot" data-slot-wrap="${slotIndex}">
                <button type="button" class="add-slot-button" data-action="ai-toggle-add" data-slot="${slotIndex}">+</button>
                ${
                    menuOpen
                        ? `<div class="slot-add-popover" data-action="ai-popover-stop">
                    <button type="button" data-action="ai-add-bot" data-difficulty="easy" data-slot="${slotIndex}">Thêm Bot dễ</button>
                    <button type="button" data-action="ai-add-bot" data-difficulty="hard" data-slot="${slotIndex}">Thêm Bot khó</button>
                    <p class="slot-add-hint">Mời người chơi thật: dùng <strong>Phòng riêng</strong> trên menu chính.</p>
                </div>`
                        : ""
                }
            </article>
        `;
    };

    const renderSlots = () => {
        if (!playerSlots) return;
        const parts = [renderHumanSlot()];
        for (let s = 2; s <= 4; s += 1) {
            const bot = bots.find((b) => b.slotIndex === s);
            if (bot) {
                parts.push(renderBotSlot(bot));
            } else {
                parts.push(renderEmptySlot(s));
            }
        }
        playerSlots.innerHTML = parts.join("");

        if (window.HeroSystem) {
            window.requestAnimationFrame(() => {
                if (humanHeroId && getHeroById(humanHeroId)?.name) {
                    window.HeroSystem.startIdle("ai-slot-human", 0.02);
                }
                bots.forEach((b, idx) => {
                    const h = getHeroById(b.heroId);
                    if (h?.name) {
                        window.HeroSystem.startIdle(`ai-slot-bot-${b.slotIndex}`, 0.05 + idx * 0.06);
                    }
                });
            });
        }
    };

    const renderAll = () => {
        if (playerName && userSummary) {
            playerName.textContent = userSummary.username || "Người chơi";
        }
        if (playerCoins) playerCoins.textContent = formatNumber(userSummary?.coins);
        if (playerTickets) playerTickets.textContent = formatNumber(userSummary?.tickets ?? 0);
        refreshAiLobbyCurrencyIcons();
        if (playerCountEl) {
            playerCountEl.textContent = `${1 + bots.length}/4 người chơi`;
        }
        applyAvatar(playerAvatar, userSummary?.username, userSummary?.avatarUrl);
        renderPreview();
        renderHeroList();
        renderSlots();
    };

    const loadUserSummary = async () => {
        const response = await fetch("/api/user/me/summary", { headers: getHeaders() });
        if (!response.ok) return;
        userSummary = await response.json();
        if (userSummary?.equippedCharacterId != null) {
            humanHeroId = userSummary.equippedCharacterId;
        }
    };

    const loadHeroes = async () => {
        try {
            const response = await fetch("/api/heroes/owned", { headers: getHeaders() });
            if (!response.ok) {
                heroes = [];
                return;
            }
            heroes = await response.json();
            if (humanHeroId != null && !getHeroById(humanHeroId)) {
                humanHeroId = null;
            }
            if (humanHeroId == null) {
                const d = defaultHeroId();
                if (d != null) humanHeroId = d;
            }
            bots.forEach((b) => {
                if (b.heroId == null) b.heroId = defaultHeroId();
                if (b.heroId != null && !getHeroById(b.heroId)) {
                    b.heroId = defaultHeroId();
                }
            });
        } catch (e) {
            console.error(e);
            heroes = [];
        }
    };

    document.addEventListener("click", (e) => {
        const pop = e.target.closest(".slot-add-popover");
        const toggle = e.target.closest("[data-action='ai-toggle-add']");
        if (!pop && !toggle && openAddMenuSlot != null) {
            openAddMenuSlot = null;
            renderSlots();
        }
    });

    document.addEventListener("click", async (event) => {
        const t = event.target.closest("[data-action]");
        if (!t) return;
        const action = t.getAttribute("data-action");

        if (action === "ai-popover-stop") {
            event.stopPropagation();
        }

        try {
            if (action === "ai-home") {
                window.location.href = "/home";
            }
            if (action === "ai-shop") {
                window.location.href = "/shop";
            }
            if (action === "ai-menu") {
                window.location.href = "/home";
            }

            if (action === "ai-focus-slot") {
                const s = t.getAttribute("data-slot");
                if (s === "1") activeSlot = "human";
                else activeSlot = Number(s);
                renderAll();
            }

            if (action === "ai-toggle-add") {
                const s = Number(t.getAttribute("data-slot"));
                openAddMenuSlot = openAddMenuSlot === s ? null : s;
                renderSlots();
                return;
            }

            if (action === "ai-add-bot") {
                const s = Number(t.getAttribute("data-slot"));
                const diff = (t.getAttribute("data-difficulty") || "easy").toLowerCase();
                const dh = diff === "hard" ? "hard" : "easy";
                const hid = defaultHeroId();
                bots.push({ slotIndex: s, difficulty: dh, heroId: hid });
                bots.sort((a, b) => a.slotIndex - b.slotIndex);
                openAddMenuSlot = null;
                activeSlot = s;
                setStatus(`Đã thêm ${dh === "hard" ? "bot khó" : "bot dễ"} ở ô ${s}.`);
                renderAll();
                return;
            }

            if (action === "ai-remove-bot") {
                const s = Number(t.getAttribute("data-slot"));
                bots = bots.filter((b) => b.slotIndex !== s);
                if (activeSlot === s) activeSlot = "human";
                renderAll();
                return;
            }

            if (action === "ai-select-hero") {
                const hid = Number(t.getAttribute("data-hero-id"));
                if (Number.isNaN(hid)) return;
                if (activeSlot === "human") {
                    humanHeroId = hid;
                } else {
                    const b = bots.find((x) => x.slotIndex === activeSlot);
                    if (b) b.heroId = hid;
                }
                renderAll();
                return;
            }

            if (action === "ai-start") {
                if (!accountId) {
                    setStatus("Bạn chưa đăng nhập.", true);
                    window.setTimeout(() => {
                        window.location.href = "/login";
                    }, 800);
                    return;
                }
                if (bots.length < 1) {
                    setStatus("Thêm ít nhất một bot (nút + ở ô trống).", true);
                    return;
                }
                if (humanHeroId == null) {
                    setStatus("Chọn nhân vật cho bạn (ô 1).", true);
                    return;
                }
                for (const b of bots) {
                    if (b.heroId == null) {
                        setStatus(`Chọn nhân vật cho bot ở ô ${b.slotIndex}.`, true);
                        activeSlot = b.slotIndex;
                        renderAll();
                        return;
                    }
                }

                const gate = await resolveActiveGameGate();
                if (gate === "redirected") {
                    return;
                }
                if (gate === "blocked") {
                    setStatus("Bạn đang trong một ván. Hãy kết thúc hoặc quay lại ván đó trước.", true);
                    return;
                }

                startButton.disabled = true;
                setStatus("Đang tạo trận...");
                const body = {
                    heroId: humanHeroId,
                    botSlots: bots.map((b) => ({
                        difficulty: b.difficulty,
                        heroId: b.heroId
                    }))
                };
                const response = await fetch("/api/gameplay/bot/start", {
                    method: "POST",
                    headers: getHeaders(true),
                    body: JSON.stringify(body)
                });
                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || "Không thể tạo trận");
                }
                const data = await response.json();
                localStorage.setItem("aiDifficulty", data.difficulty || "mixed");
                localStorage.setItem("aiBotCount", String(data.botCount ?? bots.length));
                window.location.href =
                    data.redirectUrl ||
                    `/game-board?gameId=${data.gameId}&vsBot=1&difficulty=${data.difficulty || "mixed"}&bots=${data.botCount ?? bots.length}`;
            }
        } catch (err) {
            console.error(err);
            setStatus(err.message || "Thao tác thất bại.", true);
            if (startButton) startButton.disabled = false;
        }
    });

    if (!accountId) {
        setStatus("Chưa đăng nhập — chuyển hướng...", true);
        window.setTimeout(() => {
            window.location.href = "/login";
        }, 900);
        return;
    }

    try {
        if (window.HeroSystem?.preloadCharacterSvgs) {
            await window.HeroSystem.preloadCharacterSvgs();
        }
    } catch (e) {
        console.warn(e);
    }

    await loadUserSummary();
    await loadHeroes();
    renderAll();
});
