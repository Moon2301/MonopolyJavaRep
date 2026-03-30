document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".toolbox-panel button").forEach((button) => {
        button.addEventListener("click", () => button.blur());
    });
});
