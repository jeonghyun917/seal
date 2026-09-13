package kr.or.arte.seal.evidence.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import kr.or.arte.seal.evidence.application.EvidenceWorkItem;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public final class OpenHtmlEvidencePdfRenderer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.KOREAN).withZone(KST);

    public byte[] render(EvidenceWorkItem item) {
        try {
            JsonNode snapshot = JSON.readTree(item.snapshotJson());
            String html = document(item, snapshot);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.useFont(() -> OpenHtmlEvidencePdfRenderer.class
                        .getResourceAsStream("/fonts/NotoSansKR-Regular.ttf"), "Noto Sans KR");
                builder.withHtmlContent(html, null);
                builder.toStream(output);
                builder.run();
                return output.toByteArray();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("증적 PDF를 만들 수 없습니다.", exception);
        }
    }

    private static String document(EvidenceWorkItem item, JsonNode snapshot) {
        String institution = text(snapshot, "institution", "기관 정보 없음");
        String title = text(snapshot, "title", item.templateName());
        String processingName = text(snapshot, "processingActivityName", "-");
        String processingPurpose = text(snapshot, "processingPurpose", "-");
        String retention = text(snapshot, "retentionDisplayText", "-");
        String introduction = htmlText(text(snapshot, "introductionHtml", ""));
        String withdrawal = text(snapshot, "withdrawalNotice", "-");
        String contentHash = text(snapshot, "templateContentHash", "-");

        StringBuilder consentItems = new StringBuilder();
        JsonNode items = snapshot.path("items");
        if (items.isArray()) {
            int index = 0;
            for (JsonNode consentItem : items) {
                String decision = index < item.decisions().size()
                        ? decisionCode(item.decisions().get(index)) : "";
                consentItems.append("<div class='item-card'><table class='item-title'><tr>")
                        .append("<td><span class='item-number'>").append(index + 1).append("</span>")
                        .append("<strong>").append(escape(text(consentItem, "title", "동의 항목")))
                        .append("</strong></td><td class='decision'>")
                        .append(escape(decisionLabel(decision))).append("</td></tr></table>")
                        .append("<table class='details'>")
                        .append(row("구분", typeLabel(text(consentItem, "type", "")),
                                "동의 요건", consentItem.path("required").asBoolean(false) ? "필수" : "선택"))
                        .append(rowWide("처리 목적", text(consentItem, "purpose", "-")))
                        .append(rowWide("개인정보 항목", dataItems(consentItem.path("dataItems"))))
                        .append(rowWide("보유 및 이용기간", text(consentItem, "retentionText", retention)))
                        .append(rowWide("동의 거부 안내", text(consentItem, "refusalNotice", "-")));
                String thirdParty = text(consentItem, "thirdPartyRecipient", "");
                if (!thirdParty.isBlank()) {
                    consentItems.append(rowWide("제공받는 자", thirdParty));
                }
                consentItems.append("</table></div>");
                index++;
            }
        }

        StringBuilder history = new StringBuilder();
        for (String event : item.events()) {
            String[] parts = event.split(" @ ", 2);
            history.append("<tr><td>").append(escape(eventLabel(parts[0]))).append("</td><td>")
                    .append(escape(parts.length > 1 ? format(parts[1]) : "-")).append("</td></tr>");
        }
        if (history.length() == 0) {
            history.append("<tr><td colspan='2'>기록된 처리 이력이 없습니다.</td></tr>");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>"
                + "@page{size:A4;margin:14mm 14mm 14mm;@bottom-right{content:'페이지 ' counter(page) ' / ' counter(pages);font-family:'Noto Sans KR';font-size:8pt;color:#64748b}}"
                + "*{box-sizing:border-box}body{font-family:'Noto Sans KR';color:#14213d;font-size:9.2pt;line-height:1.55;margin:0}"
                + "h1{font-size:23pt;margin:3mm 0 1mm;color:#10264b;letter-spacing:-1px}h2{font-size:13.5pt;margin:5mm 0 2.5mm;page-break-after:avoid;padding-bottom:1.5mm;border-bottom:1.5px solid #2463d4;color:#10264b}"
                + "p{margin:1.5mm 0}.eyebrow{font-size:8pt;color:#2463d4;font-weight:bold}.subtitle{color:#52647f;margin-bottom:6mm}"
                + ".summary,.details,.history,.item-title{width:100%;border-collapse:collapse;table-layout:fixed}.summary th,.summary td,.details th,.details td,.history th,.history td{border:1px solid #d9e2ef;padding:1.8mm 2.2mm;vertical-align:top}"
                + ".summary th,.details th,.history th{background:#f1f5fa;color:#42526b;text-align:left;font-weight:normal;width:20%}.summary td{font-weight:bold;width:30%}"
                + ".item-card{border:1px solid #cbd8e8;border-radius:5px;margin:0 0 3mm;page-break-inside:avoid;overflow:hidden}.item-title td{background:#eaf1fb;padding:2mm 2.5mm}.item-title .decision{text-align:right;color:#14532d;font-weight:bold;width:22%}"
                + ".item-number{display:inline-block;color:#fff;background:#2463d4;border-radius:50%;width:5mm;height:5mm;text-align:center;margin-right:2mm;font-size:8pt}"
                + ".details th{width:22%}.details td{width:28%}.history th{width:50%}.note{background:#f7f9fc;border-left:3px solid #2463d4;padding:3mm 4mm;white-space:pre-line}"
                + ".integrity{font-size:8pt;word-wrap:break-word}.items-heading{page-break-before:always}.footer-note{margin-top:2mm;padding-top:2mm;border-top:1px solid #d9e2ef;color:#64748b;font-size:8.2pt}"
                + "</style></head><body>"
                + "<div class='eyebrow'>" + escape(institution) + " · CONSENT EVIDENCE</div>"
                + "<h1>개인정보 동의 증적</h1><p class='subtitle'>" + escape(title)
                + "에 대한 요청 내용과 수신자의 응답을 확정 시점 기준으로 기록한 문서입니다.</p>"
                + "<h2>동의 요청 정보</h2><table class='summary'>"
                + row("요청 번호", item.requestCode(), "동의서 양식",
                        item.templateName() + " · " + item.templateCode() + " · v" + item.templateVersionNumber())
                + row("처리업무", processingName, "요청 생성일시", format(item.requestCreatedAt()))
                + row("메일 발송일시", format(item.firstSentAt()), "요청 만료일시", format(item.expiresAt()))
                + row("응답 제출일시", format(item.submittedAt()), "응답 상태", statusLabel(item.responseStatus()))
                + "</table>"
                + "<h2>수신자 정보</h2><table class='summary'>"
                + row("이름", item.recipientName(), "이메일", item.recipientEmail())
                + row("소속", item.recipientAffiliation(), "직책", item.recipientJobTitle())
                + "</table>"
                + "<h2>개인정보 처리 안내</h2>"
                + (!introduction.isBlank() ? "<div class='note'>" + escape(introduction) + "</div>" : "")
                + "<table class='details' style='margin-top:3mm'>"
                + rowWide("처리 목적", processingPurpose)
                + rowWide("보유 및 이용기간", retention)
                + rowWide("동의 철회 안내", withdrawal)
                + "</table>"
                + "<h2 class='items-heading'>항목별 동의 내용 및 결정</h2>" + consentItems
                + "<h2>처리 이력</h2><table class='history'><tr><th>처리 내용</th><th>일시</th></tr>"
                + history + "</table>"
                + "<h2>증적 정보</h2><table class='details integrity'>"
                + rowWide("증적 패키지 ID", string(item.packageId()))
                + rowWide("동의 요청 ID", string(item.requestId()))
                + rowWide("요청 스냅샷 SHA-256", item.snapshotHash())
                + rowWide("동의서 콘텐츠 SHA-256", contentHash)
                + "</table>"
                + "<p class='footer-note'>이 문서는 시스템이 보관한 요청 스냅샷, 응답 결정 및 처리 이력을 바탕으로 생성한 내부 무결성 증적입니다. 전자서명 또는 공인전자서명 자체를 의미하지 않습니다.</p>"
                + "</body></html>";
    }

    private static String row(String leftLabel, String leftValue, String rightLabel, String rightValue) {
        return "<tr><th>" + escape(leftLabel) + "</th><td>" + value(leftValue)
                + "</td><th>" + escape(rightLabel) + "</th><td>" + value(rightValue) + "</td></tr>";
    }

    private static String rowWide(String label, String value) {
        return "<tr><th>" + escape(label) + "</th><td colspan='3'>" + value(value) + "</td></tr>";
    }

    private static String value(String value) {
        return escape(value == null || value.isBlank() ? "-" : value);
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText(fallback);
    }

    private static String dataItems(JsonNode items) {
        if (!items.isArray() || items.isEmpty()) return "-";
        StringBuilder result = new StringBuilder();
        Iterator<JsonNode> iterator = items.elements();
        while (iterator.hasNext()) {
            if (result.length() > 0) result.append(", ");
            JsonNode item = iterator.next();
            result.append(item.isTextual() ? item.asText() : text(item, "name", item.toString()));
        }
        return result.toString();
    }

    private static String decisionCode(String decision) {
        int delimiter = decision.lastIndexOf(": ");
        return delimiter >= 0 ? decision.substring(delimiter + 2) : decision;
    }

    private static String decisionLabel(String code) {
        return switch (code == null ? "" : code.toUpperCase(Locale.ROOT)) {
            case "AGREE" -> "동의";
            case "DISAGREE" -> "미동의";
            default -> "결정 정보 없음";
        };
    }

    private static String statusLabel(String status) {
        return switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
            case "SUBMITTED" -> "응답 제출 완료";
            case "DECLINED_ALL" -> "전체 미동의";
            case "WITHDRAWN" -> "동의 철회";
            default -> status == null || status.isBlank() ? "-" : status;
        };
    }

    private static String typeLabel(String type) {
        return switch (type == null ? "" : type.toUpperCase(Locale.ROOT)) {
            case "COLLECTION_USE" -> "개인정보 수집·이용";
            case "THIRD_PARTY_PROVISION" -> "제3자 제공";
            case "SENSITIVE_INFORMATION" -> "민감정보 처리";
            case "UNIQUE_IDENTIFIER" -> "고유식별정보 처리";
            case "MARKETING" -> "홍보·마케팅";
            default -> type == null || type.isBlank() ? "-" : type;
        };
    }

    private static String eventLabel(String event) {
        return switch (event == null ? "" : event.toUpperCase(Locale.ROOT)) {
            case "CREATED" -> "동의 요청 생성";
            case "FINALIZED" -> "동의 요청 내용 확정";
            case "ACTIVATED" -> "동의 요청 활성화";
            case "LINK_REISSUED" -> "동의 링크 재발급";
            case "MAIL_QUEUED" -> "메일 발송 대기";
            case "MAIL_SENT", "SMTP_ACCEPTED" -> "메일 발송 완료";
            case "OPENED" -> "동의 요청 열람";
            case "SUBMITTED" -> "응답 제출";
            case "EXPIRED" -> "동의 요청 만료";
            case "COMPLETED" -> "동의 요청 완료";
            case "CANCELLED" -> "동의 요청 취소";
            case "DECLINED_ALL" -> "전체 미동의";
            case "WITHDRAWN" -> "동의 철회";
            default -> event == null || event.isBlank() ? "기타" : event;
        };
    }

    private static String format(Instant value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    private static String format(String value) {
        try {
            return format(Instant.parse(value));
        } catch (Exception ignored) {
            return value == null || value.isBlank() ? "-" : value.replace('T', ' ').replace("Z", "");
        }
    }

    private static String htmlText(String html) {
        if (html == null) return "";
        return html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("(?i)<li\\s*>", "• ")
                .replaceAll("(?i)</li\\s*>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private static String string(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace(String.valueOf((char) 34), "&quot;").replace("'", "&#39;");
    }
}
