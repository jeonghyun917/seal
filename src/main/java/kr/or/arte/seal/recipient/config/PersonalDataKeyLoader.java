package kr.or.arte.seal.recipient.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class PersonalDataKeyLoader {
    public LoadedPersonalDataKeys loadAesKeys(Path path, String activeKeyId) {
        if (path == null || activeKeyId == null || activeKeyId.isBlank()) {
            throw new IllegalStateException("개인정보 암호화 키 파일과 활성 키 식별자가 필요합니다.");
        }
        Map<String, SecretKey> keys = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(path, UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0 || separator == line.length() - 1) {
                    throw new IllegalStateException("암호화 키 파일 형식이 올바르지 않습니다.");
                }
                String keyId = line.substring(0, separator).trim();
                if (!keyId.matches("[A-Za-z0-9._-]{1,30}")) {
                    throw new IllegalStateException("암호화 키 식별자 형식이 올바르지 않습니다.");
                }
                byte[] bytes = decode(line.substring(separator + 1).trim());
                if (bytes.length != 32) {
                    throw new IllegalStateException("AES 암호화 키는 32바이트여야 합니다.");
                }
                if (keys.putIfAbsent(keyId, new SecretKeySpec(bytes, "AES")) != null) {
                    throw new IllegalStateException("암호화 키 식별자가 중복되었습니다.");
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("개인정보 암호화 키 파일을 읽을 수 없습니다.", exception);
        }
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalStateException("활성 암호화 키를 찾을 수 없습니다.");
        }
        return new LoadedPersonalDataKeys(activeKeyId, keys);
    }

    public byte[] loadHmacKey(Path path) {
        if (path == null) {
            throw new IllegalStateException("이메일 검색 HMAC 키 파일이 필요합니다.");
        }
        try {
            byte[] key = Files.readAllBytes(path);
            if (key.length < 32) {
                throw new IllegalStateException("HMAC 키는 32바이트 이상이어야 합니다.");
            }
            return key;
        } catch (IOException exception) {
            throw new IllegalStateException("HMAC 키 파일을 읽을 수 없습니다.", exception);
        }
    }

    private static byte[] decode(String encoded) {
        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("암호화 키가 올바른 Base64가 아닙니다.", exception);
        }
    }
}
