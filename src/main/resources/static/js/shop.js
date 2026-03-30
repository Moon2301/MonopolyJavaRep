document.addEventListener("DOMContentLoaded", async () => {
    const shopGrid = document.querySelector(".shop-grid");
    const loading = document.getElementById("shop-loading");
    const coinsLabel = document.getElementById("shopCoinsLabel");
    const backButton = document.getElementById("shopBackButton");
    const gradients = ["hero-a", "hero-b", "hero-c", "hero-d", "hero-e", "hero-f", "hero-g"];
    const accountId = localStorage.getItem("accountId");
    const ownedHeroIds = new Set();
    let currentCoins = 0;

    const formatPrice = (price) => `${new Intl.NumberFormat("vi-VN").format(price || 0)} Bạc`;
    const formatCoins = (coins) => `Bạc: ${new Intl.NumberFormat("vi-VN").format(coins || 0)}`;

    const refreshShopCoinIcon = () => {
        if (window.CoinSystem && typeof CoinSystem.initCurrencySlots === "function") {
            CoinSystem.initCurrencySlots([{ elId: "shopCoinSilverSlot", type: "silver", size: "md" }]);
        }
    };

    backButton?.addEventListener("click", () => {
        if (window.history.length > 1) {
            window.history.back();
            return;
        }
        window.location.href = "/home";
    });

    const loadShopState = async () => {
        if (!accountId) {
            if (coinsLabel) {
                coinsLabel.textContent = "Bạc: vui lòng đăng nhập";
            }
            return;
        }
        const stateResponse = await fetch("/api/user/shop/state", {
            headers: { "X-Account-Id": accountId }
        });
        if (!stateResponse.ok) {
            throw new Error("Không thể tải trạng thái shop");
        }
        const state = await stateResponse.json();
        currentCoins = state.coins || 0;
        (state.ownedHeroIds || []).forEach((id) => ownedHeroIds.add(id));
        if (coinsLabel) {
            coinsLabel.textContent = formatCoins(currentCoins);
        }
        refreshShopCoinIcon();
    };

    const purchaseHero = async (heroId) => {
        const response = await fetch("/api/user/shop/purchase", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Account-Id": accountId
            },
            body: JSON.stringify({ heroId })
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || "Mua thất bại");
        }
        return data;
    };

    try {
        await loadShopState();
        if (window.HeroSystem && typeof window.HeroSystem.preloadCharacterSvgs === "function") {
            await window.HeroSystem.preloadCharacterSvgs();
        }

        const response = await fetch("/api/heroes", {
            headers: accountId ? { "X-Account-Id": accountId } : {}
        });
        if (!response.ok) {
            throw new Error("Không thể tải danh sách hero");
        }

        const heroes = await response.json();
        shopGrid.innerHTML = "";

        heroes.forEach((hero, index) => {
            const card = document.createElement("article");
            card.className = "shop-card";

            const gradientClass = gradients[index % gradients.length];
            const isOwned = ownedHeroIds.has(hero.heroId);
            const isDefaultHero = hero.defaultUnlocked === true;
            const skillPrefix = hero.skillCooldown && hero.skillCooldown > 0
                ? `<span class="cd-badge">CD ${hero.skillCooldown}</span>`
                : `<span class="cd-badge">Passive</span>`;
            const buttonLabel = isOwned ? (isDefaultHero ? "Mặc định" : "Đã sở hữu") : "Mua";
            const uid = `sh${hero.heroId}-${index}`;

            card.innerHTML = `
                <div class="shop-card__media ${gradientClass}">
                    <div class="shop-card__svg-wrap"></div>
                </div>
                <h2>${hero.name || "Chưa đặt tên"}</h2>
                <p class="price">${formatPrice(hero.price)}</p>
                <p class="hero-look">${hero.appearanceDescription || "Đang cập nhật ngoại hình."}</p>
                <p class="hero-skill">${skillPrefix}${hero.skillDescription || "Đang cập nhật skill."}</p>
                <button type="button" ${isOwned ? "disabled" : ""}>${buttonLabel}</button>
            `;

            const svgWrap = card.querySelector(".shop-card__svg-wrap");
            if (svgWrap && window.HeroSystem && typeof window.HeroSystem.getSVG === "function") {
                svgWrap.innerHTML = window.HeroSystem.getSVG(hero.name, uid);
                window.requestAnimationFrame(() => {
                    if (typeof window.HeroSystem.startIdle === "function") {
                        window.HeroSystem.startIdle(uid, index * 0.07);
                    }
                });
            }

            const button = card.querySelector("button");
            button.addEventListener("click", async () => {
                button.blur();
                if (!accountId) {
                    alert("Vui lòng đăng nhập để mua nhân vật.");
                    return;
                }
                if (ownedHeroIds.has(hero.heroId)) {
                    return;
                }

                button.disabled = true;
                button.textContent = "Đang mua...";
                try {
                    const result = await purchaseHero(hero.heroId);
                    if (result.purchased) {
                        ownedHeroIds.add(hero.heroId);
                        currentCoins = result.remainingCoins || currentCoins;
                        if (coinsLabel) {
                            coinsLabel.textContent = formatCoins(currentCoins);
                        }
                        refreshShopCoinIcon();
                        button.textContent = "Đã sở hữu";
                    } else {
                        button.disabled = false;
                        button.textContent = "Mua";
                        alert(result.message || "Không thể mua nhân vật");
                    }
                } catch (error) {
                    button.disabled = false;
                    button.textContent = "Mua";
                    alert(error.message || "Mua thất bại");
                }
            });
            shopGrid.appendChild(card);
        });

        if (!heroes.length) {
            shopGrid.innerHTML = `<p class="shop-state">Chưa có nhân vật nào trong cửa hàng.</p>`;
        }
    } catch (error) {
        if (loading) {
            loading.textContent = "Tải danh sách thất bại. Vui lòng thử lại sau.";
        } else {
            shopGrid.innerHTML = `<p class="shop-state">Tải danh sách thất bại. Vui lòng thử lại sau.</p>`;
        }
    }
});
