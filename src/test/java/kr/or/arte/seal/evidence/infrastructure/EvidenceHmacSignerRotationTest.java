package kr.or.arte.seal.evidence.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceHmacSignerRotationTest {
    @TempDir Path temp;

    @Test
    void verifiesAnOlderSignatureAfterTheActiveKeyChanges() throws Exception {
        Path keyring = temp.resolve("evidence-hmac.keys");
        String v1 = Base64.getEncoder().encodeToString("11111111111111111111111111111111"
                .getBytes(StandardCharsets.UTF_8));
        String v2 = Base64.getEncoder().encodeToString("22222222222222222222222222222222"
                .getBytes(StandardCharsets.UTF_8));
        Files.writeString(keyring, "v1=" + v1 + System.lineSeparator() + "v2=" + v2,
                StandardCharsets.UTF_8);
        byte[] manifest = "manifest".getBytes(StandardCharsets.UTF_8);

        EvidenceHmacSigner initial = new EvidenceHmacSigner(keyring, "v1");
        String signature = initial.sign(manifest);
        EvidenceHmacSigner rotated = new EvidenceHmacSigner(keyring, "v2");

        assertThat(initial.activeKeyId()).isEqualTo("v1");
        assertThat(rotated.activeKeyId()).isEqualTo("v2");
        assertThat(rotated.verify("v1", manifest, signature)).isTrue();
    }

    @Test
    void readsTheLegacyRawKeyAsVersionOne() throws Exception {
        Path raw = temp.resolve("legacy.key");
        Files.write(raw, "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

        EvidenceHmacSigner signer = new EvidenceHmacSigner(raw, "v1");

        assertThat(signer.activeKeyId()).isEqualTo("v1");
        assertThat(signer.verify("v1", "x".getBytes(StandardCharsets.UTF_8),
                signer.sign("x".getBytes(StandardCharsets.UTF_8)))).isTrue();
    }
}
