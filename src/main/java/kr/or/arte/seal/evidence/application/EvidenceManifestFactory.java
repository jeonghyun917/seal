package kr.or.arte.seal.evidence.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

public final class EvidenceManifestFactory {
    private final ObjectMapper mapper = new ObjectMapper();

    public EvidenceManifest create(Map<String, byte[]> files) {
        try {
            TreeMap<String, String> hashes = new TreeMap<>();
            for (var entry : files.entrySet()) {
                hashes.put(entry.getKey(), sha256(entry.getValue()));
            }
            TreeMap<String, Map<String, String>> entries = new TreeMap<>();
            hashes.forEach((name, hash) -> entries.put(name, Map.of("sha256", hash)));
            byte[] json = mapper.writeValueAsBytes(Map.of("files", entries, "packageVersion", 1));
            return new EvidenceManifest(json, hashes);
        } catch (Exception exception) {
            throw new IllegalStateException("증적 매니페스트를 만들 수 없습니다.", exception);
        }
    }

    static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
