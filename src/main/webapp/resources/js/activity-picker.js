(function () {
    "use strict";

    function make(tag, className, value) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (value !== undefined) node.textContent = value;
        return node;
    }

    function seedActivity() {
        var seed = document.getElementById("selectedActivitySeed");
        if (!seed) return null;
        return {
            activityId: seed.dataset.activityId,
            versionId: seed.dataset.versionId,
            activityCode: seed.querySelector("[data-seed-code]").textContent,
            activityName: seed.querySelector("[data-seed-name]").textContent,
            processingPurpose: seed.querySelector("[data-seed-purpose]").textContent,
            retentionDisplayText: seed.querySelector("[data-seed-retention]").textContent,
            dataItems: Array.from(seed.querySelectorAll("[data-seed-data-item]")).map(function (item) {
                return {
                    processingDataItemId: item.dataset.id,
                    itemOrder: Number(item.dataset.order),
                    itemName: item.textContent,
                    required: item.dataset.required === "true"
                };
            })
        };
    }

    function initialize() {
        var dialog = document.getElementById("activityPickerDialog");
        if (!dialog) return;
        var selected = seedActivity();
        var draft = selected;
        var currentItems = [];
        var currentPage = 1;
        var hasNext = false;
        var loading = false;
        var search = document.getElementById("activitySearch");
        var results = document.getElementById("activityResults");
        var status = document.getElementById("activityResultStatus");
        var versionInput = document.getElementById("processingActivityVersionId");

        function renderSummary() {
            document.getElementById("selectedActivityName").textContent =
                selected ? selected.activityName : "선택된 개인정보 이용 업무가 없습니다.";
            document.getElementById("selectedActivityMeta").textContent = selected
                ? selected.activityCode + " · " + selected.retentionDisplayText
                : "승인된 개인정보 이용 업무를 검색하여 선택해 주세요.";
            document.getElementById("selectedActivityPurpose").textContent =
                selected ? selected.processingPurpose : "";
            versionInput.value = selected ? selected.versionId : "";
        }

        function renderDataItems(activity) {
            var targets = Array.from(document.querySelectorAll("[data-processing-data-items]"));
            var itemTemplate = document.getElementById("consent-item-template");
            if (itemTemplate) {
                var templateTarget = itemTemplate.content.querySelector("[data-processing-data-items]");
                if (templateTarget) targets.push(templateTarget);
            }
            targets.forEach(function (target) {
                var index = target.dataset.itemIndex;
                target.replaceChildren();
                if (!activity || !activity.dataItems.length) {
                    target.appendChild(make("p", "activity-empty", "선택한 개인정보 이용 업무에 등록된 개인정보 항목이 없습니다."));
                    return;
                }
                activity.dataItems.forEach(function (item) {
                    var label = make("label", "check-label");
                    var checkbox = document.createElement("input");
                    checkbox.type = "checkbox";
                    checkbox.name = "items[" + index + "].processingDataItemIds";
                    checkbox.value = item.processingDataItemId;
                    label.append(checkbox, document.createTextNode(item.itemName));
                    if (item.required) label.appendChild(make("small", "", "필수 항목"));
                    target.appendChild(label);
                });
            });
        }

        function renderResults() {
            results.replaceChildren();
            if (!currentItems.length) {
                results.appendChild(make("p", "activity-empty", "검색 결과가 없습니다."));
            }
            currentItems.forEach(function (item) {
                var label = make("label", "activity-result-card");
                var radio = document.createElement("input");
                radio.type = "radio";
                radio.name = "activity-picker-option";
                radio.checked = Boolean(draft && draft.versionId === item.versionId);
                radio.addEventListener("change", function () { draft = item; });
                var copy = make("span");
                copy.append(
                    make("strong", "", item.activityName),
                    make("small", "activity-result-meta", item.activityCode + " · " + item.retentionDisplayText),
                    make("small", "", item.processingPurpose)
                );
                label.append(radio, copy);
                results.appendChild(label);
            });
            document.getElementById("activityPageLabel").textContent = currentPage + "페이지";
            document.getElementById("previousActivityPage").disabled = currentPage <= 1 || loading;
            document.getElementById("nextActivityPage").disabled = !hasNext || loading;
        }

        async function loadPage(page) {
            if (loading) return;
            loading = true;
            status.textContent = "불러오는 중";
            try {
                var url = new URL(dialog.dataset.optionsUrl, window.location.origin);
                url.searchParams.set("keyword", search.value.trim());
                url.searchParams.set("page", page);
                var response = await fetch(url, {
                    credentials: "same-origin",
                    headers: { Accept: "application/json" }
                });
                if (!response.ok) throw new Error("개인정보 이용 업무 목록을 불러오지 못했습니다.");
                var data = await response.json();
                currentItems = data.items;
                currentPage = data.page;
                hasNext = data.hasNext;
                status.textContent = currentItems.length + "건 표시";
            } catch (error) {
                currentItems = [];
                hasNext = false;
                status.textContent = error.message;
            } finally {
                loading = false;
                renderResults();
            }
        }

        document.getElementById("openActivityPicker").addEventListener("click", function () {
            draft = selected;
            dialog.showModal();
            loadPage(1);
        });
        document.getElementById("closeActivityPicker").addEventListener("click", function () { dialog.close(); });
        document.getElementById("cancelActivitySelection").addEventListener("click", function () { dialog.close(); });
        document.getElementById("applyActivitySelection").addEventListener("click", function () {
            if (!draft) {
                status.textContent = "개인정보 이용 업무를 선택해 주세요.";
                return;
            }
            selected = draft;
            renderSummary();
            renderDataItems(selected);
            dialog.close();
        });
        document.getElementById("searchActivities").addEventListener("click", function () { loadPage(1); });
        search.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                event.preventDefault();
                loadPage(1);
            }
        });
        document.getElementById("previousActivityPage").addEventListener("click", function () {
            loadPage(currentPage - 1);
        });
        document.getElementById("nextActivityPage").addEventListener("click", function () {
            loadPage(currentPage + 1);
        });
        renderSummary();
    }

    document.addEventListener("DOMContentLoaded", initialize);
}());
