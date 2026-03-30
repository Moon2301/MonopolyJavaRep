document.addEventListener("DOMContentLoaded", () => {
    const urlParams = new URLSearchParams(window.location.search);
    const success = urlParams.get('success');

    const resultCard = document.getElementById("resultCard");
    const iconContainer = document.getElementById("iconContainer");
    const resultTitle = document.getElementById("resultTitle");
    const resultMessage = document.getElementById("resultMessage");
    const returnHomeBtn = document.getElementById("returnHomeBtn");

    resultCard.style.display = "block";

    if (success === 'true') {
        iconContainer.innerHTML = "🎉";
        resultTitle.textContent = "Thanh toán thành công!";
        resultTitle.style.background = "linear-gradient(135deg, #4caf50, #81c784)";
        resultTitle.style.webkitBackgroundClip = "text";
        resultMessage.textContent = "Cảm ơn bạn đã nạp xu. Số dư của bạn đã được cập nhật.";
    } else {
        iconContainer.innerHTML = "❌";
        resultTitle.textContent = "Giao dịch thất bại";
        resultTitle.style.background = "linear-gradient(135deg, #f44336, #e57373)";
        resultTitle.style.webkitBackgroundClip = "text";
        resultMessage.textContent = "Đã có lỗi xảy ra hoặc bạn đã hủy giao dịch.";
        resultCard.style.borderColor = "#f44336";
    }

    returnHomeBtn.addEventListener("click", () => {
        window.location.href = "/home";
    });
});