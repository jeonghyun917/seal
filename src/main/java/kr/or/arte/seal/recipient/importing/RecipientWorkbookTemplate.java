package kr.or.arte.seal.recipient.importing;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;

final class RecipientWorkbookTemplate {
    private static final String[] HEADERS = {
            "수신자 소속기관", "수신자 직책", "이름", "이메일", "수신자 정보 등록 근거"
    };
    private static final String[] GUIDE = {
            "수신자가 소속된 회사·기관명을 입력합니다. 개인 또는 무소속이면 비워 둘 수 있습니다. | 예: 주식회사 예술교육",
            "수신자의 직책이나 직위를 입력합니다. 확인되지 않으면 비워 둘 수 있습니다. | 예: 교육팀장",
            "동의 요청을 받을 수신자의 실명을 입력합니다. | 예: 홍길동",
            "동의 안내 메일을 받을 정확한 이메일 주소를 입력합니다. | 예: gildong@example.com",
            "이름과 이메일을 확보해 시스템에 등록한 경로와 근거를 입력합니다. | 예: 2026년 교육 프로그램 신청서 접수"
    };

    private RecipientWorkbookTemplate() {}

    static XSSFWorkbook create() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet recipientSheet = workbook.createSheet("수신자");
        recipientSheet.setDisplayGridlines(false);
        recipientSheet.setZoom(95);
        recipientSheet.setTabColor(color(36, 99, 199));
        recipientSheet.createFreezePane(0, 1);
        recipientSheet.setAutoFilter(new CellRangeAddress(0, 1000, 0, HEADERS.length - 1));
        recipientSheet.setActiveCell(new CellAddress("A2"));

        XSSFCellStyle headerStyle = headerStyle(workbook);
        XSSFCellStyle inputStyle = inputStyle(workbook, false);
        XSSFCellStyle alternateInputStyle = inputStyle(workbook, true);
        var header = recipientSheet.createRow(0);
        header.setHeightInPoints(34);
        var drawing = recipientSheet.createDrawingPatriarch();
        var helper = workbook.getCreationHelper();
        for (int column = 0; column < HEADERS.length; column++) {
            var cell = header.createCell(column);
            cell.setCellValue(HEADERS[column]);
            cell.setCellStyle(headerStyle);
            var anchor = helper.createClientAnchor();
            anchor.setCol1(column);
            anchor.setCol2(Math.min(column + 2, HEADERS.length));
            anchor.setRow1(0);
            anchor.setRow2(4);
            var comment = drawing.createCellComment(anchor);
            comment.setString(helper.createRichTextString(GUIDE[column]));
            comment.setAuthor("한국문화예술교육진흥원");
            cell.setCellComment(comment);
        }

        for (int rowIndex = 1; rowIndex <= 50; rowIndex++) {
            var row = recipientSheet.createRow(rowIndex);
            row.setHeightInPoints(25);
            XSSFCellStyle style = rowIndex % 2 == 0 ? alternateInputStyle : inputStyle;
            for (int column = 0; column < HEADERS.length; column++) row.createCell(column).setCellStyle(style);
        }
        int[] widths = {28, 20, 18, 32, 46};
        for (int column = 0; column < widths.length; column++) recipientSheet.setColumnWidth(column, widths[column] * 256);

        createGuideSheet(workbook);
        workbook.setActiveSheet(0);
        workbook.setSelectedTab(0);
        return workbook;
    }

    private static void createGuideSheet(XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("작성 안내");
        sheet.setDisplayGridlines(false);
        sheet.setZoom(95);
        sheet.setTabColor(color(91, 120, 163));
        sheet.setColumnWidth(0, 26 * 256);
        sheet.setColumnWidth(1, 78 * 256);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 1));

        var title = sheet.createRow(0);
        title.setHeightInPoints(26);
        var titleCell = title.createCell(0);
        titleCell.setCellValue("동의 수신자 Excel 등록 양식");
        titleCell.setCellStyle(titleStyle(workbook));
        sheet.createRow(1).setHeightInPoints(26);

        var note = sheet.createRow(2);
        note.setHeightInPoints(38);
        var noteCell = note.createCell(0);
        noteCell.setCellValue("수신자 시트의 항목명을 변경하지 말고, 2행부터 한 명씩 입력해 주세요. 최대 1,000명까지 등록할 수 있습니다.");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 1));
        noteCell.setCellStyle(noteStyle(workbook));

        var header = sheet.createRow(4);
        header.setHeightInPoints(30);
        header.createCell(0).setCellValue("항목");
        header.createCell(1).setCellValue("작성 방법 및 예시");
        XSSFCellStyle guideHeaderStyle = headerStyle(workbook);
        header.getCell(0).setCellStyle(guideHeaderStyle);
        header.getCell(1).setCellStyle(guideHeaderStyle);

        XSSFCellStyle labelStyle = guideLabelStyle(workbook);
        XSSFCellStyle descriptionStyle = guideDescriptionStyle(workbook);
        for (int index = 0; index < HEADERS.length; index++) {
            var row = sheet.createRow(index + 5);
            row.setHeightInPoints(42);
            row.createCell(0).setCellValue(HEADERS[index]);
            row.createCell(1).setCellValue(GUIDE[index].replace(" | ", "\n"));
            row.getCell(0).setCellStyle(labelStyle);
            row.getCell(1).setCellStyle(descriptionStyle);
        }
        sheet.createFreezePane(0, 5);
    }

    private static XSSFCellStyle headerStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color(31, 78, 140));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBottomBorderColor(IndexedColors.WHITE.getIndex());
        style.setFont(font(workbook, 11, true, IndexedColors.WHITE.getIndex()));
        return style;
    }

    private static XSSFCellStyle inputStyle(XSSFWorkbook workbook, boolean alternate) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        if (alternate) {
            style.setFillForegroundColor(color(247, 250, 254));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setFont(font(workbook, 10, false, IndexedColors.DARK_BLUE.getIndex()));
        return style;
    }

    private static XSSFCellStyle titleStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color(19, 55, 101));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFont(font(workbook, 18, true, IndexedColors.WHITE.getIndex()));
        return style;
    }

    private static XSSFCellStyle noteStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color(232, 241, 254));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFont(font(workbook, 10, false, IndexedColors.DARK_BLUE.getIndex()));
        return style;
    }

    private static XSSFCellStyle guideLabelStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color(239, 244, 251));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFont(font(workbook, 10, true, IndexedColors.DARK_BLUE.getIndex()));
        return style;
    }

    private static XSSFCellStyle guideDescriptionStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFont(font(workbook, 10, false, IndexedColors.DARK_BLUE.getIndex()));
        return style;
    }

    private static XSSFFont font(XSSFWorkbook workbook, int size, boolean bold, short color) {
        XSSFFont font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) size);
        font.setBold(bold);
        font.setColor(color);
        return font;
    }

    private static XSSFColor color(int red, int green, int blue) {
        return new XSSFColor(new Color(red, green, blue), new DefaultIndexedColorMap());
    }
}