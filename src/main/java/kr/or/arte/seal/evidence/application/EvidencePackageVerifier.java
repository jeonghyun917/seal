package kr.or.arte.seal.evidence.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.arte.seal.evidence.infrastructure.EvidenceHmacSigner;

import java.util.Map;

public final class EvidencePackageVerifier {
    private final EvidenceHmacSigner signer;
    private final String keyId;
    private final ObjectMapper mapper = new ObjectMapper();

    public EvidencePackageVerifier(EvidenceHmacSigner signer) {
        this(signer, signer.activeKeyId());
    }

    public EvidencePackageVerifier(EvidenceHmacSigner signer, String keyId) {
        this.signer = signer;
        this.keyId = keyId;
    }

    public EvidenceVerificationResult verify(byte[] manifest, String hmac, Map<String, byte[]> files) {
        if (!signer.verify(keyId, manifest, hmac)) {
            return EvidenceVerificationResult.INVALID_HMAC;
        }
        try {
            Map<String, Object> root = mapper.readValue(manifest, new TypeReference<>() {});
            @SuppressWarnings("unchecked") Map<String, Map<String, String>> expected =
                    (Map<String, Map<String, String>>) root.get("files");
            for (var entry : expected.entrySet()) {
                byte[] content = files.get(entry.getKey());
                if (content == null) return EvidenceVerificationResult.MISSING_FILE;
                if (!EvidenceManifestFactory.sha256(content).equals(entry.getValue().get("sha256"))) {
                    return EvidenceVerificationResult.INVALID_HASH;
                }
            }
            return EvidenceVerificationResult.VALID;
        } catch (Exception exception) {
            return EvidenceVerificationResult.INVALID_HASH;
        }
    }
}
