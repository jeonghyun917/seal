(() => {
    "use strict";
    document.querySelectorAll("[data-request-picker]").forEach(picker => {
        const search = picker.querySelector('input[type="search"]');
        const select = picker.querySelector('select[name="consentRequestId"]');
        const status = picker.querySelector("[data-request-status]");
        const previous = picker.querySelector("[data-request-prev]");
        const next = picker.querySelector("[data-request-next]");
        let currentPage = 1;
        let hasNext = false;
        let pending;
        let generation = 0;
        previous.disabled = true;
        next.disabled = true;
        async function load(page) {
            pending?.abort();
            pending = new AbortController();
            const current = ++generation;
            previous.disabled = next.disabled = true;
            status.textContent = "동의 요청을 조회하고 있습니다.";
            try {
                const url = new URL(picker.dataset.optionsUrl, window.location.href);
                url.searchParams.set("q", search.value.trim());
                url.searchParams.set("page", String(page));
                const response = await fetch(url, { signal: pending.signal, headers: { Accept: "application/json" } });
                if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) {
                    throw new Error("조회할 수 없습니다. 로그인 상태와 조회 권한을 확인해 주세요.");
                }
                const result = await response.json();
                if (current !== generation) return;
                const selected = select.value ? select.selectedOptions[0].cloneNode(true) : null;
                select.replaceChildren(new Option("동의 요청을 선택해 주세요", ""));
                result.items.forEach(item => select.add(new Option(
                    `${item.requestCode} · ${item.recipientLabel} · ${item.templateName}`, item.requestId)));
                if (selected && !result.items.some(item => item.requestId === selected.value)) select.add(selected);
                select.value = selected?.value || "";
                currentPage = result.page;
                hasNext = result.hasNext;
                status.textContent = `${currentPage}페이지 · 검색 결과 ${result.items.length}건${selected ? " · 기존 선택 유지" : ""}`;
                if (!result.items.length) status.textContent = "조건에 맞는 동의 요청이 없습니다. 검색어를 바꿔 주세요.";
            } catch (error) {
                if (error.name !== "AbortError") status.textContent = error.message;
            } finally {
                if (current === generation) {
                    previous.disabled = currentPage <= 1;
                    next.disabled = !hasNext;
                }
            }
        }
        picker.querySelector("[data-request-search]").addEventListener("click", () => load(1));
        search.addEventListener("keydown", event => {
            if (event.key === "Enter") { event.preventDefault(); load(1); }
        });
        previous.addEventListener("click", () => load(currentPage - 1));
        next.addEventListener("click", () => load(currentPage + 1));
    });
})();
