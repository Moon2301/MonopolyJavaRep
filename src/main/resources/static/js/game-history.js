document.addEventListener("DOMContentLoaded", () => {
    const accountId = localStorage.getItem("accountId");
    const redirectToLogin = () => {
        window.location.href = "/login";
    };

    if (!accountId) {
        redirectToLogin();
        return;
    }

    const historyHeader = document.getElementById("historyHeader");
    const historyList = document.getElementById("historyList");
    const historyEmptyState = document.getElementById("historyEmptyState");

    const getHeaders = () => {
        return {
            "X-Account-Id": accountId
        };
    };

    const handleUnauthorized = (response) => {
        if (response.status === 401 || response.status === 403) {
            redirectToLogin();
            return true;
        }
        return false;
    };

    const renderCard = (item) => {
        const result = item.result || "DRAW";
        const resultClass = result === "WIN" ? "win" : result === "LOSE" ? "lose" : "draw";
        const mapId = item.mapId ?? "-";
        const startedAt = item.startedAtFormatted || item.startedAt || "-";
        const endedAt = item.endedAtFormatted || item.endedAt || "-";
        const yourCharacterId = item.yourCharacterId ?? null;

        return `
            <div class="history-card">
                <div class="history-card-top">
                    <span class="result-pill ${resultClass}">${result}</span>
                    <span class="map-pill">Map #${mapId}</span>
                </div>

                <div class="history-card-row">
                    <span>Bắt đầu</span>
                    <strong>${startedAt}</strong>
                </div>
                <div class="history-card-row">
                    <span>Kết thúc</span>
                    <strong>${endedAt}</strong>
                </div>

                ${
                    yourCharacterId === null
                        ? ""
                        : `
                            <div class="history-card-row">
                                <span>Nhân vật</span>
                                <strong>${yourCharacterId}</strong>
                            </div>
                        `
                }
            </div>
        `;
    };

    const loadHistory = async () => {
        try {
            const response = await fetch("/api/user/games/history", { headers: getHeaders() });
            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai lich su tran dau.");
            }

            const items = await response.json();

            if (historyHeader) {
                historyHeader.textContent = "Lịch sử trận đấu";
            }

            if (!Array.isArray(items) || items.length === 0) {
                if (historyEmptyState) {
                    historyEmptyState.style.display = "block";
                }
                if (historyList) {
                    historyList.innerHTML = "";
                }
                return;
            }

            if (historyEmptyState) {
                historyEmptyState.style.display = "none";
            }

            historyList.innerHTML = items.map(renderCard).join("");
        } catch (error) {
            console.error(error);
        }
    };

    loadHistory();
});

