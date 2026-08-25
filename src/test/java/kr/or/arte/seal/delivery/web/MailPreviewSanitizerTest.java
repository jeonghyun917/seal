package kr.or.arte.seal.delivery.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailPreviewSanitizerTest {
    @Test
    void removesBearerConsentHrefButKeepsReadableMailBody() {
        String html = "<p>동의 내용을 확인하세요.</p>"
                + "<a href=\"https://consent.arte.or.kr/consent/secret-token-value\">동의 페이지 열기</a>";

        String preview = MailPreviewSanitizer.sanitize(html);

        assertThat(preview).contains("동의 내용을 확인하세요.", "동의 페이지 열기");
        assertThat(preview).doesNotContain("secret-token-value", "href=");
    }
}
