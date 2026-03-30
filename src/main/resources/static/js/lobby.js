document.addEventListener("DOMContentLoaded", () => {
    const accountId = localStorage.getItem("accountId");
    const roomList = document.getElementById("roomList");
    const lobbyStatus = document.getElementById("lobbyStatus");
    const createRoomButton = document.getElementById("createRoomButton");
    const joinRoomButton = document.getElementById("joinRoomButton");
    const createRoomName = document.getElementById("createRoomName");
    const createVisibility = document.getElementById("createVisibility");
    const createPassword = document.getElementById("createPassword");
    const createMode = document.getElementById("createMode");
    const createMaxPlayers = document.getElementById("createMaxPlayers");
    const joinRoomCode = document.getElementById("joinRoomCode");
    const joinRoomPassword = document.getElementById("joinRoomPassword");
    const friendList = document.getElementById("friendList");
    const friendStatus = document.getElementById("friendStatus");
    const friendUsernameInput = document.getElementById("friendUsernameInput");
    const addFriendButton = document.getElementById("addFriendButton");
    const channelChatMessages = document.getElementById("channelChatMessages");
    const channelChatInput = document.getElementById("channelChatInput");
    const sendChannelChatButton = document.getElementById("sendChannelChatButton");
    let channelChatPoller = null;

    const setStatus = (message, isError = false) => {
        if (!lobbyStatus) {
            return;
        }
        lobbyStatus.textContent = message;
        lobbyStatus.style.color = isError ? "#ffd2d2" : "rgba(247, 248, 255, 0.8)";
    };

    const getAuthHeaders = (includeJson = false) => {
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

    const renderRooms = (items) => {
        if (!roomList) {
            return;
        }

        if (!items || items.length === 0) {
            roomList.innerHTML = `
                <article class="room-card">
                    <strong>Chua co phong cho</strong>
                    <span>Hay tao phong dau tien de bat dau.</span>
                </article>
            `;
            return;
        }

        roomList.innerHTML = items.map((room) => `
            <article class="room-card">
                <strong>${room.name}</strong>
                <span>Che do: ${room.mode}</span>
                <span>${room.currentPlayers} / ${room.maxPlayers} nguoi choi</span>
                <button
                    type="button"
                    class="accent join-room-button"
                    data-room-code="${room.roomCode}"
                    data-room-visibility="${room.visibility}"
                >
                    JOIN
                </button>
            </article>
        `).join("");
    };

    const setFriendStatus = (message, isError = false) => {
        if (!friendStatus) {
            return;
        }
        friendStatus.textContent = message;
        friendStatus.style.color = isError ? "#ffd2d2" : "rgba(247, 248, 255, 0.8)";
    };

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

    const safePortraitSrc = (url) => {
        if (!url || typeof url !== "string") return "";
        const t = url.trim();
        if (t.startsWith("/") || t.startsWith("http://") || t.startsWith("https://")) return t;
        return "";
    };

    const renderFriends = (items) => {
        if (!friendList) {
            return;
        }
        if (!items || !items.length) {
            friendList.innerHTML = `
                <article class="player-row">
                    <span class="lobby-avatar-frame lobby-avatar-frame--animated lobby-avatar-frame--placeholder" aria-hidden="true">
                        <span class="lobby-avatar-initials">?</span>
                    </span>
                    <div>
                        <strong>Chua co ban be</strong>
                        <span>Nhap username de gui loi moi ket ban.</span>
                    </div>
                </article>
            `;
            return;
        }

        friendList.innerHTML = items
            .map((friend) => {
                const src = safePortraitSrc(friend.avatarUrl);
                const initial = (friend.username || "U").slice(0, 2).toUpperCase();
                const avatarBlock = src
                    ? `<span class="lobby-avatar-frame lobby-avatar-frame--animated"><img class="lobby-avatar-img" src="${escapeAttr(
                          src
                      )}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'"><span class="lobby-avatar-initials lobby-avatar-fallback" style="display:none">${escapeHtml(
                          initial
                      )}</span></span>`
                    : `<span class="lobby-avatar-frame lobby-avatar-frame--animated"><span class="lobby-avatar-initials">${escapeHtml(
                          initial
                      )}</span></span>`;
                return `
            <article class="player-row">
                <div class="friend-row">
                    <div class="friend-main">
                        ${avatarBlock}
                        <div class="friend-meta">
                            <strong>${escapeHtml(friend.username || "Unknown")}</strong>
                            <span class="friend-status">${escapeHtml(friend.status || "PENDING")}</span>
                        </div>
                    </div>
                    ${friend.unreadMessages > 0 ? `<span class="unread-badge">${friend.unreadMessages}</span>` : ""}
                </div>
            </article>
        `;
            })
            .join("");
    };

    const loadFriends = async () => {
        if (!accountId) {
            return;
        }
        setFriendStatus("Dang tai danh sach ban be...");
        try {
            const response = await fetch("/api/social/friends", {
                headers: getAuthHeaders()
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai danh sach ban be.");
            }

            const data = await response.json();
            renderFriends(data || []);
            setFriendStatus(`Da tai ${data?.length || 0} ban be.`);
        } catch (error) {
            console.error(error);
            setFriendStatus(error.message || "Khong the tai danh sach ban be.", true);
        }
    };

    const addFriend = async () => {
        const friendUsername = friendUsernameInput?.value?.trim();
        if (!friendUsername) {
            setFriendStatus("Vui long nhap username ban be.", true);
            return;
        }

        addFriendButton.disabled = true;
        setFriendStatus("Dang gui loi moi ket ban...");
        try {
            const response = await fetch("/api/social/friends/request", {
                method: "POST",
                headers: getAuthHeaders(true),
                body: JSON.stringify({ username: friendUsername })
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Gui loi moi that bai.");
            }

            friendUsernameInput.value = "";
            setFriendStatus("Da gui loi moi ket ban.");
            await loadFriends();
        } catch (error) {
            console.error(error);
            setFriendStatus(error.message || "Gui loi moi that bai.", true);
        } finally {
            addFriendButton.disabled = false;
        }
    };

    const renderChannelMessages = (items) => {
        if (!channelChatMessages) {
            return;
        }
        if (!items || !items.length) {
            channelChatMessages.innerHTML = `<article class="channel-chat-item"><p>Chưa có tin nhắn nào trong kênh.</p></article>`;
            return;
        }

        channelChatMessages.innerHTML = items.map((msg) => `
            <article class="channel-chat-item">
                <strong>User #${msg.senderId || "-"}</strong>
                <p>${msg.content || ""}</p>
                <span>${msg.createdAt || ""}</span>
            </article>
        `).join("");
        channelChatMessages.scrollTop = channelChatMessages.scrollHeight;
    };

    const loadChannelMessages = async () => {
        if (!accountId) {
            return;
        }
        try {
            const response = await fetch("/api/social/channel/messages", {
                headers: getAuthHeaders()
            });
            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                return;
            }
            const data = await response.json();
            renderChannelMessages(data || []);
        } catch (error) {
            console.error(error);
        }
    };

    const sendChannelMessage = async () => {
        const content = channelChatInput?.value?.trim();
        if (!content) {
            return;
        }
        sendChannelChatButton.disabled = true;
        try {
            const response = await fetch("/api/social/channel/messages", {
                method: "POST",
                headers: getAuthHeaders(true),
                body: JSON.stringify({ content })
            });
            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Gửi tin nhắn thất bại");
            }
            channelChatInput.value = "";
            await loadChannelMessages();
        } catch (error) {
            console.error(error);
            setFriendStatus(error.message || "Không thể gửi tin nhắn.", true);
        } finally {
            sendChannelChatButton.disabled = false;
        }
    };

    const loadRooms = async () => {
        if (!accountId) {
            setStatus("Ban chua dang nhap.", true);
            return;
        }

        setStatus("Dang tai danh sach phong...");
        try {
            const response = await fetch("/api/rooms?status=WAITING&page=0&size=20", {
                headers: getAuthHeaders()
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai danh sach phong.");
            }

            const data = await response.json();
            renderRooms(data.items || []);
            setStatus(`Da tai ${data.items?.length || 0} phong.`);
        } catch (error) {
            console.error(error);
            setStatus(error.message || "Khong the tai danh sach phong.", true);
        }
    };

    const createRoom = async () => {
        const payload = {
            name: createRoomName?.value?.trim() || "Phong cua toi",
            visibility: createVisibility?.value || "PRIVATE",
            password: createPassword?.value || "",
            mode: createMode?.value || "QUICK_GAME",
            maxPlayers: Number(createMaxPlayers?.value || 4)
        };

        if (payload.visibility === "PRIVATE" && !payload.password) {
            setStatus("Phong rieng tu can mat khau.", true);
            return;
        }

        createRoomButton.disabled = true;
        setStatus("Dang tao phong...");
        try {
            const response = await fetch("/api/rooms", {
                method: "POST",
                headers: getAuthHeaders(true),
                body: JSON.stringify(payload)
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Tao phong that bai.");
            }

            const data = await response.json();
            window.location.href = data.redirectUrl;
        } catch (error) {
            console.error(error);
            setStatus(error.message || "Tao phong that bai.", true);
        } finally {
            createRoomButton.disabled = false;
        }
    };

    const joinRoom = async (roomCode, password = "") => {
        if (!roomCode) {
            setStatus("Vui long nhap ma phong.", true);
            return;
        }

        joinRoomButton.disabled = true;
        setStatus("Dang tham gia phong...");
        try {
            const response = await fetch("/api/rooms/join", {
                method: "POST",
                headers: getAuthHeaders(true),
                body: JSON.stringify({
                    roomCode: roomCode.trim().toUpperCase(),
                    password
                })
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tham gia phong.");
            }

            const data = await response.json();
            window.location.href = data.redirectUrl;
        } catch (error) {
            console.error(error);
            setStatus(error.message || "Khong the tham gia phong.", true);
        } finally {
            joinRoomButton.disabled = false;
        }
    };

    createVisibility?.addEventListener("change", () => {
        if (!createPassword) {
            return;
        }
        createPassword.disabled = createVisibility.value !== "PRIVATE";
        if (createPassword.disabled) {
            createPassword.value = "";
        }
    });

    createRoomButton?.addEventListener("click", createRoom);
    joinRoomButton?.addEventListener("click", () => joinRoom(joinRoomCode?.value, joinRoomPassword?.value || ""));
    addFriendButton?.addEventListener("click", addFriend);
    friendUsernameInput?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            addFriend();
        }
    });
    sendChannelChatButton?.addEventListener("click", sendChannelMessage);
    channelChatInput?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            sendChannelMessage();
        }
    });

    roomList?.addEventListener("click", (event) => {
        const target = event.target.closest(".join-room-button");
        if (!target) {
            return;
        }

        const roomCode = target.getAttribute("data-room-code");
        const visibility = target.getAttribute("data-room-visibility");
        let password = "";

        if (visibility === "PRIVATE") {
            password = window.prompt("Nhap mat khau phong:") || "";
        }

        joinRoom(roomCode, password);
    });

    if (createPassword) {
        createPassword.disabled = createVisibility?.value !== "PRIVATE";
    }

    loadRooms();
    loadFriends();
    loadChannelMessages();
    channelChatPoller = window.setInterval(loadChannelMessages, 3000);
    window.addEventListener("beforeunload", () => {
        if (channelChatPoller) {
            window.clearInterval(channelChatPoller);
        }
    });
});
