package kr.or.arte.seal.evidence.infrastructure;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenHtmlEvidencePdfRendererTest {
    @Test
    void rendersReadableKoreanEvidencePdf() throws Exception {
        UUID requestId = UUID.randomUUID();
        byte[] pdf = new OpenHtmlEvidencePdfRenderer().render(requestId, "교육 프로그램 개인정보 동의",
                List.of("수집·이용: 동의", "선택 홍보: 미동의"));

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("개인정보 동의 증적").contains(requestId.toString()).contains("미동의");
        }
    }
}
