package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.config.TemplateContentConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateHtmlSanitizerTest {

    private final TemplateHtmlSanitizer sanitizer = new TemplateHtmlSanitizer(
            new TemplateContentConfiguration().templateHtmlPolicy());

    @Test
    void removesScriptAndAttributesButKeepsApprovedMarkup() {
        String result = sanitizer.sanitize(
                "<p onclick='x()'><strong>안내</strong><script>x()</script></p>");

        assertThat(result).isEqualTo("<p><strong>안내</strong></p>");
    }

    @Test
    void dropsLinksImagesStylesAndUnknownElements() {
        String result = sanitizer.sanitize(
                "<h3 style='color:red'>제목</h3><a href='https://example.com'>링크</a><img src='x'>");

        assertThat(result).isEqualTo("<h3>제목</h3>링크");
    }

    @Test
    void nullInputProducesEmptyHtml() {
        assertThat(sanitizer.sanitize(null)).isEmpty();
    }
}
