document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".match-card").forEach((card, index) => {
        card.style.transition = "transform 0.22s ease";
        card.addEventListener("mouseenter", () => {
            card.style.transform = "translateY(-2px)";
        });
        card.addEventListener("mouseleave", () => {
            card.style.transform = "translateY(0)";
        });
        card.style.animationDelay = `${index * 80}ms`;
    });
});
