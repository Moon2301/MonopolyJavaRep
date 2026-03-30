document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".switch input").forEach((input) => {
        input.addEventListener("change", () => {
            input.blur();
        });
    });
});
