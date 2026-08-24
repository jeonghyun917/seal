package kr.or.arte.seal.recipient.crypto;

public interface EmailLookupHasher {
    String hmac(String normalizedEmail);
}
