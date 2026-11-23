(function () {
    "use strict";
    function text(value) { return value || ""; }
    function optionTitle(item) { return text(item.maskedName) + " · " + text(item.maskedEmail); }
    function optionMeta(item) { return [item.affiliationName || "소속기관 미입력", item.jobTitle].filter(Boolean).join(" · "); }
    function optionBasis(item) { return item.collectionBasis ? "등록 근거 · " + item.collectionBasis : ""; }
    function initialize() {
        var dialog = document.getElementById("recipientPickerDialog");
        if (!dialog) return;
        if (dialog.dataset.pickerInitialized === "true") return;
        dialog.dataset.pickerInitialized = "true";
        var singleSelection = dialog.dataset.selectionMode === "single";
        var inputName = dialog.dataset.inputName || "recipientIds";
        var selected = new Map();
        var draft = new Map();
        var currentItems = [];
        var currentPage = 1;
        var hasNext = false;
        var loading = false;
        var search = document.getElementById("recipientSearch");
        var results = document.getElementById("recipientResults");
        var selectedList = document.getElementById("dialogSelectedRecipients");
        var resultStatus = document.getElementById("recipientResultStatus");
        var inputs = document.getElementById("selectedRecipientInputs");
        function addSelection(item) {
            if (!singleSelection && !draft.has(item.recipientId) && draft.size >= 1000) {
                resultStatus.textContent = "한 번에 최대 1,000명까지 선택할 수 있습니다. 검색 조건을 좁혀 나누어 등록해 주세요.";
                return false;
            }
            draft.set(item.recipientId, item);
            return true;
        }

        document.querySelectorAll(".selected-recipient-seed").forEach(function (seed) {
            selected.set(seed.dataset.recipientId, { recipientId: seed.dataset.recipientId,
                maskedName: seed.querySelector(".seed-name").textContent,
                maskedEmail: seed.querySelector(".seed-email").textContent,
                affiliationName: seed.querySelector(".seed-affiliation").textContent,
                jobTitle: seed.querySelector(".seed-job-title").textContent,
                collectionBasis: seed.querySelector(".seed-collection-basis").textContent });
        });

        function make(tag, className, value) {
            var node = document.createElement(tag);
            if (className) node.className = className;
            if (value !== undefined) node.textContent = value;
            return node;
        }
        function renderSummary() {
            var values = Array.from(selected.values());
            document.getElementById("selectedRecipientCount").textContent = singleSelection
                ? (values.length ? "대상자 선택됨" : "대상자 미선택")
                : values.length + "명 선택";
            document.getElementById("selectedRecipientPreview").textContent = values.length
                ? values.slice(0, 3).map(optionTitle).join(", ") + (values.length > 3 ? " 외 " + (values.length - 3) + "명" : "")
                : (singleSelection ? "선택된 대상자가 없습니다." : "선택된 수신자가 없습니다.");
            inputs.replaceChildren();
            values.forEach(function (item) {
                var input = document.createElement("input");
                input.type = "hidden"; input.name = inputName; input.value = item.recipientId;
                inputs.appendChild(input);
            });
        }
        function renderSelected() {
            var values = Array.from(draft.values());
            document.getElementById("dialogSelectedCount").textContent = values.length + "명";
            selectedList.replaceChildren();
            if (!values.length) { selectedList.appendChild(make("p", "recipient-empty", "선택된 수신자가 없습니다.")); return; }
            values.forEach(function (item) {
                var row = make("div", "dialog-selected-item");
                var copy = make("span"); copy.append(make("strong", "", optionTitle(item)), make("small", "", optionMeta(item)));
                if (optionBasis(item)) copy.append(make("small", "recipient-basis", optionBasis(item)));
                var remove = make("button", "", "해제"); remove.type = "button";
                remove.addEventListener("click", function () { draft.delete(item.recipientId); renderSelected(); renderResults(); });
                row.append(copy, remove); selectedList.appendChild(row);
            });
        }
        function renderResults() {
            results.replaceChildren();
            if (!currentItems.length) { results.appendChild(make("p", "recipient-empty", "검색 결과가 없습니다.")); }
            currentItems.forEach(function (item) {
                var label = make("label", "recipient-result-item");
                var checkbox = document.createElement("input"); checkbox.type = singleSelection ? "radio" : "checkbox";
                if (singleSelection) checkbox.name = "recipientPickerChoice";
                checkbox.checked = draft.has(item.recipientId);
                checkbox.addEventListener("change", function () {
                    if (checkbox.checked) {
                        if (singleSelection) draft.clear();
                        addSelection(item);
                    } else draft.delete(item.recipientId);
                    renderSelected(); renderResults();
                });
                var copy = make("span"); copy.append(make("strong", "", optionTitle(item)), make("small", "", optionMeta(item)));
                if (optionBasis(item)) copy.append(make("small", "recipient-basis", optionBasis(item)));
                label.append(checkbox, copy); results.appendChild(label);
            });
            document.getElementById("recipientPageLabel").textContent = currentPage + "페이지";
            document.getElementById("previousRecipientPage").disabled = currentPage <= 1 || loading;
            document.getElementById("nextRecipientPage").disabled = !hasNext || loading;
        }
        async function loadPage(page) {
            if (loading) return;
            loading = true; resultStatus.textContent = "불러오는 중";
            try {
                var url = new URL(dialog.dataset.optionsUrl, window.location.href);
                url.searchParams.set("keyword", search.value.trim()); url.searchParams.set("page", page);
                var response = await fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } });
                if (!response.ok) throw new Error("수신자 목록을 불러오지 못했습니다.");
                var data = await response.json(); currentItems = data.items; currentPage = data.page; hasNext = data.hasNext;
                resultStatus.textContent = currentItems.length + "명 표시";
            } catch (error) { currentItems = []; hasNext = false; resultStatus.textContent = error.message; }
            finally { loading = false; renderResults(); }
        }
        async function selectAllResults() {
            if (loading) return;
            loading = true; resultStatus.textContent = "검색 결과를 선택하는 중";
            try {
                var url = new URL(dialog.dataset.optionsUrl, window.location.href);
                url.searchParams.set("keyword", search.value.trim()); url.searchParams.set("all", "true");
                var response = await fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } });
                if (!response.ok) throw new Error("검색 결과 전체를 선택하지 못했습니다.");
                var data = await response.json();
                var limited = data.hasNext === true;
                data.items.forEach(function (item) { if (!addSelection(item)) limited = true; });
                resultStatus.textContent = limited
                    ? "최대 1,000명까지만 선택했습니다. 전체 검색 결과가 아닙니다. 검색 조건을 좁혀 나누어 등록해 주세요."
                    : draft.size + "명을 선택했습니다.";
                renderSelected();
            } catch (error) { resultStatus.textContent = error.message; }
            finally { loading = false; renderResults(); }
        }
        document.getElementById("openRecipientPicker").addEventListener("click", function () { draft = new Map(selected); renderSelected(); dialog.showModal(); loadPage(1); });
        document.getElementById("closeRecipientPicker").addEventListener("click", function () { dialog.close(); });
        document.getElementById("cancelRecipientSelection").addEventListener("click", function () { dialog.close(); });
        document.getElementById("applyRecipientSelection").addEventListener("click", function () {
            selected.clear(); draft.forEach(function (item, id) { selected.set(id, item); });
            renderSummary(); dialog.close();
            dialog.dispatchEvent(new CustomEvent("recipient-picker:applied", { detail: { items: Array.from(selected.values()) } }));
        });
        document.getElementById("searchRecipients").addEventListener("click", function () { loadPage(1); });
        search.addEventListener("keydown", function (event) { if (event.key === "Enter") { event.preventDefault(); loadPage(1); } });
        document.getElementById("previousRecipientPage").addEventListener("click", function () { loadPage(currentPage - 1); });
        document.getElementById("nextRecipientPage").addEventListener("click", function () { loadPage(currentPage + 1); });
        var selectCurrentPage = document.getElementById("selectCurrentPage");
        var selectAll = document.getElementById("selectAllResults");
        if (selectCurrentPage) selectCurrentPage.addEventListener("click", function () { currentItems.forEach(addSelection); renderSelected(); renderResults(); });
        if (selectAll) selectAll.addEventListener("click", selectAllResults);
        renderSummary();
    }
    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initialize);
    else initialize();
}());
