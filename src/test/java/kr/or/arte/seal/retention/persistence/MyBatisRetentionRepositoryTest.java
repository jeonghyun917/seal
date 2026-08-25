package kr.or.arte.seal.retention.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MyBatisRetentionRepositoryTest {
    @Test
    void restrictionHoldFailsWhenActorCannotSeeConsentRequest() {
        RetentionMapper mapper = mock(RetentionMapper.class);
        MyBatisRetentionRepository repository = new MyBatisRetentionRepository(mapper);

        assertThatThrownBy(() -> repository.placeRestrictionHold(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("처리제한");
    }
}
