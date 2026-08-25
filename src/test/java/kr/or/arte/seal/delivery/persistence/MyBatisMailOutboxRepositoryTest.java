package kr.or.arte.seal.delivery.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MyBatisMailOutboxRepositoryTest {
    @Test
    void acceptedRequiresMatchingProcessingStateAndRowVersion() {
        MailOutboxMapper mapper = mock(MailOutboxMapper.class);
        MyBatisMailOutboxRepository repository = new MyBatisMailOutboxRepository(mapper);

        assertThatThrownBy(() -> repository.accepted(UUID.randomUUID(), 1, 3, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상태");
    }
}
