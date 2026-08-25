package kr.or.arte.seal.evidence.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalEvidenceQuarantineTest {
    @TempDir Path temp;

    @Test
    void quarantineMoveAndPurgeAreIdempotent() {
        LocalEvidenceStorage storage = new LocalEvidenceStorage(temp);
        UUID org = UUID.randomUUID(), request = UUID.randomUUID(), pack = UUID.randomUUID(), job = UUID.randomUUID();
        storage.write(org, request, pack,
                Map.of("request.json", "{}".getBytes(StandardCharsets.UTF_8)));
        String source = org + "/" + request + "/" + pack;

        QuarantinedEvidence first = storage.quarantine(job, source);
        QuarantinedEvidence repeated = storage.quarantine(job, source);

        assertThat(first).isEqualTo(repeated);
        assertThat(first.fileCount()).isEqualTo(1);
        assertThat(Files.exists(storage.resolveRelative(source))).isFalse();
        assertThat(Files.exists(storage.resolveRelative(first.quarantinePath()))).isTrue();
        assertThat(storage.purgeQuarantine(first.quarantinePath())).isEqualTo(1);
        assertThat(storage.purgeQuarantine(first.quarantinePath())).isZero();
    }
}
