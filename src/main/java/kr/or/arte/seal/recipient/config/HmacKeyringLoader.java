package kr.or.arte.seal.recipient.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HmacKeyringLoader {
    public LoadedHmacKeys load(Path path, String activeKeyId) {
        if (path == null || activeKeyId == null || !activeKeyId.matches("[A-Za-z0-9._-]{1,30}")) {
            throw new IllegalStateException("HMAC 키 파일과 활성 키 ID가 필요합니다.");
        }
        try {
            byte[] raw = Files.readAllBytes(path);
            if (raw.length == 32) return new LoadedHmacKeys("v1", Map.of("v1", raw));
            Map<String, byte[]> keys = new LinkedHashMap<>();
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator <= 0 || separator == line.length() - 1) {
                    throw new IllegalStateException("HMAC 키링 형식이 올바르지 않습니다.");
                }
                String keyId = line.substring(0, separator).trim();
                if (!keyId.matches("[A-Za-z0-9._-]{1,30}")) {
                    throw new IllegalStateException("HMAC 키 ID 형식이 올바르지 않습니다.");
                }
                byte[] key = Base64.getDecoder().decode(line.substring(separator + 1).trim());
                if (key.length < 32) throw new IllegalStateException("HMAC 키는 32바이트 이상이어야 합니다.");
                if (keys.putIfAbsent(keyId, key) != null) {
                    throw new IllegalStateException("HMAC 키 ID가 중복되었습니다.");
                }
            }
            if (keys.isEmpty()) {
                if (raw.length < 32) throw new IllegalStateException("HMAC 키는 32바이트 이상이어야 합니다.");
                return new LoadedHmacKeys("v1", Map.of("v1", raw));
            }
            return new LoadedHmacKeys(activeKeyId, keys);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC 키 파일을 읽을 수 없습니다.", exception);
        }
    }
}
