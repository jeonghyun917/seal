package kr.or.arte.seal.delivery.web;

import java.util.regex.Pattern;

final class MailPreviewSanitizer {
    private static final Pattern CONSENT_HREF = Pattern.compile(
            "(?i)\\s+href\\s*=\\s*\\x22[^\\x22]*/consent/[^\\x22]+\\x22");

    private MailPreviewSanitizer() { }

    static String sanitize(String html) {
        if (html == null) return "<p>메일 본문이 파기되었습니다.</p>";
        return CONSENT_HREF.matcher(html).replaceAll("");
    }
}
