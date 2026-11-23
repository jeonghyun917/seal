(() => {
    "use strict";

    const link = document.querySelector("[data-template-download]");
    if (!link || !window.fetch || !window.URL || !window.URL.createObjectURL) {
        return;
    }

    const form = link.form;
    (form || link).addEventListener(form ? "submit" : "click", async (event) => {
        if (form && event.submitter !== link) return;
        event.preventDefault();
        if (link.dataset.downloading === "true") {
            return;
        }

        const label = link.querySelector("[data-download-label]") || link;
        const originalText = label.textContent;
        link.dataset.downloading = "true";
        link.setAttribute("aria-disabled", "true");
        link.setAttribute("aria-busy", "true");
        label.textContent = "다운로드 중...";
        const status = document.querySelector("[data-download-status]");
        if (status) status.textContent = "엑셀 파일을 준비하고 있습니다. 잠시 기다려 주세요.";

        try {
            const url = new URL(form ? link.formAction : link.href, window.location.href);
            if (form) new FormData(form).forEach((value, key) => url.searchParams.set(key, value));
            const response = await fetch(url, {
                credentials: "same-origin",
                headers: { "X-Requested-With": "XMLHttpRequest" }
            });
            if (!response.ok || (response.headers.get("content-type") || "").includes("text/html")) {
                throw new Error(`Template download failed: ${response.status}`);
            }

            const blob = await response.blob();
            const objectUrl = window.URL.createObjectURL(blob);
            const download = document.createElement("a");
            download.href = objectUrl;
            download.download = link.dataset.downloadFilename || link.getAttribute("download") || "recipient-import-template.xlsx";
            download.hidden = true;
            document.body.appendChild(download);
            download.click();
            download.remove();
            if (status) status.textContent = "파일 준비가 완료되었습니다. 브라우저의 다운로드 목록을 확인해 주세요.";
            window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 1000);
        } catch (error) {
            const message = "엑셀 다운로드에 실패했습니다. 로그인 상태를 확인한 뒤 다시 시도해 주세요.";
            if (status) status.textContent = message;
            else window.alert(message);
        } finally {
            label.textContent = originalText;
            link.removeAttribute("aria-disabled");
            link.removeAttribute("aria-busy");
            delete link.dataset.downloading;
        }
    });
})();
