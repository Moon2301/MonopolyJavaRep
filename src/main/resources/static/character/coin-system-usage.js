/**
 * coin-system-usage.js — Ví dụ tích hợp vào project
 * Không copy file này vào project, chỉ dùng làm tham khảo
 */

// ══════════════════════════════════════════════════════════════════
// SETUP: Thêm vào mỗi trang cần dùng
// ══════════════════════════════════════════════════════════════════

/*  Trong HTML (trước </body>):
    <canvas id="coinCanvas" style="position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:100;"></canvas>
    <script src="/js/coin-system.js"></script>
*/


// ══════════════════════════════════════════════════════════════════
// 1. HIỆU ỨNG NHẬN TIỀN KHI QUA Ô GO
//    Xu bay từ ô GO đến thẻ player
// ══════════════════════════════════════════════════════════════════

function onPassGo(playerCardEl) {
    const canvas = document.getElementById('coinCanvas');
    const goCell = document.querySelector('[data-cell-index="0"]');

    CoinSystem.flow(canvas, goCell, playerCardEl, {
        type:       'gold',
        count:      10,
        arcHeight:  -100,
        stagger:    45,
        onDone: () => {
            CoinSystem.floatText(playerCardEl, '+200G', '#D4A017');
        },
    });
}


// ══════════════════════════════════════════════════════════════════
// 2. HIỆU ỨNG TRẢ TIỀN THUÊ
//    Xu bay từ player này đến player kia
// ══════════════════════════════════════════════════════════════════

function onPayRent(fromPlayerEl, toPlayerEl, amount) {
    const canvas = document.getElementById('coinCanvas');
    const count  = Math.min(Math.max(3, Math.floor(amount / 50)), 20);

    CoinSystem.flow(canvas, fromPlayerEl, toPlayerEl, {
        type:      amount >= 300 ? 'gold' : amount >= 100 ? 'silver' : 'bronze',
        count,
        arcHeight: -70,
        speed:     0.025,
        onDone: () => {
            CoinSystem.floatText(toPlayerEl, `+${amount}G`, '#D4A017');
            CoinSystem.floatText(fromPlayerEl, `-${amount}G`, '#A32D2D');
        },
    });
}


// ══════════════════════════════════════════════════════════════════
// 3. HIỆU ỨNG RÚT BÀI (xu bùng phát)
//    Khi rút được thẻ "Tiền Trời Cho"
// ══════════════════════════════════════════════════════════════════

function onDrawMoneyCard(cardEl, amount) {
    const canvas = document.getElementById('coinCanvas');

    CoinSystem.burst(canvas, cardEl, 'gold', 14, {
        speed:   8,
        gravity: 0.25,
    });

    setTimeout(() => {
        CoinSystem.floatText(cardEl, `+${amount}G`, '#D4A017');
    }, 300);
}


// ══════════════════════════════════════════════════════════════════
// 4. RENDER ĐỒNG XU VÀO DOM (private-table, shop)
//    Hiển thị xu trang trí với animation spin
// ══════════════════════════════════════════════════════════════════

function renderCoinDisplay() {
    const goldSlot   = document.getElementById('coinGold');
    const silverSlot = document.getElementById('coinSilver');
    const bronzeSlot = document.getElementById('coinBronze');

    if (goldSlot) {
        CoinSystem.renderCoin(goldSlot, 'gold', 'lg', {
            spin: true,
            onClick: () => CoinSystem.burst(document.getElementById('coinCanvas'), goldSlot, 'gold', 12),
        });
    }
    if (silverSlot) CoinSystem.renderCoin(silverSlot, 'silver', 'md', { spin: true });
    if (bronzeSlot) CoinSystem.renderCoin(bronzeSlot, 'bronze', 'md', { spin: true });
}


// ══════════════════════════════════════════════════════════════════
// 5. CHỒNG XU TRONG BẢNG HIỂN THỊ TÀI SẢN
// ══════════════════════════════════════════════════════════════════

function renderPropertyWealth(el, goldCount, silverCount, bronzeCount) {
    CoinSystem.renderPile(el, {
        layers: [
            { type: 'bronze', count: bronzeCount },
            { type: 'silver', count: silverCount },
            { type: 'gold',   count: goldCount   },
        ],
        width: 72,
        bob:   true,
    });
}


// ══════════════════════════════════════════════════════════════════
// 6. TÍCH HỢP VỚI game-board.js — thay thế animateDice callback
// ══════════════════════════════════════════════════════════════════

/*
// Trong animateDice() sau khi di chuyển xong:
window.setTimeout(() => {
    window.clearInterval(rolling);
    // ... code cũ lắc xúc xắc ...

    // Thêm hiệu ứng xu khi qua GO
    if (newPosition < oldPosition) {
        const goCell   = boardElement.querySelector('[data-cell-index="0"]');
        const playerCard = document.getElementById(`playerCard${activePlayerIndex + 1}`);
        onPassGo(playerCard);
    }
}, 1100);
*/


// ══════════════════════════════════════════════════════════════════
// 7. HIỆU ỨNG DÒNG VÀNG THEO XÚC XẮC (skill Tăng tiền nhận)
//    Hero Phú Lộc Thọ kích hoạt: xu bay nhiều hơn + to hơn
// ══════════════════════════════════════════════════════════════════

function onSkillIncomeBonus(fromEl, toEl, bonusAmount) {
    const canvas = document.getElementById('coinCanvas');

    CoinSystem.flow(canvas, fromEl, toEl, {
        type:      'gold',
        count:     16,
        radius:    12,
        arcHeight: -120,
        speed:     0.018,
        spread:    50,
        stagger:   35,
        onDone: () => {
            CoinSystem.burst(canvas, toEl, 'gold', 8);
            CoinSystem.floatText(toEl, `+${bonusAmount}G (Bonus!)`, '#D4A017');
        },
    });
}