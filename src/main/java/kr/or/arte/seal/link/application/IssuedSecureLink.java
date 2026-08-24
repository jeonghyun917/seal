package kr.or.arte.seal.link.application;

import java.time.Instant;

public record IssuedSecureLink(java.util.UUID linkId, String token, Instant expiresAt) {
    public IssuedSecureLink(String token, Instant expiresAt) { this(null, token, expiresAt); }
    @Override public String toString() { return "IssuedSecureLink[expiresAt=" + expiresAt + "]"; }
}
