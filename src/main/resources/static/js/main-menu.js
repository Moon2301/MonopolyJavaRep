window.onload = async () => {
    const accountId = localStorage.getItem("accountId");
    let topupResult = null;
    try {
        const params = new URLSearchParams(window.location.search);
        const raw = params.get("topupSuccess");
        if (raw !== null) {
            params.delete("topupSuccess");
            const qs = params.toString();
            window.history.replaceState({}, "", window.location.pathname + (qs ? `?${qs}` : ""));
            topupResult = raw;
        }
    } catch (e) {
        console.warn(e);
    }
    const playerAvatar = document.getElementById("playerAvatar");
    const playerName = document.getElementById("playerName");
    const playerCoins = document.getElementById("playerCoins");
    const playerTickets = document.getElementById("playerTickets");
    const playerProfileBanner = document.getElementById("playerProfileBanner");
    const roomCodeInput = document.getElementById("roomCodeInput");
    const createRoomButton = document.getElementById("createRoomButton");
    const joinRoomButton = document.getElementById("joinRoomButton");
    const roomActionStatus = document.getElementById("roomActionStatus");
    const routeTargets = document.querySelectorAll("[data-route]");
    const friendBadge = document.getElementById("friendBadge");
    const notificationPanel = document.getElementById("notificationPanel");
    const notificationBackdrop = document.getElementById("notificationBackdrop");
    const openNotificationsBtn = document.getElementById("openNotificationsBtn");
    const closeNotificationsBtn = document.getElementById("closeNotificationsBtn");
    const markAllNotificationsBtn = document.getElementById("markAllNotificationsBtn");
    const notificationList = document.getElementById("notificationList");
    const notificationBadge = document.getElementById("notificationBadge");
    const menuLogoutBtn = document.getElementById("menuLogoutBtn");

    const setRoomStatus = (message, isError = false) => {
        if (!roomActionStatus) {
            return;
        }
        roomActionStatus.textContent = message;
        roomActionStatus.style.color = isError ? "#ffd7d7" : "#fff8ed";
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

    const handleUnauthorized = (response, shouldRedirect = false) => {
        if (response.status === 401 || response.status === 403) {
            if (shouldRedirect) {
                window.location.href = "/login";
            }
            return true;
        }
        return false;
    };

    const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

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

    /** @returns {"proceed"|"blocked"|"redirected"} */
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

    const refreshMenuCurrencyIcons = () => {
        if (window.CoinSystem && typeof CoinSystem.initCurrencySlots === "function") {
            CoinSystem.initCurrencySlots([
                { elId: "menuCoinSilver", type: "silver" },
                { elId: "menuCoinGold", type: "gold" }
            ]);
        }
    };

    const bindHomeSummary = (data) => {
        const player = data.player || {};

        if (playerAvatar) {
            playerAvatar.src = player.avatarUrl || "/images/avatar-default.png";
        }
        if (playerName) {
            playerName.textContent = player.username || "Nguoi choi";
        }
        if (playerCoins) {
            playerCoins.textContent = formatNumber(player.coins);
        }
        if (playerTickets) {
            playerTickets.textContent = formatNumber(player.tickets);
        }
        refreshMenuCurrencyIcons();
    };

    const loadHomeSummary = async () => {
        if (!accountId) {
            window.location.href = "/login";
            return;
        }

        try {
            const response = await fetch("/api/home/summary", {
                headers: getHeaders()
            });

            if (handleUnauthorized(response, true)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai thong tin trang chu.");
            }

            const data = await response.json();
            bindHomeSummary(data);
        } catch (error) {
            console.error(error);
        }
    };

    /** Số lời mời kết bạn chờ chấp nhận (badge nút bạn bè). */
    const loadFriendBadge = async () => {
        if (!friendBadge || !accountId) {
            return;
        }
        try {
            const response = await fetch("/api/social/friends", { headers: getHeaders() });
            if (!response.ok) {
                return;
            }
            const list = await response.json();
            const pending = Array.isArray(list) ? list.filter((f) => f.canAccept === true).length : 0;
            if (pending > 0) {
                friendBadge.hidden = false;
                friendBadge.textContent = pending > 9 ? "9+" : String(pending);
            } else {
                friendBadge.hidden = true;
            }
        } catch (e) {
            console.warn(e);
        }
    };

    const escapeHtml = (s) =>
        String(s ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

    const loadNotificationBadge = async () => {
        if (!notificationBadge || !accountId) {
            return;
        }
        try {
            const response = await fetch("/api/social/notifications/unread-count", {
                headers: getHeaders()
            });
            if (!response.ok) {
                return;
            }
            const data = await response.json();
            const n = Number(data.unreadCount || 0);
            if (n > 0) {
                notificationBadge.hidden = false;
                notificationBadge.textContent = n > 9 ? "9+" : String(n);
            } else {
                notificationBadge.hidden = true;
            }
        } catch (e) {
            console.warn(e);
        }
    };

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
            if (handleUnauthorized(response, false)) {
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
                    const sender = it.senderUsername ? escapeHtml(it.senderUsername) : "";
                    const meta =
                        (sender ? `${sender} · ` : "") + escapeHtml(it.createdAt || "");
                    return `<li>
  <button type="button" class="${cls}" data-notification-id="${it.notificationId}" data-type="${escapeHtml(typ)}" data-room-id="${escapeHtml(rid)}">
    <div class="notification-item-title">${escapeHtml(it.title || "Thông báo")}</div>
    <div class="notification-item-meta">${meta}</div>
    <div class="notification-item-body">${escapeHtml(it.body || "")}</div>
  </button>
</li>`;
                })
                .join("");
        } catch (e) {
            console.warn(e);
            notificationList.innerHTML = "<li>Lỗi tải thông báo.</li>";
        }
    };

    const createRoom = async () => {
        createRoomButton.disabled = true;
        setRoomStatus("Dang tao phong...");

        try {
            const gate = await resolveActiveGameGate();
            if (gate === "redirected") {
                return;
            }
            if (gate === "blocked") {
                throw new Error("Bạn đang trong một ván. Hãy kết thúc hoặc quay lại ván đó trước.");
            }
            const response = await fetch("/api/rooms", {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({
                    name: "My Room",
                    visibility: "PUBLIC",
                    password: "",
                    mode: "QUICK_GAME",
                    maxPlayers: 4
                })
            });

            if (handleUnauthorized(response, false)) {
                throw new Error("Khong co quyen tao phong hoac token khong hop le.");
            }
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Tao phong that bai.");
            }

            const data = await response.json();
            window.location.href = data.redirectUrl;
        } catch (error) {
            console.error(error);
            setRoomStatus(error.message || "Tao phong that bai.", true);
        } finally {
            createRoomButton.disabled = false;
        }
    };

    const joinRoom = async () => {
        const roomCode = roomCodeInput?.value?.trim().toUpperCase();
        if (!roomCode) {
            setRoomStatus("Vui long nhap room code.", true);
            return;
        }

        joinRoomButton.disabled = true;
        setRoomStatus("Dang tham gia phong...");

        try {
            const gate = await resolveActiveGameGate();
            if (gate === "redirected") {
                return;
            }
            if (gate === "blocked") {
                throw new Error("Bạn đang trong một ván. Hãy kết thúc hoặc quay lại ván đó trước.");
            }
            const response = await fetch("/api/rooms/join", {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({
                    roomCode,
                    password: ""
                })
            });

            if (handleUnauthorized(response, false)) {
                throw new Error("Khong co quyen tham gia phong hoac token khong hop le.");
            }
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Khong the tham gia phong.");
            }

            const data = await response.json();
            window.location.href = data.redirectUrl;
        } catch (error) {
            console.error(error);
            setRoomStatus(error.message || "Khong the tham gia phong.", true);
        } finally {
            joinRoomButton.disabled = false;
        }
    };

    routeTargets.forEach((target) => {
        target.addEventListener("click", (event) => {
            const route = event.currentTarget.getAttribute("data-route");
            if (route) {
                window.location.href = route;
            }
        });
    });

    const openProfile = () => {
        window.location.href = "/profile";
    };
    playerProfileBanner?.addEventListener("click", openProfile);
    playerProfileBanner?.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            openProfile();
        }
    });

    createRoomButton?.addEventListener("click", createRoom);
    joinRoomButton?.addEventListener("click", joinRoom);
    roomCodeInput?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            joinRoom();
        }
    });

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
                await loadNotificationBadge();
                await loadNotificationsList();
            }
        } catch (err) {
            console.warn(err);
        }
    });
    notificationList?.addEventListener("click", async (e) => {
        const btn = e.target.closest("[data-notification-id]");
        if (!btn) {
            return;
        }
        e.preventDefault();
        const id = btn.getAttribute("data-notification-id");
        const type = btn.getAttribute("data-type");
        const rid = btn.getAttribute("data-room-id");
        if (!id) {
            return;
        }
        try {
            const r = await fetch(`/api/social/notifications/${id}/read`, {
                method: "POST",
                headers: getHeaders(true),
                body: "{}"
            });
            if (r.ok) {
                await loadNotificationBadge();
            }
        } catch (err) {
            console.warn(err);
        }
        btn.classList.remove("notification-item--unread");
        if (type === "ROOM_INVITE" && rid) {
            window.location.href = `/private-table?roomId=${encodeURIComponent(rid)}`;
        } else if (type === "FRIEND_REQUEST") {
            window.location.href = "/friends";
        }
    });
    menuLogoutBtn?.addEventListener("click", () => {
        localStorage.removeItem("accountId");
        window.location.href = "/login";
    });

    await loadHomeSummary();
    if (topupResult === "true") {
        alert("Nạp tiền thành công! Số dư Vàng (Xu nạp) đã được cập nhật.");
    } else if (topupResult === "false") {
        alert("Giao dịch chưa hoàn tất hoặc không thành công.");
    }
    loadFriendBadge();
    loadNotificationBadge();
};
