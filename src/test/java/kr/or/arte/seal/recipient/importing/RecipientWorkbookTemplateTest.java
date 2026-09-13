package kr.or.arte.seal.recipient.importing;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientWorkbookTemplateTest {

    @Test
    void createsCenteredBrandedHeaderAndWritingGuide() throws Exception {
        try (var workbook = RecipientWorkbookTemplate.create()) {
            var recipientSheet = workbook.getSheet("수신자");
            var header = recipientSheet.getRow(0);

            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(header.getHeightInPoints()).isGreaterThanOrEqualTo(30f);
            assertThat(header.getCell(0).getCellStyle().getAlignment())
                    .isEqualTo(HorizontalAlignment.CENTER);
            assertThat(header.getCell(0).getCellStyle().getFillPattern())
                    .isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(header.getCell(0).getCellStyle().getFillForegroundColor())
                    .isNotEqualTo(IndexedColors.AUTOMATIC.getIndex());
            assertThat(header.getCell(0).getCellStyle().getFontIndexAsInt()).isPositive();
            assertThat(recipientSheet.getPaneInformation()).isNotNull();
            assertThat(recipientSheet.getCTWorksheet().isSetAutoFilter()).isTrue();
            assertThat(workbook.getSheet("작성 안내")).isNotNull();
        }
    }
}
