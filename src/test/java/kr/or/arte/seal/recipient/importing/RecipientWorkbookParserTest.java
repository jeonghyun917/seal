package kr.or.arte.seal.recipient.importing;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipientWorkbookParserTest {
    private final RecipientWorkbookParser parser = new RecipientWorkbookParser(1_000);

    @Test
    void parsesPerRecipientCollectionBasisAndReportsInvalidRows() throws Exception {
        byte[] file = workbook(new String[][]{
                {"수신자 소속기관", "수신자 직책", "이름", "이메일", "수신자 정보 등록 근거"},
                {"외부회사 주식회사", "교육팀장", "김예술", " ARTIST@EXAMPLE.COM ", "2026년 교육 신청서 접수"},
                {"", "", "", "broken", "짧음"}
        });

        ParsedRecipientWorkbook parsed = parser.parse(new ByteArrayInputStream(file));

        assertThat(parsed.rows()).hasSize(2);
        assertThat(parsed.rows().get(0).affiliationName()).isEqualTo("외부회사 주식회사");
        assertThat(parsed.rows().get(0).jobTitle()).isEqualTo("교육팀장");
        assertThat(parsed.rows().get(0).collectionBasis()).isEqualTo("2026년 교육 신청서 접수");
        assertThat(parsed.rows().get(0).normalizedEmail()).isEqualTo("artist@example.com");
        assertThat(parsed.rows().get(0).errors()).isEmpty();
        assertThat(parsed.rows().get(1).errors())
                .containsExactly("NAME_REQUIRED", "EMAIL_INVALID", "COLLECTION_BASIS_LENGTH_INVALID");
    }

    @Test
    void rejectsWorkbookWithoutPerRecipientCollectionBasisColumn() throws Exception {
        byte[] file = workbook(new String[][]{
                {"수신자 소속기관", "수신자 직책", "이름", "이메일"},
                {"외부회사", "교육팀장", "김예술", "artist@example.com"}
        });

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(file)))
                .isInstanceOf(RecipientWorkbookException.class)
                .hasMessageContaining("수신자 정보 등록 근거");
    }

    @Test
    void reportsMissingCollectionBasisOnItsRow() throws Exception {
        byte[] file = workbook(new String[][]{
                {"수신자 소속기관", "수신자 직책", "이름", "이메일", "수신자 정보 등록 근거"},
                {"외부회사", "교육팀장", "김예술", "artist@example.com", ""}
        });

        assertThat(parser.parse(new ByteArrayInputStream(file)).rows().get(0).errors())
                .containsExactly("COLLECTION_BASIS_REQUIRED");
    }

    @Test
    void reportsJobTitleOverOneHundredCharacters() throws Exception {
        byte[] file = workbook(new String[][]{
                {"수신자 소속기관", "수신자 직책", "이름", "이메일", "수신자 정보 등록 근거"},
                {"외부회사", "직".repeat(101), "김예술", "artist@example.com", "2026년 교육 신청서 접수"}
        });

        assertThat(parser.parse(new ByteArrayInputStream(file)).rows().get(0).errors())
                .containsExactly("JOB_TITLE_TOO_LONG");
    }

    @Test
    void rejectsUnknownHeaders() throws Exception {
        byte[] file = workbook(new String[][]{{"수신자 소속기관", "수신자 직책", "이름", "이메일", "휴대전화"}});
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(file)))
                .isInstanceOf(RecipientWorkbookException.class)
                .hasMessageContaining("헤더");
    }

    @Test
    void rejectsFormulaCells() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("수신자");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("수신자 소속기관");
            header.createCell(1).setCellValue("수신자 직책");
            header.createCell(2).setCellValue("이름");
            header.createCell(3).setCellValue("이메일");
            header.createCell(4).setCellValue("수신자 정보 등록 근거");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("외부회사");
            row.createCell(1).setCellValue("교육팀장");
            row.createCell(2).setCellFormula("\"김예술\"");
            row.createCell(3).setCellValue("a@example.com");
            row.createCell(4).setCellValue("2026년 교육 신청서 접수");
            workbook.write(output);
            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(output.toByteArray())))
                    .isInstanceOf(RecipientWorkbookException.class).hasMessageContaining("수식");
        }
    }

    private static byte[] workbook(String[][] values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("수신자");
            for (int r = 0; r < values.length; r++) {
                var row = sheet.createRow(r);
                for (int c = 0; c < values[r].length; c++) row.createCell(c).setCellValue(values[r][c]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}