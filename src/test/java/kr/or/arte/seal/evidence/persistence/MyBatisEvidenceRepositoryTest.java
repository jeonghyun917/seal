package kr.or.arte.seal.evidence.persistence;

import kr.or.arte.seal.evidence.application.GeneratedEvidence;
import kr.or.arte.seal.evidence.application.EvidenceFileMetadata;
import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyBatisEvidenceRepositoryTest {
    @Test
    void detailRowIsCombinedWithItsEvidenceFiles() {
        EvidenceMapper mapper = mock(EvidenceMapper.class);
        MyBatisEvidenceRepository repository = new MyBatisEvidenceRepository(mapper, mock(PersonalDataCipher.class));
        UUID actorId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        EvidencePackageRow row = new EvidencePackageRow(packageId, requestId,
                "CR-20260828-ABCDEFGH", "개인정보 수집·이용 동의 안내",
                EvidenceGenerationStatus.QUEUED, null, null, null, null,
                Instant.parse("2026-08-28T05:47:33Z"), null, null);
        EvidenceFileMetadata file = new EvidenceFileMetadata(
                "PDF", "consent.pdf", "evidence/consent.pdf", 120L, "sha256");
        when(mapper.findScoped(actorId, packageId)).thenReturn(java.util.Optional.of(row));
        when(mapper.files(packageId)).thenReturn(List.of(file));

        var detail = repository.findScoped(actorId, packageId).orElseThrow();

        assertThat(detail.packageId()).isEqualTo(packageId);
        assertThat(detail.requestCode()).isEqualTo("CR-20260828-ABCDEFGH");
        assertThat(detail.templateTitle()).isEqualTo("개인정보 수집·이용 동의 안내");
        assertThat(detail.files()).containsExactly(file);
    }
    @Test
    void finalizationFailsWhenProcessingLeaseWasAlreadyLost() {
        EvidenceMapper mapper = mock(EvidenceMapper.class);
        MyBatisEvidenceRepository repository = new MyBatisEvidenceRepository(mapper, mock(PersonalDataCipher.class));
        GeneratedEvidence generated = new GeneratedEvidence("evidence/path", "sha", "hmac", "v1", List.of());

        assertThatThrownBy(() -> repository.finalized(UUID.randomUUID(), 7, generated, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최종화");
    }
}
