package kr.or.arte.seal.evidence.persistence;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import static org.assertj.core.api.Assertions.*;

class EvidenceScopeSqlTest {
    @ParameterizedTest
    @ValueSource(strings = {"search", "findScoped", "findByRequestScoped", "requeue"})
    void allAdministratorEntryPointsApplyActivityScope(String statement) throws Exception {
        var configuration = new Configuration();
        try (var input = getClass().getResourceAsStream("/mybatis/mappers/evidence/EvidenceMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "EvidenceMapper.xml", configuration.getSqlFragments()).parse();
        }
        String sql = configuration.getMappedStatement(EvidenceMapper.class.getName() + "." + statement)
                .getBoundSql(new HashMap<>()).getSql().replaceAll("\\s+", " ");
        assertThat(sql).contains("user_processing_activity_scope configured", "user_processing_activity_scope allowed",
                "allowed.processing_activity_id=r.processing_activity_id");
        if (statement.equals("search")) {
            assertThat(sql.indexOf("user_processing_activity_scope")).isLessThan(sql.indexOf("LIMIT"));
        }
    }
}
