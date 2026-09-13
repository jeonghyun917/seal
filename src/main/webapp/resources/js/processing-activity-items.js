(() => {
    "use strict";

    document.addEventListener("DOMContentLoaded", () => {
        const list = document.querySelector("[data-data-item-list]");
        const addButton = document.querySelector("[data-data-item-add]");
        const count = document.querySelector("[data-data-item-count]");
        if (!list || !addButton) return;

        const maximum = Number(list.dataset.maxItems || 50);

        function createRow(index) {
            const row = document.createElement("div");
            row.className = "data-item-row";
            row.dataset.dataItemRow = "";
            row.innerHTML = `
                <label>항목명
                    <input id="data-item-${index}-name" name="items[${index}].itemName" type="text" maxlength="100" placeholder="항목명">
                </label>
                <label>이 항목이 필요한 이유
                    <input id="data-item-${index}-reason" name="items[${index}].necessityReason" type="text" maxlength="500" placeholder="이 항목이 필요한 이유">
                </label>
                <label class="check-label">
                    <input name="_items[${index}].required" type="hidden" value="on">
                    <input id="data-item-${index}-required" name="items[${index}].required" type="checkbox" value="true"> 필수
                </label>
                <button class="secondary-button data-item-remove" type="button" data-data-item-remove>삭제</button>`;
            return row;
        }

        function renumberRows() {
            Array.from(list.querySelectorAll("[data-data-item-row]")).forEach((row, index) => {
                row.querySelectorAll("input").forEach(input => {
                    input.name = input.name.replace(/items\[\d+]/, `items[${index}]`);
                    if (input.id) input.id = input.id.replace(/data-item-\d+-/, `data-item-${index}-`);
                });
            });
        }

        function updateControls() {
            const rows = list.querySelectorAll("[data-data-item-row]");
            if (count) count.textContent = String(rows.length);
            addButton.disabled = rows.length >= maximum;
            rows.forEach(row => {
                const removeButton = row.querySelector("[data-data-item-remove]");
                if (removeButton) removeButton.disabled = rows.length === 1;
            });
        }

        addButton.addEventListener("click", () => {
            const rows = list.querySelectorAll("[data-data-item-row]");
            if (rows.length >= maximum) return;
            const row = createRow(rows.length);
            list.append(row);
            row.querySelector("input[type='text']")?.focus();
            updateControls();
        });

        list.addEventListener("click", event => {
            const removeButton = event.target.closest("[data-data-item-remove]");
            if (!removeButton) return;
            const rows = list.querySelectorAll("[data-data-item-row]");
            if (rows.length === 1) return;
            removeButton.closest("[data-data-item-row]")?.remove();
            renumberRows();
            updateControls();
        });

        updateControls();
    });
})();
