package kr.or.arte.seal.recipient.importing;

import kr.or.arte.seal.recipient.crypto.EmailNormalizer;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RecipientWorkbookParser {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final int maxRows;

    public RecipientWorkbookParser(int maxRows) {
        if (maxRows < 1) throw new IllegalArgumentException("최대 행 수는 1 이상이어야 합니다.");
        this.maxRows = maxRows;
    }

    public ParsedRecipientWorkbook parse(InputStream input) {
        ZipSecureFile.setMinInflateRatio(0.01d);
        try (XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) throw new RecipientWorkbookException("워크시트가 없습니다.");
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            if (header == null || header.getLastCellNum() != 2
                    || !"이름".equals(text(header.getCell(0))) || !"이메일".equals(text(header.getCell(1)))) {
                throw new RecipientWorkbookException("헤더는 이름, 이메일 두 열이어야 합니다.");
            }
            int last = sheet.getLastRowNum();
            if (last > maxRows) throw new RecipientWorkbookException("대상자는 최대 " + maxRows + "행까지 등록할 수 있습니다.");
            List<RecipientWorkbookRow> rows = new ArrayList<>();
            for (int index = 1; index <= last; index++) {
                var row = sheet.getRow(index);
                if (row == null || (text(row.getCell(0)).isBlank() && text(row.getCell(1)).isBlank())) continue;
                rejectFormula(row.getCell(0)); rejectFormula(row.getCell(1));
                String name = text(row.getCell(0)).strip();
                String email = text(row.getCell(1)).strip();
                List<String> errors = new ArrayList<>();
                if (name.isBlank()) errors.add("NAME_REQUIRED");
                else if (name.length() > 100) errors.add("NAME_TOO_LONG");
                String normalized = email.toLowerCase(Locale.ROOT);
                try { normalized = EmailNormalizer.normalize(email); }
                catch (IllegalArgumentException ignored) { errors.add("EMAIL_INVALID"); }
                if (!email.isBlank() && !EMAIL.matcher(normalized).matches() && !errors.contains("EMAIL_INVALID")) {
                    errors.add("EMAIL_INVALID");
                }
                rows.add(new RecipientWorkbookRow(index + 1, name, normalized, errors));
            }
            if (rows.isEmpty()) throw new RecipientWorkbookException("등록할 대상자 행이 없습니다.");
            return new ParsedRecipientWorkbook(rows);
        } catch (RecipientWorkbookException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new RecipientWorkbookException("유효한 XLSX 파일을 읽을 수 없습니다.", exception);
        }
    }

    private static void rejectFormula(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.FORMULA) {
            throw new RecipientWorkbookException("수식 셀은 사용할 수 없습니다.");
        }
    }
    private static String text(Cell cell) {
        return cell == null ? "" : new DataFormatter(Locale.KOREA).formatCellValue(cell);
    }
}
