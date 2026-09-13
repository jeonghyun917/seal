package kr.or.arte.seal.statistics.application;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.time.*;
import java.util.*;
import java.io.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsWorkbookTest {
    static StatisticsSnapshot empty() { return new StatisticsSnapshot(List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of()); }
    @Test void producesSevenSheetsEvenWhenEmpty() throws Exception {
        var query=mock(StatisticsQueryService.class);
        when(query.query(any(),any(),any())).thenReturn(empty());
        var service=new StatisticsExportService(query,mock(NamedParameterJdbcTemplate.class),mock(PersonalDataCipher.class),Clock.systemUTC());
        var output=new ByteArrayOutputStream();
        service.write(UUID.randomUUID(),LocalDate.of(2026,8,10),LocalDate.of(2026,9,8),output);
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertThat(book.getNumberOfSheets()).isEqualTo(7);
            assertThat(java.util.stream.IntStream.range(0,7).mapToObj(book::getSheetName).toList())
                .containsExactly("통합요약","동의 요청","동의 응답·철회","메일","증적","권리요청","파기·보존 보류");
            for(int i=1;i<7;i++) {
                assertThat(book.getSheetAt(i).getPaneInformation().isFreezePane()).isTrue();
                assertThat(book.getSheetAt(i).getRow(11).getCell(0).getStringCellValue()).contains("내역이 없습니다");
            }
            for(var row:book.getSheetAt(0))for(var cell:row)
                if(cell.getCellType()==CellType.FORMULA)assertThat(cell.getCachedFormulaResultType()).isNotEqualTo(CellType.ERROR);
        }
    }
    @Test void summaryKeepsWarningsSeparateAndSumsRightsTypes() throws Exception {
        var s=new StatisticsSnapshot(List.of(new StatusCount("READY",2),new StatusCount("COMPLETED",1)),
            List.of(new StatusCount("SUBMITTED",1)),List.of(new StatusCount("WITHDRAWN",4)),List.of(),List.of(),List.of(),
            List.of(new StatusCount("ACCESS:RECEIVED",2),new StatusCount("ERASURE:RECEIVED",3)),
            List.of(new StatusCount("OVERDUE",1)),List.of(),List.of());
        try(var book=new XSSFWorkbook()) {
            var style=new StatisticsWorkbook.Styles(book);
            StatisticsWorkbook.summary(book,style,s,LocalDate.of(2026,8,10),LocalDate.of(2026,9,8),LocalDateTime.now());
            book.getCreationHelper().createFormulaEvaluator().evaluateAll();
            var totals=new ArrayList<Double>();
            for(var row:book.getSheetAt(0)) {
                if(row.getCell(0)==null)continue;
                if(row.getCell(0).toString().equals("상태 총계"))totals.add(row.getCell(2).getNumericCellValue());
                if(row.getCell(4)!=null&&row.getCell(4).toString().equals("WITHDRAWN")) {
                    assertThat(row.getCell(2).getNumericCellValue()).isEqualTo(4);
                    assertThat(row.getCell(3).toString()).isEqualTo("해당 없음");
                }
            }
            assertThat(totals).containsExactly(3d,1d,0d,0d,5d,0d);
        }
    }
    @Test void stringsCannotBecomeFormulasAndDatesUseKoreanTime() throws Exception {
        try(var book=new XSSFWorkbook()) {
            var sh=book.createSheet();var row=sh.createRow(0);
            var text=row.createCell(0);StatisticsWorkbook.value(text,"=HYPERLINK(\"bad\")");
            assertThat(text.getCellType()).isEqualTo(CellType.STRING);
            var time=row.createCell(1);StatisticsWorkbook.value(time,java.sql.Timestamp.from(Instant.parse("2026-08-31T04:13:02Z")));
            assertThat(time.getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026,8,31,13,13,2));
            assertThatThrownBy(()->StatisticsWorkbook.value(text,"x".repeat(32768))).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
