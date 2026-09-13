(() => {
    const container = document.querySelector("#consent-items");
    const addButton = document.querySelector("#add-consent-item");
    const template = document.querySelector("#consent-item-template");
    if (!container || !addButton || !template) return;

    const reindex = () => {
        container.querySelectorAll("[data-consent-item]").forEach((item, index) => {
            item.querySelector("[data-item-number]").textContent = String(index + 1);
            item.querySelectorAll("[name]").forEach((field) => {
                field.name = field.name.replace(/items\[\d+\]/, `items[${index}]`);
                if (field.id) field.id = field.id.replace(/items\d+/, `items${index}`);
            });
        });
    };

    addButton.addEventListener("click", () => {
        const index = container.querySelectorAll("[data-consent-item]").length;
        const html = template.innerHTML.replaceAll("__INDEX__", String(index));
        container.insertAdjacentHTML("beforeend", html);
        reindex();
        container.dispatchEvent(new Event("change", { bubbles: true }));
    });

    container.addEventListener("click", (event) => {
        const remove = event.target.closest("[data-remove-item]");
        if (!remove) return;
        const items = container.querySelectorAll("[data-consent-item]");
        if (items.length <= 1) return;
        remove.closest("[data-consent-item]").remove();
        reindex();
        container.dispatchEvent(new Event("change", { bubbles: true }));
    });
})();
