package kr.or.arte.seal.link.application;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureTokenGenerator {
    private final SecureRandom random;
    public SecureTokenGenerator(SecureRandom random) { this.random = random; }
    public String generate() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
