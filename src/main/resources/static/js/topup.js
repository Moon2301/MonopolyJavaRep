document.addEventListener("DOMContentLoaded", () => {
    const accountId = localStorage.getItem("accountId") || sessionStorage.getItem("accountId");
    if (!accountId) {
        alert("Vui lòng đăng nhập trước khi nạp thẻ.");
        window.location.href = "/login";
        return;
    }

    const backButton = document.getElementById("backButton");
    backButton?.addEventListener("click", () => {
        window.location.href = "/home";
    });

    const cards = document.querySelectorAll(".topup-card");
    cards.forEach(card => {
        card.addEventListener("click", async () => {
            const amount = card.getAttribute("data-amount");
            if (!amount || isNaN(amount)) return;

            card.style.opacity = "0.7";
            card.style.pointerEvents = "none";

            try {
                const response = await fetch(`/api/payment/vnpay/create-url?amount=${amount}`, {
                    method: "POST",
                    headers: {
                        "X-Account-Id": accountId
                    }
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.url) {
                        window.location.href = data.url;
                    } else {
                        alert("Không thể khởi tạo cổng thanh toán.");
                    }
                } else {
                    alert("Đã xảy ra lỗi kết nối với máy chủ.");
                }
            } catch (err) {
                console.error(err);
                alert("Lỗi kết nối.");
            } finally {
                card.style.opacity = "1";
                card.style.pointerEvents = "auto";
            }
        });
    });
});