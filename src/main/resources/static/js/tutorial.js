document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".rule-card").forEach((card, index) => {
        card.style.transform = "translateY(14px)";
        card.style.opacity = "0";
        requestAnimationFrame(() => {
            setTimeout(() => {
                card.style.transition = "transform 0.28s ease, opacity 0.28s ease";
                card.style.transform = "translateY(0)";
                card.style.opacity = "1";
            }, index * 70);
        });
    });
});
