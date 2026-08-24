package kr.or.arte.seal.recipient.importing;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class RecipientImportPostgresIntegrationTest {
    @Autowired RecipientImportService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void validatesPreviewsAndConfirmsEncryptedXlsxRows() throws Exception {
        UUID actor = jdbc.queryForObject(
                "SELECT admin_user_id FROM seal.admin_user WHERE login_id='seal-admin'", UUID.class);
        UUID organization = jdbc.queryForObject(
                "SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE", UUID.class);
        String rawEmail = "excel-" + UUID.randomUUID() + "@example.com";

        var preview = service.validate(actor, organization, "교육 신청 명단에서 직접 수집",
                workbook("엑셀 대상자", rawEmail));
        var loadedPreview = service.preview(actor, preview.batchId());
        var result = service.confirm(actor, preview.batchId());
        var repeated = service.confirm(actor, preview.batchId());

        assertThat(loadedPreview.rows()).hasSize(2);
        assertThat(loadedPreview.validCount()).isEqualTo(1);
        assertThat(loadedPreview.duplicateCount()).isEqualTo(1);
        assertThat(loadedPreview.rows().get(0).maskedEmail()).doesNotContain(rawEmail);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(repeated.existing()).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT collection_source FROM seal.recipient WHERE recipient_id=(SELECT imported_recipient_id FROM seal.recipient_import_row WHERE recipient_import_batch_id=? AND imported_recipient_id IS NOT NULL)",
                String.class, preview.batchId())).isEqualTo("EXCEL");
        assertThat(jdbc.queryForObject(
                "SELECT bool_and(recipient_name_ciphertext IS NULL AND recipient_email_ciphertext IS NULL) FROM seal.recipient_import_row WHERE recipient_import_batch_id=?",
                Boolean.class, preview.batchId())).isTrue();
        assertThat(jdbc.queryForList(
                "SELECT row_status FROM seal.recipient_import_row WHERE recipient_import_batch_id=? ORDER BY row_number",
                String.class, preview.batchId())).containsExactly("IMPORTED", "PURGED");
    }

    private static byte[] workbook(String name, String email) throws Exception {
        try (var book = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = book.createSheet("대상자");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름");
            header.createCell(1).setCellValue("이메일");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(name);
            row.createCell(1).setCellValue(email);
            var duplicate = sheet.createRow(2);
            duplicate.createCell(0).setCellValue(name + " 중복");
            duplicate.createCell(1).setCellValue(email);
            book.write(output);
            return output.toByteArray();
        }
    }
}
