document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".stat-card").forEach((card) => {
        card.addEventListener("mouseenter", () => {
            card.style.transform = "translateY(-2px)";
            card.style.transition = "transform 0.2s ease";
        });
        card.addEventListener("mouseleave", () => {
            card.style.transform = "translateY(0)";
        });
    });

    const accountId = localStorage.getItem("accountId");
    const redirectToLogin = () => {
        window.location.href = "/login";
    };

    if (!accountId) {
        redirectToLogin();
        return;
    }

    const profileAvatar = document.getElementById("profileAvatar");
    const profileUsername = document.getElementById("profileUsername");
    const profileCoins = document.getElementById("profileCoins");
    const profileTickets = document.getElementById("profileTickets");
    const statMatches = document.getElementById("statMatches");
    const statWinRate = document.getElementById("statWinRate");
    const statWonAssets = document.getElementById("statWonAssets");
    const statRankTier = document.getElementById("statRankTier");

    const equippedCharacterImage = document.getElementById("equippedCharacterImage");
    const equippedCharacterName = document.getElementById("equippedCharacterName");
    const currentHeroSelect = document.getElementById("currentHeroSelect");
    const currentHeroStatus = document.getElementById("currentHeroStatus");

    const editAvatarButton = document.getElementById("editAvatarButton");
    const avatarFileInput = document.getElementById("avatarFileInput");
    const avatarStatus = document.getElementById("avatarStatus");
    const logoutButton = document.getElementById("logoutButton");
    const changeNameButton = document.getElementById("changeNameButton");
    const renameHint = document.getElementById("renameHint");
    const renameModal = document.getElementById("renameModal");
    const renameInput = document.getElementById("renameInput");
    const renameModalError = document.getElementById("renameModalError");
    const renameConfirmButton = document.getElementById("renameConfirmButton");

    const RENAME_COST = 1000;
    let lastCoins = 0;

    const getHeaders = (json = false) => {
        const headers = {};
        headers["X-Account-Id"] = accountId;
        if (json) {
            headers["Content-Type"] = "application/json";
        }
        return headers;
    };

    const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const refreshProfileCurrencyIcons = () => {
        if (window.CoinSystem && typeof CoinSystem.initCurrencySlots === "function") {
            CoinSystem.initCurrencySlots([
                { elId: "profileCoinSilverSlot", type: "silver" },
                { elId: "profileCoinGoldSlot", type: "gold" }
            ]);
        }
    };

    const handleUnauthorized = (response) => {
        if (response.status === 401 || response.status === 403) {
            redirectToLogin();
            return true;
        }
        return false;
    };

    const loadSummary = async () => {
        try {
            const response = await fetch("/api/user/me/summary", { headers: getHeaders() });
            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai thong tin ho so.");
            }

            const data = await response.json();

            if (profileUsername) {
                profileUsername.textContent = data.username || "Nguoi choi";
            }
            if (profileCoins) {
                profileCoins.textContent = `${formatNumber(data.coins)} Bạc`;
            }
            lastCoins = data.coins != null ? Number(data.coins) : 0;
            if (changeNameButton) {
                changeNameButton.disabled = lastCoins < RENAME_COST;
            }
            if (renameHint) {
                renameHint.textContent =
                    lastCoins < RENAME_COST
                        ? `Cần ít nhất ${formatNumber(RENAME_COST)} bạc để đổi tên (bạn đang có ${formatNumber(lastCoins)} bạc).`
                        : "";
            }
            if (profileTickets) {
                profileTickets.textContent = `${formatNumber(data.tickets)} Vé`;
            }
            refreshProfileCurrencyIcons();
            if (statMatches) {
                statMatches.textContent = data.matches ?? 0;
            }
            if (statWinRate) {
                statWinRate.textContent = `${data.winRate ?? 0}%`;
            }
            if (statWonAssets) {
                statWonAssets.textContent = formatNumber(data.totalWonAssets);
            }
            if (statRankTier) {
                statRankTier.textContent = data.rankTier || "-";
            }

            if (equippedCharacterName) {
                equippedCharacterName.textContent = data.equippedCharacterName || "-";
            }
            if (equippedCharacterImage) {
                if (data.equippedCharacterImageUrl) {
                    equippedCharacterImage.style.display = "block";
                    equippedCharacterImage.src = data.equippedCharacterImageUrl;
                    // Clear alt text fallback; keep name in text below.
                    equippedCharacterImage.alt = data.equippedCharacterName || "Hình nhân vật";
                } else {
                    equippedCharacterImage.style.display = "none";
                    equippedCharacterImage.src = "";
                }
            }

            const fallbackAvatarText = () => {
                if (!profileAvatar || !data.username) return;
                profileAvatar.textContent = String(data.username).slice(0, 1).toUpperCase();
                // Keep CSS gradient by clearing inline background override.
                profileAvatar.style.background = "";
            };

            if (profileAvatar && data.avatarUrl) {
                const img = new Image();
                img.onload = () => {
                    profileAvatar.textContent = "";
                    profileAvatar.style.background = `url('${data.avatarUrl}') center/cover no-repeat`;
                };
                img.onerror = () => {
                    fallbackAvatarText();
                };
                img.src = data.avatarUrl;
            } else if (profileAvatar && data.username) {
                fallbackAvatarText();
            }

            await refreshCurrentHeroSelect(data);
        } catch (error) {
            console.error(error);
        }
    };

    const refreshCurrentHeroSelect = async (summaryData) => {
        if (!currentHeroSelect) return;
        try {
            const response = await fetch("/api/heroes/owned", { headers: getHeaders() });
            if (handleUnauthorized(response)) return;
            if (!response.ok) throw new Error("Không tải được danh sách hero.");
            const heroes = await response.json();
            const prev = currentHeroSelect.value;
            currentHeroSelect.innerHTML = "";
            const placeholder = document.createElement("option");
            placeholder.value = "";
            placeholder.textContent = heroes.length ? "— Chọn hero mặc định —" : "Chưa có hero — vào Cửa hàng";
            currentHeroSelect.appendChild(placeholder);
            (Array.isArray(heroes) ? heroes : []).forEach((h) => {
                const opt = document.createElement("option");
                opt.value = String(h.heroId ?? "");
                opt.textContent = h.name || `Hero ${h.heroId}`;
                currentHeroSelect.appendChild(opt);
            });
            const targetId =
                summaryData?.currentHeroId != null
                    ? String(summaryData.currentHeroId)
                    : summaryData?.equippedCharacterId != null
                      ? String(summaryData.equippedCharacterId)
                      : "";
            if (targetId && [...currentHeroSelect.options].some((o) => o.value === targetId)) {
                currentHeroSelect.value = targetId;
            } else if (prev && [...currentHeroSelect.options].some((o) => o.value === prev)) {
                currentHeroSelect.value = prev;
            }
        } catch (e) {
            console.error(e);
            if (currentHeroStatus) currentHeroStatus.textContent = e.message || "Không tải hero.";
        }
    };

    let currentHeroSaving = false;
    currentHeroSelect?.addEventListener("change", async () => {
        if (currentHeroSaving || !currentHeroSelect) return;
        const v = currentHeroSelect.value;
        if (!v) return;
        currentHeroSaving = true;
        if (currentHeroStatus) currentHeroStatus.textContent = "";
        try {
            const response = await fetch("/api/user/me/current-hero", {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({ heroId: Number(v) })
            });
            if (handleUnauthorized(response)) return;
            if (!response.ok) {
                const t = await response.text();
                throw new Error(t || "Không lưu được.");
            }
            const data = await response.json();
            if (currentHeroStatus) currentHeroStatus.textContent = "Đã lưu hero mặc định.";
            if (equippedCharacterName) {
                equippedCharacterName.textContent = data.equippedCharacterName || "-";
            }
            if (equippedCharacterImage && data.equippedCharacterImageUrl) {
                equippedCharacterImage.style.display = "block";
                equippedCharacterImage.src = data.equippedCharacterImageUrl;
                equippedCharacterImage.alt = data.equippedCharacterName || "";
            }
        } catch (e) {
            if (currentHeroStatus) currentHeroStatus.textContent = e.message || "Lỗi lưu hero.";
            await loadSummary();
        } finally {
            currentHeroSaving = false;
        }
    });

    const setAvatarStatus = (message, isError = false) => {
        if (!avatarStatus) return;
        avatarStatus.textContent = message || "";
        avatarStatus.style.color = isError ? "#ffd7d7" : "";
    };

    const uploadAvatar = async (file) => {
        if (!file) {
            setAvatarStatus("Chưa chọn ảnh.", true);
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            setAvatarStatus("Ảnh quá lớn... Giới hạn 5MB.", true);
            return;
        }

        try {
            setAvatarStatus("Dang cap nhat avatar...");
            const formData = new FormData();
            formData.append("avatar", file);

            const response = await fetch("/api/user/me/avatar", {
                method: "POST",
                headers: getHeaders(),
                body: formData
            });

            if (handleUnauthorized(response)) {
                return;
            }

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Cap nhat avatar that bai.");
            }

            setAvatarStatus("Đã cập nhật avatar!");
            await loadSummary();
        } catch (error) {
            console.error(error);
            setAvatarStatus(error.message || "Cập nhật avatar thất bại.", true);
        } finally {
            if (avatarFileInput) {
                avatarFileInput.value = "";
            }
        }
    };

    if (editAvatarButton && avatarFileInput) {
        editAvatarButton.addEventListener("click", () => {
            avatarFileInput.click();
        });

        avatarFileInput.addEventListener("change", (e) => {
            const file = e.target.files && e.target.files[0] ? e.target.files[0] : null;
            uploadAvatar(file);
        });
    }

    logoutButton?.addEventListener("click", () => {
        localStorage.removeItem("accountId");
        localStorage.removeItem("userRole");
        redirectToLogin();
    });

    const openRenameModal = () => {
        if (!renameModal) return;
        if (renameModalError) renameModalError.textContent = "";
        if (renameInput) renameInput.value = "";
        renameModal.hidden = false;
        renameModal.setAttribute("aria-hidden", "false");
        renameInput?.focus();
    };

    const closeRenameModal = () => {
        if (!renameModal) return;
        renameModal.hidden = true;
        renameModal.setAttribute("aria-hidden", "true");
        if (renameModalError) renameModalError.textContent = "";
    };

    changeNameButton?.addEventListener("click", () => {
        if (lastCoins < RENAME_COST) return;
        openRenameModal();
    });

    renameModal?.querySelectorAll("[data-close-rename]").forEach((el) => {
        el.addEventListener("click", closeRenameModal);
    });

    renameConfirmButton?.addEventListener("click", async () => {
        const name = renameInput ? renameInput.value.trim() : "";
        if (!name) {
            if (renameModalError) renameModalError.textContent = "Nhập tên mới.";
            return;
        }
        renameConfirmButton.disabled = true;
        if (renameModalError) renameModalError.textContent = "";
        try {
            const response = await fetch("/api/user/me/display-name", {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({ newUsername: name })
            });
            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                const errText = await response.text();
                throw new Error(errText || "Đổi tên thất bại.");
            }
            closeRenameModal();
            await loadSummary();
        } catch (e) {
            if (renameModalError) {
                renameModalError.textContent = e.message || "Lỗi đổi tên.";
            }
        } finally {
            renameConfirmButton.disabled = false;
        }
    });

    loadSummary();
});
