package kr.or.arte.seal.link.application;

import java.time.Instant;

public record IssuedSecureLink(String token, Instant expiresAt) {
    @Override public String toString() { return "IssuedSecureLink[expiresAt=" + expiresAt + "]"; }
}
