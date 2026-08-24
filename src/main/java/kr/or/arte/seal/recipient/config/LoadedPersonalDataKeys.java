package kr.or.arte.seal.recipient.config;

import javax.crypto.SecretKey;
import java.util.Map;

public record LoadedPersonalDataKeys(String activeKeyId, Map<String, SecretKey> keys) {
    public LoadedPersonalDataKeys {
        keys = Map.copyOf(keys);
    }
}
