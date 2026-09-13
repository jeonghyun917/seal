package kr.or.arte.seal.statistics.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsJspTest {

    @Test
    void explainsPeriodBasisAndRendersWarningsOutsideDonutGroups() throws IOException {
        String jsp = Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/admin/statistics/index.jsp"));

        assertThat(jsp).contains("기간 기준");
        assertThat(jsp).contains("statistics.responseWarnings()", "statistics.evidenceWarnings()",
                "statistics.rightsWarnings()", "statistics.retentionWarnings()");
        assertThat(jsp).doesNotContain("['SUBMITTED','DECLINED_ALL','WITHDRAWN']");
        assertThat(jsp).doesNotContain("'CANCELLED','OVERDUE'");
        assertThat(jsp).doesNotContain("'DESTROYED','VERIFY_INVALID_HMAC'");
    }
}
