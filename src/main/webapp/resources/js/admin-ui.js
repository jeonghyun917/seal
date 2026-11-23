(() => {
    "use strict";

    function initializeMainContent() {
        const main = document.querySelector("main.admin-main");
        if (!main) return;
        main.id = main.id || "admin-main";
        main.tabIndex = -1;
        document.querySelector(".skip-link")?.addEventListener("click", () => {
            window.setTimeout(() => main.focus({ preventScroll: true }), 0);
        });
    }

    function initializeNavigation() {
        const button = document.querySelector(".admin-menu-button");
        const nav = document.getElementById("adminPrimaryNav");
        if (!button || !nav) return;
        const desktop = window.matchMedia("(min-width: 901px)");

        nav.querySelector("a.active")?.setAttribute("aria-current", "page");

        const close = (restoreFocus = false) => {
            document.body.classList.remove("admin-nav-open");
            button.setAttribute("aria-expanded", "false");
            nav.inert = !desktop.matches;
            if (restoreFocus) button.focus();
        };
        const open = () => {
            nav.inert = false;
            document.body.classList.add("admin-nav-open");
            button.setAttribute("aria-expanded", "true");
            (nav.querySelector("a.active") || nav.querySelector("a"))?.focus();
        };

        button.addEventListener("click", () => {
            if (document.body.classList.contains("admin-nav-open")) close(true);
            else open();
        });
        nav.addEventListener("click", event => {
            if (event.target.closest("a")) close();
            if (event.target.closest(".admin-nav-close")) close(true);
        });
        document.addEventListener("click", event => {
            if (!document.body.classList.contains("admin-nav-open")) return;
            if (!nav.contains(event.target) && !button.contains(event.target)) close(true);
        });
        document.addEventListener("keydown", event => {
            if (!document.body.classList.contains("admin-nav-open")) return;
            if (event.key === "Escape") {
                close(true);
                return;
            }
            if (event.key !== "Tab") return;
            const focusable = Array.from(nav.querySelectorAll('a[href], button:not([disabled])'));
            if (!focusable.length) return;
            const first = focusable[0];
            const last = focusable[focusable.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        });
        desktop.addEventListener("change", () => close(nav.contains(document.activeElement)));
        close();
    }

    function appendDescription(control, id) {
        const current = (control.getAttribute("aria-describedby") || "").split(/\s+/).filter(Boolean);
        if (!current.includes(id)) current.push(id);
        control.setAttribute("aria-describedby", current.join(" "));
        control.setAttribute("aria-invalid", "true");
    }

    function initializeFormErrors() {
        const errors = Array.from(document.querySelectorAll(".field-error"))
            .filter(error => error.textContent.trim());
        let firstInvalidControl;

        errors.forEach((error, index) => {
            error.id = error.id || `field-error-${index + 1}`;
            error.setAttribute("role", "alert");
            const group = error.closest(".form-field, label, fieldset, .consent-item-card") || error.parentElement;
            const controls = Array.from(group?.querySelectorAll("input:not([type=hidden]), select, textarea") || [])
                .map(control => document.getElementById(control.dataset.editorTarget) || control)
                .filter(control => !control.hidden);
            controls.forEach(control => appendDescription(control, error.id));
            firstInvalidControl ||= controls[0];
        });

        if (firstInvalidControl) {
            window.setTimeout(() => firstInvalidControl.focus({ preventScroll: true }), 0);
        }
    }

    function initializeForms() {
        document.querySelectorAll('input[type="email"], input[type="search"], input[name$="Code"], input[name$="Id"]')
            .forEach(input => input.spellcheck = false);

        document.querySelectorAll('form.list-filter').forEach(form => {
            if ((form.getAttribute("method") || "get").toLowerCase() !== "get") return;
            const current = new URL(window.location.href);
            if (![...current.searchParams.keys()].some(key => key !== "page" && current.searchParams.get(key))) return;
            const reset = document.createElement("a");
            reset.className = "text-button";
            reset.textContent = "검색 초기화";
            reset.href = window.location.pathname;
            form.append(reset);
        });

        window.addEventListener("pageshow", () => {
            document.querySelectorAll("form.is-submitting").forEach(form => {
                form.classList.remove("is-submitting");
                form.removeAttribute("aria-busy");
                form.querySelectorAll('button[aria-disabled="true"]').forEach(button => {
                    button.removeAttribute("aria-disabled");
                    if (button.dataset.originalLabel !== undefined) {
                        button.textContent = button.dataset.originalLabel;
                        delete button.dataset.originalLabel;
                    }
                });
            });
        });
        let approvedForm = null;
        document.addEventListener("submit", event => {
            if (event.defaultPrevented || !event.target.matches("form")) return;
            const form = event.target;
            // Downloads keep the current page open; do not leave its filters locked.
            if (event.submitter?.hasAttribute("data-download")) return;
            if (form.classList.contains("is-submitting")) {
                event.preventDefault();
                return;
            }
            if (form.dataset.confirmMessage && approvedForm !== form) {
                event.preventDefault();
                const submitter = event.submitter;
                confirmAction(form.dataset.confirmTitle || "작업 확인", form.dataset.confirmMessage, () => {
                    approvedForm = form;
                    try { form.requestSubmit(submitter || undefined); }
                    finally { approvedForm = null; }
                });
                return;
            }
            form.classList.add("is-submitting");
            form.setAttribute("aria-busy", "true");
            const submitter = event.submitter;
            if (submitter && !submitter.dataset.loadingLabel) {
                submitter.dataset.originalLabel = submitter.textContent;
                submitter.textContent = "처리 중…";
            }
            form.querySelectorAll('button[type="submit"], button:not([type])').forEach(button => {
                button.setAttribute("aria-disabled", "true");
            });
        });

        const dirtyForms = new Set();
        const markDirty = event => {
            const form = event.target.closest("form[data-unsaved-warning], form.template-editor, form.data-form[method='post'], form.stack-form[method='post']");
            if (form?.querySelector('input[autocomplete="current-password"]')) return;
            if (form) dirtyForms.add(form);
        };
        document.addEventListener("input", markDirty);
        document.addEventListener("change", markDirty);
        document.addEventListener("recipient-picker:applied", markDirty, true);
        window.addEventListener("beforeunload", event => {
            if ([...dirtyForms].some(form => !form.classList.contains("is-submitting"))) {
                event.preventDefault();
                event.returnValue = "";
            }
        });
    }

    function confirmAction(title, message, accept) {
        if (document.querySelector("dialog.admin-confirm[open]")) return;
        const returnFocus = document.activeElement;
        const dialog = document.createElement("dialog");
        dialog.className = "admin-confirm";
        dialog.setAttribute("aria-labelledby", "admin-confirm-title");
        dialog.setAttribute("aria-describedby", "admin-confirm-message");
        const heading = document.createElement("h2");
        heading.id = "admin-confirm-title";
        heading.textContent = title;
        const description = document.createElement("p");
        description.id = "admin-confirm-message";
        description.textContent = message;
        const actions = document.createElement("div");
        actions.className = "form-actions";
        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.className = "secondary-button";
        cancel.textContent = "취소";
        const confirm = document.createElement("button");
        confirm.type = "button";
        confirm.className = "danger-button";
        confirm.textContent = "확인 후 실행";
        cancel.addEventListener("click", () => dialog.close());
        confirm.addEventListener("click", () => { dialog.close(); accept(); });
        dialog.addEventListener("close", () => {
            dialog.remove();
            if (returnFocus?.isConnected) returnFocus.focus();
        }, { once: true });
        actions.append(cancel, confirm);
        dialog.append(heading, description, actions);
        document.body.append(dialog);
        dialog.showModal();
        cancel.focus();
    }

    function initializeDialogs() {
        let returnFocus = null;
        document.addEventListener("click", event => {
            const trigger = event.target.closest("#openRecipientPicker, #openActivityPicker, [data-dialog-open]");
            if (trigger) returnFocus = trigger;
        }, true);

        document.querySelectorAll("dialog").forEach((dialog, index) => {
            const heading = dialog.querySelector("h1, h2, h3");
            if (heading) {
                heading.id = heading.id || `dialog-title-${index + 1}`;
                dialog.setAttribute("aria-labelledby", heading.id);
            }
            dialog.setAttribute("aria-modal", "true");
            dialog.addEventListener("close", () => {
                if (returnFocus?.isConnected) returnFocus.focus();
                returnFocus = null;
            });
            dialog.addEventListener("click", event => {
                if (event.target === dialog) dialog.close();
            });
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        initializeMainContent();
        initializeNavigation();
        initializeFormErrors();
        initializeForms();
        initializeDialogs();
    });
})();
