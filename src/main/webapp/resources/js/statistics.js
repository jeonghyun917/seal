(() => {
    const svgNamespace = "http://www.w3.org/2000/svg";

    document.querySelectorAll("[data-statistics-chart]").forEach((chart) => {
        const svg = chart.querySelector(".statistics-donut");
        const rows = Array.from(chart.querySelectorAll(".statistics-legend li[data-count]"));
        const values = rows.map((row) => Number(row.dataset.count) || 0);
        const total = values.reduce((sum, value) => sum + value, 0);
        let offset = 0;

        rows.forEach((row, index) => row.classList.toggle("is-zero", values[index] === 0));

        values.forEach((value, index) => {
            if (value <= 0 || total <= 0) return;
            const percentage = value / total * 100;
            const segment = document.createElementNS(svgNamespace, "circle");
            segment.setAttribute("cx", "60");
            segment.setAttribute("cy", "60");
            segment.setAttribute("r", "44");
            segment.setAttribute("pathLength", "100");
            segment.setAttribute("stroke-dasharray", `${percentage} ${100 - percentage}`);
            segment.setAttribute("stroke-dashoffset", `${-offset}`);
            segment.classList.add("statistics-donut-segment", `chart-color-${index % 10}`);
            svg.appendChild(segment);
            offset += percentage;
        });

        const summary = rows.map((row, index) => `${row.dataset.label} ${values[index]}건`).join(", ");
        svg.setAttribute("aria-label", `${chart.dataset.chartTitle}: 총 ${total}건${summary ? `, ${summary}` : ""}`);
    });
})();