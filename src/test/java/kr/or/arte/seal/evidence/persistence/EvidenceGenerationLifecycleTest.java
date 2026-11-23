package kr.or.arte.seal.evidence.persistence;

import kr.or.arte.seal.evidence.application.*;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvidenceGenerationLifecycleTest {
    @Test void destructionRejectsLateEnqueue() {
        var mapper = mock(EvidenceMapper.class);
        var repository = new MyBatisEvidenceRepository(mapper, mock(PersonalDataCipher.class));
        UUID request = UUID.randomUUID();
        when(mapper.hasStartedDestruction(request)).thenReturn(true);
        assertThatThrownBy(() -> repository.enqueue(UUID.randomUUID(), request, Instant.now()))
                .isInstanceOf(IllegalStateException.class);
        verify(mapper, never()).enqueue(any(), any(), any(), any());
        var order = inOrder(mapper);
        order.verify(mapper).lockRequest(request);
        order.verify(mapper).hasStartedDestruction(request);
    }
    @Test void revokedClaimCannotGenerateAnyFiles() {
        var mapper = mock(EvidenceMapper.class);
        var repository = new MyBatisEvidenceRepository(mapper, mock(PersonalDataCipher.class));
        var item = new EvidenceWorkItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "동의서", "{}", Instant.now(), List.of(), List.of(), 1, 7);
        var generator = mock(EvidenceArtifactGenerator.class);
        when(mapper.lockPublishableClaim(item.packageId(), item.requestId(), 7)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> repository.generateAndFinalize(item, generator, Instant.now()))
                .isInstanceOf(EvidenceClaimLostException.class);
        verifyNoInteractions(generator);
    }
}
