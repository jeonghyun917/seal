package kr.or.arte.seal.retention.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionDetailJspTest {
    @Test
    void executionActionSupportsApprovedStartAndExecutingResume() throws Exception {
        String jsp = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/retention/detail.jsp"));

        assertThat(jsp).contains("item.executionAvailable()", "item.executionKey()");
        assertThat(jsp).contains("item.status() == 'EXECUTING'");
        assertThat(jsp).contains("class=\"detail-list\"", "파기 방법", "statusLabel('destruction'",
                "requestTemplate.templateName()", "아직 실행되지 않았습니다");
        assertThat(jsp).contains("confirmMessage", "recipient.maskedName()", "recipient.maskedEmail()",
                "consentRequest.requestCode()");
        assertThat(jsp).contains("<details><summary>작업 식별자 보기</summary>");
    }
}
