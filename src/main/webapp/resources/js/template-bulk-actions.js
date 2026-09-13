(function () {
    "use strict";

    function initializeBulkForm(form) {
        var selectAll = form.querySelector("[data-select-all]");
        var rows = Array.prototype.slice.call(form.querySelectorAll("[data-row-select]"));
        var submit = form.querySelector("[data-bulk-submit]");
        var trigger = form.querySelector("[data-bulk-trigger]");
        var counts = Array.prototype.slice.call(form.querySelectorAll("[data-selection-count]"));
        var dialog = form.querySelector("[data-delete-dialog]");

        if (!submit || rows.length === 0) {
            return;
        }

        function updateState() {
            var selected = rows.filter(function (checkbox) { return checkbox.checked; });
            submit.disabled = selected.length === 0;
            if (trigger) {
                trigger.disabled = selected.length === 0;
            }
            counts.forEach(function (count) {
                count.textContent = selected.length + "개 선택";
            });
            if (selectAll) {
                selectAll.checked = selected.length === rows.length;
                selectAll.indeterminate = selected.length > 0 && selected.length < rows.length;
            }
        }

        if (selectAll) {
            selectAll.addEventListener("change", function () {
                rows.forEach(function (checkbox) { checkbox.checked = selectAll.checked; });
                updateState();
            });
        }
        rows.forEach(function (checkbox) {
            checkbox.addEventListener("change", updateState);
        });

        if (trigger && dialog) {
            trigger.addEventListener("click", function () {
                dialog.showModal();
                var reason = dialog.querySelector("[name='deleteReason']");
                if (reason) {
                    reason.focus();
                }
            });
            dialog.querySelectorAll("[data-dialog-close]").forEach(function (button) {
                button.addEventListener("click", function () { dialog.close(); });
            });
            dialog.addEventListener("click", function (event) {
                if (event.target === dialog) {
                    dialog.close();
                }
            });
        }

        form.addEventListener("submit", function (event) {
            if (submit.disabled) {
                event.preventDefault();
                return;
            }
            var message = form.getAttribute("data-confirm-message");
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
        updateState();
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-bulk-form]").forEach(initializeBulkForm);
    });
}());