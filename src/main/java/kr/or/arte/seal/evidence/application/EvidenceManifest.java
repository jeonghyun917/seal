package kr.or.arte.seal.evidence.application;

import java.util.Map;

public record EvidenceManifest(byte[] canonicalJson, Map<String, String> fileHashes) {
    public EvidenceManifest {
        canonicalJson = canonicalJson.clone();
        fileHashes = Map.copyOf(fileHashes);
    }
}
