package kr.or.arte.seal.evidence.persistence;

import kr.or.arte.seal.evidence.application.GeneratedEvidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MyBatisEvidenceRepositoryTest {
    @Test
    void finalizationFailsWhenProcessingLeaseWasAlreadyLost() {
        EvidenceMapper mapper = mock(EvidenceMapper.class);
        MyBatisEvidenceRepository repository = new MyBatisEvidenceRepository(mapper);
        GeneratedEvidence generated = new GeneratedEvidence("evidence/path", "sha", "hmac", "v1", List.of());

        assertThatThrownBy(() -> repository.finalized(UUID.randomUUID(), 7, generated, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최종화");
    }
}
