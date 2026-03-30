document.addEventListener("DOMContentLoaded", () => {
    const accountId = localStorage.getItem("accountId");
    const inviteForm = document.getElementById("inviteForm");
    const friendUsername = document.getElementById("friendUsername");
    const sendInviteBtn = document.getElementById("sendInviteBtn");
    const friendsStatus = document.getElementById("friendsStatus");
    const incomingList = document.getElementById("incomingList");
    const acceptedList = document.getElementById("acceptedList");
    const outgoingList = document.getElementById("outgoingList");
    const incomingEmpty = document.getElementById("incomingEmpty");
    const acceptedEmpty = document.getElementById("acceptedEmpty");
    const outgoingEmpty = document.getElementById("outgoingEmpty");

    const headers = (json = false) => {
        const h = {};
        if (accountId) h["X-Account-Id"] = accountId;
        if (json) h["Content-Type"] = "application/json";
        return h;
    };

    const setStatus = (msg, isError = false) => {
        if (!friendsStatus) return;
        friendsStatus.textContent = msg;
        friendsStatus.style.color = isError ? "#b91c1c" : "#3d2a26";
    };

    const esc = (s) =>
        String(s ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");

    const render = (items) => {
        incomingList.innerHTML = "";
        acceptedList.innerHTML = "";
        outgoingList.innerHTML = "";

        const incoming = items.filter((x) => x.canAccept);
        const accepted = items.filter((x) => x.status === "ACCEPTED");
        const outgoing = items.filter((x) => x.pendingOutgoing);

        incomingEmpty.hidden = incoming.length > 0;
        acceptedEmpty.hidden = accepted.length > 0;
        outgoingEmpty.hidden = outgoing.length > 0;

        const presenceLabel = (pst) => {
            if (pst === "ONLINE") return "Trực tuyến";
            if (pst === "PLAYING") return "Đang chơi";
            return "Offline";
        };

        const row = (f, withAccept) => {
            const li = document.createElement("li");
            li.className = "friend-item";
            const av = f.avatarUrl || "/images/avatar-default.png";
            const pr = presenceLabel(f.presenceStatus);
            li.innerHTML = `
                <img src="${esc(av)}" alt="">
                <div class="meta">
                    <strong>${esc(f.username)}</strong>
                    <small>${esc(f.status)} · ${esc(pr)}</small>
                </div>
            `;
            if (withAccept) {
                const btn = document.createElement("button");
                btn.type = "button";
                btn.textContent = "Chấp nhận";
                btn.addEventListener("click", () => accept(f.friendId));
                li.appendChild(btn);
            }
            return li;
        };

        incoming.forEach((f) => incomingList.appendChild(row(f, true)));
        accepted.forEach((f) => acceptedList.appendChild(row(f, false)));
        outgoing.forEach((f) => outgoingList.appendChild(row(f, false)));
    };

    const loadFriends = async () => {
        if (!accountId) {
            window.location.href = "/login";
            return;
        }
        try {
            const res = await fetch("/api/social/friends", { headers: headers() });
            if (res.status === 401 || res.status === 403) {
                window.location.href = "/login";
                return;
            }
            if (!res.ok) throw new Error("Không tải được danh sách.");
            const items = await res.json();
            render(Array.isArray(items) ? items : []);
        } catch (e) {
            console.error(e);
            setStatus(e.message || "Lỗi tải dữ liệu.", true);
        }
    };

    const accept = async (friendId) => {
        try {
            const res = await fetch(`/api/social/friends/${friendId}/accept`, {
                method: "POST",
                headers: headers()
            });
            if (!res.ok) {
                const t = await res.text();
                throw new Error(t || "Chấp nhận thất bại.");
            }
            setStatus("Đã kết bạn.");
            await loadFriends();
        } catch (e) {
            setStatus(e.message || "Lỗi.", true);
        }
    };

    inviteForm?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const name = friendUsername?.value?.trim();
        if (!name) {
            setStatus("Nhập username.", true);
            return;
        }
        sendInviteBtn.disabled = true;
        setStatus("Đang gửi…");
        try {
            const res = await fetch("/api/social/friends/request", {
                method: "POST",
                headers: headers(true),
                body: JSON.stringify({ username: name })
            });
            if (!res.ok) {
                const t = await res.text();
                throw new Error(t || "Gửi thất bại.");
            }
            friendUsername.value = "";
            setStatus("Đã gửi lời mời.");
            await loadFriends();
        } catch (err) {
            setStatus(err.message || "Lỗi.", true);
        } finally {
            sendInviteBtn.disabled = false;
        }
    });

    loadFriends();
});
