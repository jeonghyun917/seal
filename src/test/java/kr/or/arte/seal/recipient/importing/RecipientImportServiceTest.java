package kr.or.arte.seal.recipient.importing;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.crypto.AesGcmPersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.HmacEmailLookupHasher;
import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientImportServiceTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ORG = UUID.randomUUID();

    @Test
    void validatesEncryptedRowsAndConfirmationPurgesStagingCiphertext() throws Exception {
        FakeRepository repository = new FakeRepository();
        byte[] key = new byte[32]; java.util.Arrays.fill(key, (byte) 7);
        var cipher = new AesGcmPersonalDataCipher(Map.of("v1", new SecretKeySpec(key, "AES")), "v1", new SecureRandom());
        var service = new RecipientImportService(repository, new RecipientWorkbookParser(1_000),
                content -> MalwareScanResult.DISABLED, cipher, new HmacEmailLookupHasher(key),
                command -> {}, Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC),
                new RecipientImportProperties(1_000, 5 * 1024 * 1024));

        RecipientImportPreview preview = service.validate(ACTOR, ORG, workbook());
        RecipientImportResult result = service.confirm(ACTOR, preview.batchId());

        assertThat(preview.validCount()).isEqualTo(1);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(repository.rows.get(0).nameCiphertext()).isNull();
        assertThat(repository.rows.get(0).status()).isEqualTo(RecipientImportRowStatus.IMPORTED);
        assertThat(repository.inserted.affiliationName()).isEqualTo("외부회사 주식회사");
        assertThat(repository.inserted.jobTitle()).isEqualTo("교육팀장");
        assertThat(repository.inserted.collectionBasis()).isEqualTo("2026년 교육 신청서 접수");
    }

    @Test
    void rejectsOrganizationOutsideActorScopeAndDelegatesExpiry() throws Exception {
        FakeRepository repository = new FakeRepository(); repository.access = false;
        byte[] key = new byte[32]; java.util.Arrays.fill(key, (byte) 7);
        var cipher = new AesGcmPersonalDataCipher(Map.of("v1", new SecretKeySpec(key, "AES")), "v1", new SecureRandom());
        var service = new RecipientImportService(repository, new RecipientWorkbookParser(1_000),
                content -> MalwareScanResult.DISABLED, cipher, new HmacEmailLookupHasher(key), command -> {},
                Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC),
                new RecipientImportProperties(1_000, 5 * 1024 * 1024));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.validate(ACTOR, ORG, workbook()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        repository.expired = 2;
        assertThat(service.expireDue(10)).isEqualTo(2);
    }

    private static byte[] workbook() throws Exception {
        try (var book = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet=book.createSheet("수신자"); var h=sheet.createRow(0);
            h.createCell(0).setCellValue("수신자 소속기관"); h.createCell(1).setCellValue("수신자 직책"); h.createCell(2).setCellValue("이름"); h.createCell(3).setCellValue("이메일"); h.createCell(4).setCellValue("수신자 정보 등록 근거");
            var r=sheet.createRow(1); r.createCell(0).setCellValue("외부회사 주식회사"); r.createCell(1).setCellValue("교육팀장"); r.createCell(2).setCellValue("김예술"); r.createCell(3).setCellValue("artist@example.com"); r.createCell(4).setCellValue("2026년 교육 신청서 접수");
            book.write(output); return output.toByteArray();
        }
    }

    private static final class FakeRepository implements RecipientImportRepository {
        RecipientImportBatchRecord batch; RecipientStoredRecord inserted; final List<RecipientImportRowRecord> rows=new ArrayList<>();boolean access=true;int expired;
        public boolean canAccessOrganization(UUID actorId, UUID orgId){return access;}
        public boolean activeEmailExists(UUID orgId,String hmac){return false;}
        public void insertBatch(RecipientImportBatchRecord value,List<RecipientImportRowRecord> values){batch=value;rows.addAll(values);}
        public Optional<RecipientImportBatchRecord> findScopedForUpdate(UUID actorId,UUID batchId){return Optional.of(batch);}
        public List<RecipientImportRowRecord> findRows(UUID batchId){return List.copyOf(rows);}
        public UUID insertRecipientFromImport(RecipientStoredRecord row,UUID actor,Instant now){inserted=row;return row.recipientId();}
        public void markImported(UUID batchId,int row,UUID recipientId){int i=row-2;rows.set(i,rows.get(i).imported(recipientId));}
        public void complete(UUID batchId,int count,Instant now){batch=batch.completed(count,now);}
        public int expireDue(Instant now,int limit){return expired;}
    }
}
