package kr.or.arte.seal.evidence.infrastructure;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

public final class OpenHtmlEvidencePdfRenderer {
    public byte[] render(UUID requestId, String title, List<String> decisions) {
        StringBuilder rows = new StringBuilder();
        for (String decision : decisions) rows.append("<li>").append(escape(decision)).append("</li>");
        String html = "<html><head><meta charset=\"UTF-8\"/><style>"
                + "@page{size:A4;margin:22mm}body{font-family:'Noto Sans KR';color:#172033;font-size:11pt}"
                + "h1{color:#1456a0;font-size:20pt}dt{font-weight:bold;margin-top:10px}li{margin:7px 0}"
                + "</style></head><body><h1>개인정보 동의 증적</h1><dl><dt>동의서</dt><dd>" + escape(title)
                + "</dd><dt>요청 식별자</dt><dd>" + requestId + "</dd></dl><h2>항목별 결정</h2><ul>"
                + rows + "</ul><p>이 문서는 시스템 내부 무결성 증적이며 전자서명이 아닙니다.</p></body></html>";
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> OpenHtmlEvidencePdfRenderer.class.getResourceAsStream("/fonts/NotoSansKR-Regular.ttf"),
                    "Noto Sans KR");
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("증적 PDF를 만들 수 없습니다.", exception);
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
