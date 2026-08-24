package kr.or.arte.seal.request.application;

import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ConsentRequestQueryServiceTest {

    @Test
    void detailQueryDoesNotUseWriteLockingRepositoryMethod() {
        UUID actorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ConsentRequestRecord row = new ConsentRequestRecord(requestId, "CR-20260824-ABCDEFGH",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ConsentRequestStatus.DRAFT,
                Instant.parse("2026-08-31T09:00:00Z"), UUID.randomUUID(), null, null,
                actorId, Instant.parse("2026-08-24T09:00:00Z"), 0);
        ConsentRequestRepository repository = mock(ConsentRequestRepository.class, invocation -> {
            if (invocation.getMethod().getReturnType().equals(Optional.class)) {
                return Optional.of(row);
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });

        ConsentRequestRecord result = new ConsentRequestQueryService(repository).require(actorId, requestId);

        assertThat(result).isEqualTo(row);
        verify(repository).findScoped(actorId, requestId);
        verify(repository, never()).findScopedForUpdate(any(), any());
    }
}
