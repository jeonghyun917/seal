package kr.or.arte.seal.recipient.config;

import java.util.Map;

public record LoadedHmacKeys(String activeKeyId, Map<String, byte[]> keys) {
    public LoadedHmacKeys {
        keys = Map.copyOf(keys);
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("활성 HMAC 키가 키링에 없습니다.");
        }
    }
}
