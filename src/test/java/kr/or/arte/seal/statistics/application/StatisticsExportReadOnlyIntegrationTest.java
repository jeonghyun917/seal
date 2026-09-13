package kr.or.arte.seal.statistics.application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.mybatis.spring.*;
import kr.or.arte.seal.statistics.persistence.*;
import kr.or.arte.seal.recipient.crypto.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="SEAL_DB_PASSWORD",matches=".+")
class StatisticsExportReadOnlyIntegrationTest {
    @Test void realSqlRespectsScopeAndMatchesSummaryWithoutRunningApplicationJobs() throws Exception {
        var env=System.getenv();
        var ds=new DriverManagerDataSource(env.getOrDefault("SEAL_DB_URL","jdbc:postgresql://localhost:5432/seal_dev?currentSchema=seal"),
            env.getOrDefault("SEAL_DB_USERNAME","seal_app"),env.get("SEAL_DB_PASSWORD"));
        var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);
        factory.setTypeHandlersPackage("kr.or.arte.seal.common.persistence");
        factory.setMapperLocations(new ClassPathResource("mybatis/mappers/statistics/StatisticsMapper.xml"));
        var session=new SqlSessionTemplate(factory.getObject());
        var query=new StatisticsQueryService(new MyBatisStatisticsRepository(session.getMapper(StatisticsMapper.class)));
        // Synthetic decrypted values only: the fixture never outputs live personal data.
        var cipher=mock(PersonalDataCipher.class);
        when(cipher.decrypt(any(),any())).thenAnswer(a->((PersonalDataContext)a.getArgument(0)).field().equals("name")?"테스트이름":"sample@example.test");
        var service=new StatisticsExportService(query,new NamedParameterJdbcTemplate(ds),cipher,Clock.systemUTC());
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));tx.setReadOnly(true);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        tx.executeWithoutResult(status->{
            try {
                var jdbc=new JdbcTemplate(ds);
                assertThat(jdbc.queryForObject("SHOW transaction_read_only",String.class)).isEqualTo("on");
                UUID actor=jdbc.queryForObject("SELECT admin_user_id FROM seal.user_scope WHERE active AND scope_type='ORGANIZATION' ORDER BY include_descendants DESC LIMIT 1",UUID.class);
                var from=LocalDate.of(2026,8,10);var to=LocalDate.of(2026,9,8);
                var snapshot=query.query(actor,from,to);
                var output=new ByteArrayOutputStream();service.write(actor,from,to,output);
                try(var book=new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
                    assertThat(book.getNumberOfSheets()).isEqualTo(7);
                    var groups=List.of(snapshot.requests(),snapshot.responses(),snapshot.mail(),snapshot.evidence(),snapshot.rights(),snapshot.retention());
                    for(int i=1;i<=6;i++) {
                        long expected=groups.get(i-1).stream().mapToLong(StatusCount::count).sum();
                        var sh=book.getSheetAt(i);
                        var count=java.util.stream.StreamSupport.stream(sh.spliterator(),false)
                            .filter(r->r.getCell(0)!=null&&r.getCell(0).toString().equals("내역 건수")).findFirst().orElseThrow();
                        assertThat(count.getCell(1).getNumericCellValue()).as(sh.getSheetName()).isEqualTo(expected);
                        for(var row:sh)for(var cell:row)
                            assertThat(cell.toString()).doesNotContain("sample@example.test","테스트이름");
                    }
                }
                Files.write(Path.of("target/statistics-export-preview.xlsx"),output.toByteArray());
                output.reset();service.write(UUID.randomUUID(),from,to,output);
                try(var book=new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
                    for(int i=1;i<7;i++)assertThat(book.getSheetAt(i).getRow(11).getCell(0).toString()).contains("내역이 없습니다");
                }
            } catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
