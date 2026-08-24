package kr.or.arte.seal.evidence.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalEvidenceStorageTest {
    @TempDir Path temp;

    @Test
    void writesPackageAtomicallyInsideConfiguredRoot() throws Exception {
        LocalEvidenceStorage storage = new LocalEvidenceStorage(temp);
        UUID org = UUID.randomUUID(), request = UUID.randomUUID(), pack = UUID.randomUUID();

        Path result = storage.write(org, request, pack,
                Map.of("request.json", "{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(result).startsWith(temp.toRealPath());
        assertThat(Files.readString(result.resolve("request.json"))).isEqualTo("{}");
        assertThat(Files.list(result.getParent()).filter(p -> p.getFileName().toString().startsWith(".tmp-"))).isEmpty();
    }

    @Test
    void rejectsUnsafeFileNames() {
        LocalEvidenceStorage storage = new LocalEvidenceStorage(temp);
        assertThatThrownBy(() -> storage.write(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Map.of("../escape.json", new byte[0]))).isInstanceOf(IllegalArgumentException.class);
    }
}
