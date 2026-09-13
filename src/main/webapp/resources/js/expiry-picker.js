(function (root, factory) {
    const api = factory();
    if (typeof module === "object" && module.exports) module.exports = api;
    if (root && root.document) api.initialize(root.document, root.Date);
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
    "use strict";

    function pad(value) {
        return String(value).padStart(2, "0");
    }

    function combineExpiry(date, hour, minute) {
        if (!date || hour === "" || minute === "") return "";
        return `${date}T${pad(hour)}:${pad(minute)}`;
    }

    function expiryAfterDays(now, days) {
        const selected = new Date(now.getTime());
        selected.setDate(selected.getDate() + days);
        return {
            date: `${selected.getFullYear()}-${pad(selected.getMonth() + 1)}-${pad(selected.getDate())}`,
            hour: "18",
            minute: "00"
        };
    }

    function initialize(document, DateConstructor) {
        const hidden = document.getElementById("expiresAt");
        const date = document.getElementById("expiryDate");
        const hour = document.getElementById("expiryHour");
        const minute = document.getElementById("expiryMinute");
        if (!hidden || !date || !hour || !minute) return;

        const current = hidden.value ? hidden.value.slice(0, 16) : "";
        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(current)) {
            date.value = current.slice(0, 10);
            hour.value = String(Number(current.slice(11, 13)));
            minute.value = current.slice(14, 16);
        }

        const sync = () => {
            hidden.value = combineExpiry(date.value, hour.value, minute.value);
        };
        [date, hour, minute].forEach(control => control.addEventListener("change", sync));

        document.querySelectorAll("[data-expiry-days]").forEach(button => {
            button.addEventListener("click", () => {
                const selected = expiryAfterDays(new DateConstructor(), Number(button.dataset.expiryDays));
                date.value = selected.date;
                hour.value = String(Number(selected.hour));
                minute.value = selected.minute;
                sync();
                date.focus();
            });
        });

        hidden.form?.addEventListener("submit", sync);
    }

    return { combineExpiry, expiryAfterDays, initialize };
});