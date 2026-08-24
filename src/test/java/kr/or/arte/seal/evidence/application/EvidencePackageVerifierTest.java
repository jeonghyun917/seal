package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.infrastructure.EvidenceHmacSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class EvidencePackageVerifierTest {
    @TempDir Path temp;

    @Test
    void detectsHmacAndFileTampering() throws Exception {
        Path key = temp.resolve("hmac.key");
        Files.write(key, "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        var files = new LinkedHashMap<String, byte[]>();
        files.put("request.json", "original".getBytes(StandardCharsets.UTF_8));
        EvidenceManifest manifest = new EvidenceManifestFactory().create(files);
        EvidenceHmacSigner signer = new EvidenceHmacSigner(key);
        String hmac = signer.sign(manifest.canonicalJson());
        EvidencePackageVerifier verifier = new EvidencePackageVerifier(signer);

        assertThat(verifier.verify(manifest.canonicalJson(), hmac, files)).isEqualTo(EvidenceVerificationResult.VALID);
        assertThat(verifier.verify(manifest.canonicalJson(), hmac + "x", files)).isEqualTo(EvidenceVerificationResult.INVALID_HMAC);
        files.put("request.json", "tampered".getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(manifest.canonicalJson(), hmac, files)).isEqualTo(EvidenceVerificationResult.INVALID_HASH);
    }
}
