(() => {
    const fileInput = document.getElementById("file");
    const fileName = document.getElementById("file-name");

    if (!fileInput || !fileName) {
        return;
    }

    fileInput.addEventListener("change", () => {
        const selectedFile = fileInput.files?.[0];
        fileName.textContent = selectedFile?.name ?? "선택된 파일 없음";
        fileName.classList.toggle("has-file", Boolean(selectedFile));
    });
})();