package kr.or.arte.seal.statistics.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsMapperXmlTest {

    @Test
    void emitsEventsAndWarningsIntoDedicatedCategories() throws Exception {
        String mapper = Files.readString(Path.of(
                "src/main/resources/mybatis/mappers/statistics/StatisticsMapper.xml"));

        assertThat(mapper).contains(
                "SELECT 'RESPONSE_WARNING', 'WITHDRAWN', count(*)",
                "SELECT 'EVIDENCE_WARNING', 'VERIFY_' || v.verification_result, count(*)",
                "SELECT 'RIGHTS_WARNING', 'OVERDUE', count(*)",
                "SELECT 'RETENTION_WARNING', 'LEGAL_HOLD_ACTIVE', count(*)");
        assertThat(mapper).doesNotContain(
                "SELECT 'RESPONSE', 'WITHDRAWN', count(*)",
                "SELECT 'RIGHTS', 'OVERDUE', count(*)");
    }
}
