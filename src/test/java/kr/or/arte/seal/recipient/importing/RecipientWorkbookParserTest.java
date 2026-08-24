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
    void parsesOnlyNameAndEmailColumnsAndReportsInvalidRows() throws Exception {
        byte[] file = workbook(new String[][]{
                {"이름", "이메일"},
                {"김예술", " ARTIST@EXAMPLE.COM "},
                {"", "broken"}
        });

        ParsedRecipientWorkbook parsed = parser.parse(new ByteArrayInputStream(file));

        assertThat(parsed.rows()).hasSize(2);
        assertThat(parsed.rows().get(0).normalizedEmail()).isEqualTo("artist@example.com");
        assertThat(parsed.rows().get(0).errors()).isEmpty();
        assertThat(parsed.rows().get(1).errors()).containsExactly("NAME_REQUIRED", "EMAIL_INVALID");
    }

    @Test
    void rejectsUnknownHeaders() throws Exception {
        byte[] file = workbook(new String[][]{{"이름", "이메일", "휴대전화"}});
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(file)))
                .isInstanceOf(RecipientWorkbookException.class)
                .hasMessageContaining("헤더");
    }

    @Test
    void rejectsFormulaCells() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("대상자");
            var header = sheet.createRow(0); header.createCell(0).setCellValue("이름"); header.createCell(1).setCellValue("이메일");
            var row = sheet.createRow(1); row.createCell(0).setCellFormula("\"김예술\""); row.createCell(1).setCellValue("a@example.com");
            workbook.write(output);
            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(output.toByteArray())))
                    .isInstanceOf(RecipientWorkbookException.class).hasMessageContaining("수식");
        }
    }

    private static byte[] workbook(String[][] values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("대상자");
            for (int r = 0; r < values.length; r++) {
                var row = sheet.createRow(r);
                for (int c = 0; c < values[r].length; c++) row.createCell(c).setCellValue(values[r][c]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
