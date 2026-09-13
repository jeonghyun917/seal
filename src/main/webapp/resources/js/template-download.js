(() => {
    "use strict";

    const link = document.querySelector("[data-template-download]");
    if (!link || !window.fetch || !window.URL || !window.URL.createObjectURL) {
        return;
    }

    link.addEventListener("click", async (event) => {
        event.preventDefault();
        if (link.dataset.downloading === "true") {
            return;
        }

        const label = link.querySelector("[data-download-label]") || link;
        const originalText = label.textContent;
        link.dataset.downloading = "true";
        link.setAttribute("aria-disabled", "true");
        label.textContent = "다운로드 중...";

        try {
            const response = await fetch(link.href, {
                credentials: "same-origin",
                headers: { "X-Requested-With": "XMLHttpRequest" }
            });
            if (!response.ok) {
                throw new Error(`Template download failed: ${response.status}`);
            }

            const blob = await response.blob();
            const objectUrl = window.URL.createObjectURL(blob);
            const download = document.createElement("a");
            download.href = objectUrl;
            download.download = "recipient-import-template.xlsx";
            download.hidden = true;
            document.body.appendChild(download);
            download.click();
            download.remove();
            window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 1000);
        } catch (error) {
            window.alert("양식 다운로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        } finally {
            label.textContent = originalText;
            link.removeAttribute("aria-disabled");
            delete link.dataset.downloading;
        }
    });
})();
