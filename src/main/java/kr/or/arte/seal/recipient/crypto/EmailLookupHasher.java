package kr.or.arte.seal.recipient.crypto;

import java.util.List;

public interface EmailLookupHasher {
    String hmac(String normalizedEmail);
    default List<String> candidates(String normalizedEmail) { return List.of(hmac(normalizedEmail)); }
}
