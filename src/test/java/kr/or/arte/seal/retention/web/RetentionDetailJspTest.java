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
    }
}
