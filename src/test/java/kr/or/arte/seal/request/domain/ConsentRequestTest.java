package kr.or.arte.seal.request.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentRequestTest {
    private static final Instant NOW = Instant.parse("2026-08-24T04:00:00Z");

    @Test
    void draftFinalizesToReadyWithAtLeastOneHourRemaining() {
        ConsentRequest draft = ConsentRequest.draft(UUID.randomUUID(), "CR-20260824-ABCDEFGH",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                NOW.plus(2, ChronoUnit.HOURS));
        ConsentRequest ready = draft.finalizeAt(NOW, UUID.randomUUID());
        assertThat(ready.status()).isEqualTo(ConsentRequestStatus.READY);
    }

    @Test
    void requestCannotFinalizeWithLessThanOneHourRemaining() {
        ConsentRequest draft = ConsentRequest.draft(UUID.randomUUID(), "CR-20260824-ABCDEFGH",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                NOW.plus(59, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> draft.finalizeAt(NOW, UUID.randomUUID()))
                .isInstanceOf(RequestValidationException.class);
    }
}
