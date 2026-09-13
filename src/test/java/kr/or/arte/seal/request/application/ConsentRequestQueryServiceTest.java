package kr.or.arte.seal.request.application;

import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsentRequestQueryServiceTest {

    @Test
    void recipientSearchUsesTheScopedRecipientRepositoryQuery() {
        UUID actorId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        ConsentRequestRepository repository = mock(ConsentRequestRepository.class);
        ConsentRequestRecord row = new ConsentRequestRecord(UUID.randomUUID(), "CR-RECIPIENT",
                UUID.randomUUID(), recipientId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), ConsentRequestStatus.ACTIVE,
                Instant.parse("2026-09-30T09:00:00Z"), UUID.randomUUID(), null, null,
                actorId, Instant.parse("2026-09-01T09:00:00Z"), 0);
        when(repository.searchScopedByRecipient(actorId, recipientId)).thenReturn(List.of(row));

        var result = new ConsentRequestQueryService(repository).searchByRecipient(actorId, recipientId);

        assertThat(result).extracting(ConsentRequestSummary::requestId).containsExactly(row.requestId());
        verify(repository).searchScopedByRecipient(actorId, recipientId);
        verify(repository, never()).searchScoped(actorId, null);
    }

    @Test
    void pagedSearchPassesTrimmedQueryToScopedRepository() {
        UUID actorId = UUID.randomUUID();
        ConsentRequestRepository repository = mock(ConsentRequestRepository.class);
        when(repository.searchScoped(actorId, ConsentRequestStatus.ACTIVE, "교육 동의", 21, 20))
                .thenReturn(java.util.List.of());

        new ConsentRequestQueryService(repository)
                .searchPage(actorId, ConsentRequestStatus.ACTIVE, "  교육 동의  ", 2, 20);

        verify(repository).searchScoped(actorId, ConsentRequestStatus.ACTIVE, "교육 동의", 21, 20);
    }

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
