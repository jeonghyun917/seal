package kr.or.arte.seal.evidence.infrastructure;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EvidenceHmacSigner {
    private static final String LEGACY_KEY_ID = "v1";
    private final String activeKeyId;
    private final Map<String, byte[]> keys;

    public EvidenceHmacSigner(Path keyFile) {
        this(keyFile, LEGACY_KEY_ID);
    }

    public EvidenceHmacSigner(Path keyFile, String activeKeyId) {
        if (activeKeyId == null || !activeKeyId.matches("[A-Za-z0-9._-]{1,30}")) {
            throw new IllegalArgumentException("증적 HMAC 활성 키 ID가 올바르지 않습니다.");
        }
        this.keys = load(keyFile);
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalStateException("증적 HMAC 활성 키를 찾을 수 없습니다.");
        }
        this.activeKeyId = activeKeyId;
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public String sign(byte[] value) {
        return sign(activeKeyId, value);
    }

    public boolean verify(byte[] value, String signature) {
        return verify(activeKeyId, value, signature);
    }

    public boolean verify(String keyId, byte[] value, String signature) {
        if (signature == null || keyId == null || !keys.containsKey(keyId)) return false;
        return MessageDigest.isEqual(sign(keyId, value).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }

    private String sign(String keyId, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keys.get(keyId), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Map<String, byte[]> load(Path keyFile) {
        try {
            byte[] raw = Files.readAllBytes(keyFile);
            if (raw.length == 32) return Map.of(LEGACY_KEY_ID, raw);
            Map<String, byte[]> loaded = new LinkedHashMap<>();
            for (String rawLine : Files.readAllLines(keyFile, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator <= 0 || separator == line.length() - 1) {
                    throw new IllegalStateException("증적 HMAC 키링 형식이 올바르지 않습니다.");
                }
                String keyId = line.substring(0, separator).trim();
                if (!keyId.matches("[A-Za-z0-9._-]{1,30}")) {
                    throw new IllegalStateException("증적 HMAC 키 ID가 올바르지 않습니다.");
                }
                byte[] key = Base64.getDecoder().decode(line.substring(separator + 1).trim());
                if (key.length != 32) throw new IllegalStateException("증적 HMAC 키는 32바이트여야 합니다.");
                if (loaded.putIfAbsent(keyId, key) != null) {
                    throw new IllegalStateException("증적 HMAC 키 ID가 중복되었습니다.");
                }
            }
            if (loaded.isEmpty()) throw new IllegalStateException("증적 HMAC 키링이 비어 있습니다.");
            return Map.copyOf(loaded);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("증적 HMAC 키를 읽을 수 없습니다.", exception);
        }
    }
}
