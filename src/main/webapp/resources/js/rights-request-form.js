(function () {
    "use strict";
    function initialize() {
    var dialog = document.getElementById("recipientPickerDialog");
    var select = document.getElementById("consentRequestId");
    if (!dialog || !select || select.dataset.rightsInitialized === "true") return;
    select.dataset.rightsInitialized = "true";
    var status = document.getElementById("consentRequestStatus");
    var initialValue = select.dataset.selectedValue || "";
    var labels = { DRAFT: "초안", READY: "발송 준비", ACTIVE: "진행 중", COMPLETED: "완료", CANCELLED: "취소", EXPIRED: "만료" };

    function reset(message) {
        select.replaceChildren(new Option(message, ""));
        select.disabled = true;
        status.textContent = "";
    }
    async function load(recipientId) {
        if (!recipientId) { reset("대상자를 먼저 선택해 주세요"); return; }
        select.disabled = true;
        status.textContent = "동의요청을 불러오는 중입니다.";
        try {
            var url = new URL(select.dataset.optionsUrl, window.location.href);
            url.searchParams.set("recipientId", recipientId);
            var response = await fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error("동의요청을 불러오지 못했습니다.");
            var items = await response.json();
            select.replaceChildren(new Option("선택 안 함", ""));
            items.forEach(function (item) {
                select.add(new Option(item.requestCode + " · " + (labels[item.status] || item.status), item.requestId));
            });
            select.disabled = false;
            if (initialValue && items.some(function (item) { return item.requestId === initialValue; })) select.value = initialValue;
            status.textContent = items.length ? items.length + "건의 동의요청을 불러왔습니다." : "연결 가능한 동의요청이 없습니다.";
            initialValue = "";
        } catch (error) {
            reset("동의요청을 불러오지 못했습니다");
            status.textContent = error.message;
        }
    }
    dialog.addEventListener("recipient-picker:applied", function (event) {
        load(event.detail.items[0] ? event.detail.items[0].recipientId : "");
    });
    var initialRecipient = document.querySelector('#selectedRecipientInputs input[name="recipientId"]');
    load(initialRecipient ? initialRecipient.value : "");
    }
    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initialize);
    else initialize();
}());
