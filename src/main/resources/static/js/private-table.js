document.addEventListener("DOMContentLoaded", async () => {
    const accountId = localStorage.getItem("accountId");
    const roomId = new URLSearchParams(window.location.search).get("roomId");

    const refreshPrivateRoomCurrencyIcons = () => {
        if (window.CoinSystem && typeof CoinSystem.initCurrencySlots === "function") {
            CoinSystem.initCurrencySlots([
                { elId: "menuCoinSilver", type: "silver" },
                { elId: "menuCoinGold", type: "gold" }
            ]);
        }
    };

    const playerName = document.getElementById("playerName");
    const playerAvatar = document.getElementById("playerAvatar");
    const playerCoins = document.getElementById("playerCoins");
    const playerTickets = document.getElementById("playerTickets");
    const roomCode = document.getElementById("roomCode");
    const roomVisibilityIndicator = document.getElementById("roomVisibilityIndicator");
    const selectedHeroImage = document.getElementById("selectedHeroImage");
    const selectedHeroSvgHost = document.getElementById("selectedHeroSvgHost");
    const selectedHeroName = document.getElementById("selectedHeroName");
    const heroSkillPanel = document.getElementById("heroSkillPanel");
    const selectedHeroSkillName = document.getElementById("selectedHeroSkillName");
    const selectedHeroSkillDesc = document.getElementById("selectedHeroSkillDesc");
    const selectedHeroSkillCd = document.getElementById("selectedHeroSkillCd");
    const heroEmptyState = document.getElementById("heroEmptyState");
    const heroList = document.getElementById("heroList");
    const playerSlots = document.getElementById("playerSlots");
    const readyButton = document.getElementById("readyButton");
    const playerCount = document.getElementById("playerCount");
    const tableStatus = document.getElementById("tableStatus");
    const inviteFriendModal = document.getElementById("inviteFriendModal");
    const inviteModalList = document.getElementById("inviteModalList");
    const inviteUsernameInput = document.getElementById("inviteUsernameInput");
    const notificationPanel = document.getElementById("notificationPanel");
    const notificationBackdrop = document.getElementById("notificationBackdrop");
    const openNotificationsBtn = document.getElementById("openNotificationsBtn");
    const closeNotificationsBtn = document.getElementById("closeNotificationsBtn");
    const markAllNotificationsBtn = document.getElementById("markAllNotificationsBtn");
    const notificationList = document.getElementById("notificationList");
    const menuLogoutBtn = document.getElementById("menuLogoutBtn");

    let roomState = null;
    let heroes = [];
    let pollingHandle = null;
    let lastSelectedHeroUid = null;
    /** Một lần: gán hero mặc định từ hồ sơ nếu chưa chọn. */
    let defaultHeroSynced = false;
    /** Chỉ gọi join-by-roomId lần đầu (poll sau không cần POST lặp). */
    let ensuredRoomJoin = false;

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
        if (!tableStatus) {
            return;
        }
        tableStatus.textContent = message;
        tableStatus.style.color = isError ? "#ffd7d7" : "#fff8eb";
    };

    const getHeaders = (includeJson = false) => {
        const headers = {};
        if (accountId) {
            headers["X-Account-Id"] = accountId;
        }
        if (includeJson) {
            headers["Content-Type"] = "application/json";
        }
        return headers;
    };

    const handleUnauthorized = (response) => {
        if (response.status === 401 || response.status === 403) {
            setStatus("Phien dang nhap khong hop le. Vui long dang nhap lai.", true);
            window.setTimeout(() => {
                window.location.href = "/login";
            }, 1200);
            return true;
        }
        return false;
    };

    const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const getHeroById = (heroId) => {
        if (heroId == null || heroId === "") {
            return null;
        }
        const n = Number(heroId);
        return heroes.find((hero) => Number(hero.heroId) === n) || null;
    };

    const applyAvatar = (element, username, avatarUrl) => {
        if (!element) {
            return;
        }

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

    const renderSelectedHero = () => {
        const apiHero = roomState?.currentPlayer?.selectedHero;
        const merged = apiHero?.heroId != null ? getHeroById(apiHero.heroId) : null;
        const hero = merged || apiHero;

        if (selectedHeroName) {
            selectedHeroName.textContent = hero?.name || "No hero selected";
        }

        if (heroSkillPanel && selectedHeroSkillName && selectedHeroSkillDesc && selectedHeroSkillCd) {
            const hasHero = Boolean(hero?.name);
            const hasSkillText = Boolean(hero?.skillName || hero?.skillDescription);
            if (hasHero && hasSkillText) {
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

        if (!selectedHeroImage || !heroEmptyState) {
            return;
        }

        if (window.HeroSystem && lastSelectedHeroUid) {
            window.HeroSystem.stopIdle(lastSelectedHeroUid);
            lastSelectedHeroUid = null;
        }

        if (selectedHeroSvgHost) {
            selectedHeroSvgHost.innerHTML = "";
            selectedHeroSvgHost.hidden = true;
        }

        if (hero?.imageUrl) {
            selectedHeroImage.src = hero.imageUrl;
            selectedHeroImage.style.display = "block";
            heroEmptyState.style.display = "none";
            selectedHeroImage.onerror = () => {
                selectedHeroImage.style.display = "none";
                if (hero?.name && window.HeroSystem && selectedHeroSvgHost) {
                    lastSelectedHeroUid = `sel-${hero.heroId}`;
                    selectedHeroSvgHost.innerHTML = window.HeroSystem.getSVG(hero.name, lastSelectedHeroUid);
                    selectedHeroSvgHost.hidden = false;
                    heroEmptyState.style.display = "none";
                    window.HeroSystem.startIdle(lastSelectedHeroUid, 0);
                } else {
                    heroEmptyState.style.display = "grid";
                }
            };
            return;
        }

        if (hero?.name && window.HeroSystem && selectedHeroSvgHost) {
            lastSelectedHeroUid = `sel-${hero.heroId}`;
            selectedHeroSvgHost.innerHTML = window.HeroSystem.getSVG(hero.name, lastSelectedHeroUid);
            selectedHeroSvgHost.hidden = false;
            selectedHeroImage.style.display = "none";
            heroEmptyState.style.display = "none";
            selectedHeroImage.removeAttribute("src");
            window.HeroSystem.startIdle(lastSelectedHeroUid, 0);
            return;
        }

        selectedHeroImage.removeAttribute("src");
        selectedHeroImage.style.display = "none";
        heroEmptyState.style.display = "grid";
    };

    const renderHeroList = () => {
        if (!heroList) {
            return;
        }

        if (!heroes.length) {
            heroList.innerHTML = "";
            return;
        }

        const currentHeroId = roomState?.currentPlayer?.selectedHero?.heroId;
        const currentHeroNum = currentHeroId != null ? Number(currentHeroId) : null;
        const useSvg = typeof window.HeroSystem !== "undefined";

        heroList.innerHTML = heroes
            .map((hero) => {
                const uid = `list-${hero.heroId}`;
                const svgBlock =
                    useSvg && hero.name
                        ? `<div class="hero-option-media hero-option-media--svg">${window.HeroSystem.getSVG(hero.name, uid)}</div>`
                        : `<div class="hero-option-media">
                    <img
                        src="${escapeAttr(hero.imageUrl || "")}"
                        alt="${escapeAttr(hero.name)}"
                        onerror="this.style.display='none';this.nextElementSibling.style.display='grid';"
                    >
                    <div class="hero-option-fallback" style="display:none;">${escapeHtml(hero.name)}</div>
                </div>`;
                return `
            <button
                type="button"
                class="hero-option ${currentHeroNum != null && currentHeroNum === Number(hero.heroId) ? "active" : ""}"
                data-action="select-hero"
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
                    window.HeroSystem.startIdle(`list-${hero.heroId}`, idx * 0.1);
                });
            });
        }
    };

    const resolveSlotHero = (player) => {
        if (!player?.selectedHeroId) {
            return null;
        }

        if (roomState?.currentPlayer?.playerId === player.playerId && roomState?.currentPlayer?.selectedHero) {
            return roomState.currentPlayer.selectedHero;
        }

        return getHeroById(player.selectedHeroId);
    };

    const renderOccupiedSlot = (player) => {
        const hero = resolveSlotHero(player);
        const heroName = hero?.name || "No hero selected";
        const heroImageUrl = hero?.imageUrl || "";
        let readyText;
        let readyClass;
        if (player.isHost) {
            readyText = "Chủ phòng";
            readyClass = "ready-host";
        } else {
            readyText = player.isReady ? "READY" : "NOT READY";
            readyClass = player.isReady ? "ready-on" : "ready-off";
        }
        const avatarMarkup = player.avatarUrl
            ? `<img class="slot-avatar-image" src="${player.avatarUrl}" alt="${player.username}">`
            : `<span>${(player.username || "P").slice(0, 2).toUpperCase()}</span>`;

        return `
            <article class="slot-card occupied combined-card">
                <div class="slot-card-header">
                    <div class="slot-host-avatar">${avatarMarkup}</div>
                    <div class="slot-meta">
                        <strong>${player.username}</strong>
                        <span>${player.isHost ? "Host" : "Player"}</span>
                    </div>
                </div>
                <div class="slot-hero-media ${window.HeroSystem && hero?.name && !heroImageUrl ? "slot-hero-media--svg" : ""}">
                    ${
                        heroImageUrl
                            ? `
                            <img
                                class="slot-hero-image"
                                src="${escapeAttr(heroImageUrl)}"
                                alt="${escapeAttr(heroName)}"
                                onerror="this.style.display='none';this.nextElementSibling.style.display='grid';"
                            >
                            <div class="slot-hero-placeholder" style="display:none;">${escapeHtml(heroName)}</div>
                        `
                            : window.HeroSystem && hero?.name
                              ? `<div class="slot-hero-svg-wrap">${window.HeroSystem.getSVG(hero.name, `slot-hero-${player.playerId}`)}</div>`
                              : `<div class="slot-hero-placeholder">${escapeHtml(heroName)}</div>`
                    }
                </div>
                <div class="slot-hero-name">${heroName}</div>
                <div class="slot-ready ${readyClass}">${readyText}</div>
            </article>
        `;
    };

    const renderPlayerSlots = () => {
        if (!playerSlots || !roomState?.room) {
            return;
        }

        const playersBySlot = new Map((roomState.players || []).map((player) => [player.slotIndex, player]));
        const maxPlayers = roomState.room.maxPlayers || 4;
        const slotMarkup = [];

        for (let slotIndex = 1; slotIndex <= maxPlayers; slotIndex += 1) {
            const player = playersBySlot.get(slotIndex);
            if (player) {
                slotMarkup.push(renderOccupiedSlot(player));
            } else {
                slotMarkup.push(`
                    <article class="slot-card empty">
                        <button type="button" class="add-slot-button" data-action="add-player">+</button>
                    </article>
                `);
            }
        }

        playerSlots.innerHTML = slotMarkup.join("");
        if (window.HeroSystem && playerSlots) {
            window.requestAnimationFrame(() => {
                (roomState?.players || []).forEach((pl, idx) => {
                    window.HeroSystem.startIdle(`slot-hero-${pl.playerId}`, idx * 0.07);
                });
            });
        }
    };

    const renderRoom = () => {
        const currentPlayer = roomState?.currentPlayer || {};
        const currentPlayerState = (roomState?.players || []).find((player) => player.playerId === currentPlayer.playerId);

        if (playerName) {
            playerName.textContent = currentPlayer.username || "Nguoi choi";
        }
        applyAvatar(playerAvatar, currentPlayer.username, currentPlayer.avatarUrl);
        if (playerCoins) {
            playerCoins.textContent = formatNumber(currentPlayer.coins);
        }
        if (playerTickets) {
            playerTickets.textContent = currentPlayer.tickets ?? 0;
        }
        refreshPrivateRoomCurrencyIcons();
        if (roomCode) {
            const currentRoomCode = roomState?.room?.roomCode ?? "-";
            roomCode.textContent = currentRoomCode;
        }
        if (roomVisibilityIndicator) {
            roomVisibilityIndicator.textContent = roomState?.room?.visibility === "PRIVATE" ? "🔒" : "🌐";
        }
        if (readyButton) {
            const isHost = currentPlayerState?.isHost === true;
            const players = roomState?.players || [];
            const guests = players.filter((p) => !p.isHost);
            const allGuestsReady = guests.length > 0 && guests.every((p) => p.isReady);
            const enoughPlayers = players.length >= 2;

            if (isHost) {
                readyButton.textContent = "Bắt đầu";
                readyButton.setAttribute("data-action", "start-game");
                readyButton.disabled = !enoughPlayers || !allGuestsReady;
            } else {
                readyButton.textContent = currentPlayerState?.isReady ? "UNREADY" : "READY";
                readyButton.setAttribute("data-action", "ready");
                readyButton.disabled = false;
            }
        }
        if (playerCount) {
            playerCount.textContent = `${roomState?.players?.length || 0}/${roomState?.room?.maxPlayers || 4} players`;
        }

        renderSelectedHero();
        renderHeroList();
        renderPlayerSlots();
        setStatus(`Phong ${roomState?.room?.name || ""}`);
    };

    const openInviteModal = () => {
        if (!inviteFriendModal || !inviteModalList) {
            return;
        }
        const friends = roomState?.inviteFriends;
        inviteFriendModal.hidden = false;
        inviteFriendModal.setAttribute("aria-hidden", "false");
        if (!Array.isArray(friends) || friends.length === 0) {
            inviteModalList.innerHTML = `<li class="invite-friends-empty">Chưa có bạn để mời. Thêm bạn ở trang <a href="/friends">Bạn bè</a> hoặc gửi mã phòng.</li>`;
            return;
        }
        inviteModalList.innerHTML = friends
            .map((f) => {
                const name = escapeHtml(f.username || "—");
                const pid = f.userProfileId != null ? String(f.userProfileId) : "";
                const pst = f.presenceStatus === "ONLINE" ? "Trực tuyến" : f.presenceStatus === "PLAYING" ? "Đang chơi" : "Offline";
                return `<li class="invite-modal-row">
            <span class="invite-modal-name">${name}<small class="invite-presence"> · ${escapeHtml(pst)}</small></span>
            <button type="button" class="invite-friend-btn" data-action="invite-friend" data-profile-id="${escapeAttr(pid)}">Mời</button>
          </li>`;
            })
            .join("");
    };

    const closeInviteModal = () => {
        if (!inviteFriendModal) {
            return;
        }
        inviteFriendModal.hidden = true;
        inviteFriendModal.setAttribute("aria-hidden", "true");
    };

    const postRoomInvite = async (toUserProfileId) => {
        const response = await fetch("/api/social/room-invite", {
            method: "POST",
            headers: getHeaders(true),
            body: JSON.stringify({
                roomId: Number(roomId),
                toUserProfileId: Number(toUserProfileId)
            })
        });
        if (handleUnauthorized(response)) {
            return;
        }
        if (!response.ok) {
            const err = await response.text();
            throw new Error(err || "Không gửi được lời mời.");
        }
    };

    const postRoomInviteByUsername = async (username) => {
        const response = await fetch("/api/social/room-invite", {
            method: "POST",
            headers: getHeaders(true),
            body: JSON.stringify({
                roomId: Number(roomId),
                username: String(username).trim()
            })
        });
        if (handleUnauthorized(response)) {
            return;
        }
        if (!response.ok) {
            const err = await response.text();
            throw new Error(err || "Không gửi được lời mời.");
        }
    };

    /** Vào phòng theo roomId (link thông báo) — GET chi tiết trước đây lỗi vì chưa có RoomPlayer. */
    const ensureJoinedRoom = async () => {
        const joinRes = await fetch(`/api/rooms/${roomId}/join`, {
            method: "POST",
            headers: getHeaders(true),
            body: JSON.stringify({})
        });
        if (handleUnauthorized(joinRes)) {
            throw new Error("Phiên đăng nhập không hợp lệ.");
        }
        if (!joinRes.ok) {
            const t = ((await joinRes.text()) || "").trim();
            if (t.includes("Invalid room password") || t.toLowerCase().includes("password")) {
                throw new Error(
                    "Phòng riêng cần mật khẩu. Hãy vào từ menu chính: nhập mã phòng và mật khẩu."
                );
            }
            if (t.includes("Room is full")) {
                throw new Error("Phòng đã đầy.");
            }
            if (t.includes("not joinable")) {
                throw new Error("Phòng không còn chờ hoặc đã vào ván.");
            }
            throw new Error(t || "Không thể vào phòng.");
        }
    };

    const loadHeroes = async () => {
        try {
            const response = await fetch("/api/heroes/owned", {
                headers: getHeaders()
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                heroes = [];
                return;
            }

            heroes = await response.json();
        } catch (error) {
            console.error(error);
            heroes = [];
        }
    };

    const maybeRedirectToGameBoard = () => {
        const gid = roomState?.room?.activeGameId;
        const st = roomState?.room?.status;
        if (gid != null && String(st).toUpperCase() === "IN_GAME") {
            window.location.href = `/game-board?gameId=${gid}&roomId=${encodeURIComponent(roomId)}`;
            return true;
        }
        return false;
    };

    const loadRoom = async () => {
        if (!accountId) {
            setStatus("Ban chua dang nhap.", true);
            return;
        }
        if (!roomId) {
            setStatus("Thieu roomId tren URL.", true);
            return;
        }

        try {
            if (!ensuredRoomJoin) {
                await ensureJoinedRoom();
                ensuredRoomJoin = true;
            }

            const response = await fetch(`/api/rooms/${roomId}`, {
                headers: getHeaders()
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai thong tin phong.");
            }

            roomState = await response.json();

            if (
                !defaultHeroSynced &&
                roomState?.currentPlayer &&
                !roomState.currentPlayer.selectedHero
            ) {
                defaultHeroSynced = true;
                try {
                    const syncRes = await fetch(`/api/rooms/${roomId}/hero/apply-default`, {
                        method: "POST",
                        headers: getHeaders(true),
                        body: "{}"
                    });
                    if (syncRes.ok) {
                        const again = await fetch(`/api/rooms/${roomId}`, { headers: getHeaders() });
                        if (again.ok) {
                            roomState = await again.json();
                        }
                    }
                } catch (e) {
                    console.warn(e);
                }
            }

            if (maybeRedirectToGameBoard()) {
                return;
            }
            renderRoom();
        } catch (error) {
            const netFail =
                error instanceof TypeError &&
                (String(error.message).includes("fetch") || String(error.message).includes("Failed to fetch"));
            const msg = netFail
                ? "Khong ket noi duoc server. Hay chay Spring Boot (vd: port 8080)."
                : error.message || "Khong the tai thong tin phong.";
            setStatus(msg, true);
        }
    };

    const postHeroSelection = async (heroId) => {
        const response = await fetch(`/api/rooms/${roomId}/hero`, {
            method: "POST",
            headers: getHeaders(true),
            body: JSON.stringify({ heroId: Number(heroId) })
        });

        if (handleUnauthorized(response)) {
            return;
        }
        if (!response.ok) {
            throw new Error("Khong the chon hero.");
        }

        await loadRoom();
    };

    const postReady = async () => {
        const currentPlayer = roomState?.currentPlayer;
        const currentPlayerState = (roomState?.players || []).find((player) => player.playerId === currentPlayer?.playerId);
        const nextReadyValue = !currentPlayerState?.isReady;

        const response = await fetch(`/api/rooms/${roomId}/ready`, {
            method: "POST",
            headers: getHeaders(true),
            body: JSON.stringify({ ready: nextReadyValue })
        });

        if (handleUnauthorized(response)) {
            return;
        }
        if (!response.ok) {
            throw new Error("Khong the cap nhat trang thai san sang.");
        }

        await loadRoom();
    };

    const postStartGame = async () => {
        const response = await fetch(`/api/rooms/${roomId}/start`, {
            method: "POST",
            headers: getHeaders(true),
            body: "{}"
        });

        if (handleUnauthorized(response)) {
            return;
        }
        if (!response.ok) {
            const err = await response.text();
            throw new Error(err || "Không thể bắt đầu trận.");
        }

        const data = await response.json();
        if (data?.redirectUrl) {
            window.location.href = data.redirectUrl;
        }
    };

    document.addEventListener("click", async (event) => {
        const target = event.target.closest("[data-action]");
        if (!target) {
            return;
        }

        const action = target.getAttribute("data-action");

        try {
            if (action === "select-hero") {
                const heroId = target.getAttribute("data-hero-id");
                await postHeroSelection(heroId);
            }

            if (action === "ready") {
                await postReady();
            }

            if (action === "start-game") {
                await postStartGame();
            }

            if (action === "copy-code" && roomCode?.textContent && navigator.clipboard) {
                await navigator.clipboard.writeText(roomCode.textContent);
                setStatus("Da sao chep ma phong.");
            }

            if (action === "add-player") {
                openInviteModal();
            }

            if (action === "close-invite-modal") {
                closeInviteModal();
            }

            if (action === "invite-friend") {
                const pid = target.getAttribute("data-profile-id");
                if (!pid) {
                    return;
                }
                await postRoomInvite(pid);
                setStatus("Đã gửi lời mời (bạn bè xem thông báo chuông ở menu chính).");
                closeInviteModal();
            }

            if (action === "invite-by-username") {
                const name = inviteUsernameInput?.value?.trim();
                if (!name) {
                    setStatus("Nhập username người cần mời.", true);
                    return;
                }
                await postRoomInviteByUsername(name);
                if (inviteUsernameInput) {
                    inviteUsernameInput.value = "";
                }
                setStatus("Đã gửi lời mời (thông báo ở menu chính — chuông).");
                closeInviteModal();
            }

            if (action === "home") {
                window.location.href = "/home";
            }
        } catch (error) {
            console.error(error);
            setStatus(error.message || "Thao tac that bai.", true);
        }
    });

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

    const escapeHtmlNote = (s) =>
        String(s ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

    const openNotificationPanel = () => {
        if (!notificationPanel) {
            return;
        }
        notificationPanel.hidden = false;
        notificationPanel.setAttribute("aria-hidden", "false");
        loadNotificationsList();
    };

    const closeNotificationPanel = () => {
        if (!notificationPanel) {
            return;
        }
        notificationPanel.hidden = true;
        notificationPanel.setAttribute("aria-hidden", "true");
    };

    const loadNotificationsList = async () => {
        if (!notificationList || !accountId) {
            return;
        }
        try {
            const response = await fetch("/api/social/notifications", { headers: getHeaders() });
            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                notificationList.innerHTML = "<li>Không tải được thông báo.</li>";
                return;
            }
            const items = await response.json();
            if (!Array.isArray(items) || items.length === 0) {
                notificationList.innerHTML = '<li class="notification-empty">Chưa có thông báo.</li>';
                return;
            }
            notificationList.innerHTML = items
                .map((it) => {
                    const unread = it.read === false;
                    const cls = unread ? "notification-item notification-item--unread" : "notification-item";
                    const rid = it.roomId != null ? String(it.roomId) : "";
                    const typ = String(it.type || "");
                    const sender = it.senderUsername ? escapeHtmlNote(it.senderUsername) : "";
                    const meta = (sender ? `${sender} · ` : "") + escapeHtmlNote(it.createdAt || "");
                    return `<li>
  <button type="button" class="${cls}" data-notification-id="${it.notificationId}" data-type="${escapeHtmlNote(typ)}" data-room-id="${escapeHtmlNote(rid)}">
    <div class="notification-item-title">${escapeHtmlNote(it.title || "Thông báo")}</div>
    <div class="notification-item-meta">${meta}</div>
    <div class="notification-item-body">${escapeHtmlNote(it.body || "")}</div>
  </button>
</li>`;
                })
                .join("");
        } catch (e) {
            console.warn(e);
            notificationList.innerHTML = "<li>Lỗi tải thông báo.</li>";
        }
    };

    openNotificationsBtn?.addEventListener("click", (e) => {
        e.preventDefault();
        e.stopPropagation();
        openNotificationPanel();
    });
    notificationBackdrop?.addEventListener("click", closeNotificationPanel);
    closeNotificationsBtn?.addEventListener("click", closeNotificationPanel);
    markAllNotificationsBtn?.addEventListener("click", async () => {
        try {
            const r = await fetch("/api/social/notifications/read-all", {
                method: "POST",
                headers: getHeaders(true),
                body: "{}"
            });
            if (r.ok) {
                await loadNotificationsList();
            }
        } catch (e) {
            console.warn(e);
        }
    });
    menuLogoutBtn?.addEventListener("click", () => {
        localStorage.removeItem("accountId");
        window.location.href = "/login";
    });

    notificationList?.addEventListener("click", async (ev) => {
        const btn = ev.target.closest("[data-notification-id]");
        if (!btn) {
            return;
        }
        ev.preventDefault();
        const id = btn.getAttribute("data-notification-id");
        const type = btn.getAttribute("data-type");
        const rid = btn.getAttribute("data-room-id");
        if (!id) {
            return;
        }
        try {
            await fetch(`/api/social/notifications/${id}/read`, {
                method: "POST",
                headers: getHeaders(true),
                body: "{}"
            });
        } catch (e) {
            console.warn(e);
        }
        btn.classList.remove("notification-item--unread");
        if (type === "ROOM_INVITE" && rid) {
            window.location.href = `/private-table?roomId=${encodeURIComponent(rid)}`;
        } else if (type === "FRIEND_REQUEST") {
            window.location.href = "/friends";
        } else {
            await loadNotificationsList();
        }
    });

    document.querySelectorAll("[data-route]").forEach((el) => {
        el.addEventListener("click", (event) => {
            const route = event.currentTarget.getAttribute("data-route");
            if (route) {
                event.preventDefault();
                window.location.href = route;
            }
        });
    });

    try {
        if (window.HeroSystem?.preloadCharacterSvgs) {
            await window.HeroSystem.preloadCharacterSvgs();
        }
    } catch (e) {
        console.warn("Hero SVG preload failed", e);
    }

    const agBoot = await fetchActiveGame();
    if (agBoot?.hasActiveGame) {
        if (agBoot.soloVsAi) {
            if (window.confirm("Bạn đang còn trong ván đấu máy. Có muốn quay lại không?")) {
                window.location.href = `/game-board?gameId=${agBoot.gameId}&vsBot=1`;
                return;
            }
        } else if (agBoot.roomId != null && String(agBoot.roomId) !== String(roomId)) {
            if (window.confirm("Bạn đang còn trong ván multiplayer. Có muốn quay lại không?")) {
                window.location.href = `/game-board?gameId=${agBoot.gameId}&roomId=${agBoot.roomId}`;
                return;
            }
        }
    }

    await Promise.all([loadHeroes(), loadRoom()]);
    pollingHandle = window.setInterval(loadRoom, 2000);
    window.addEventListener("beforeunload", () => {
        if (pollingHandle) {
            window.clearInterval(pollingHandle);
        }
    });
});
