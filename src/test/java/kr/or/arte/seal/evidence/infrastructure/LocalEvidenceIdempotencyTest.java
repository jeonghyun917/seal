package kr.or.arte.seal.evidence.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalEvidenceIdempotencyTest {
    @TempDir Path temp;

    @Test
    void acceptsAnIdenticalRetryButRejectsDifferentContentForTheSamePackage() {
        LocalEvidenceStorage storage = new LocalEvidenceStorage(temp);
        UUID org = UUID.randomUUID(), request = UUID.randomUUID(), pack = UUID.randomUUID();
        Map<String, byte[]> original = Map.of("request.json", "{}".getBytes(StandardCharsets.UTF_8));

        Path first = storage.write(org, request, pack, original);
        Path repeated = storage.write(org, request, pack, original);

        assertThat(repeated).isEqualTo(first);
        assertThatThrownBy(() -> storage.write(org, request, pack,
                Map.of("request.json", "changed".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("다른 내용");
    }
}
