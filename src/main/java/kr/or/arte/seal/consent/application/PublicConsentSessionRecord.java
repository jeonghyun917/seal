package kr.or.arte.seal.consent.application;
import java.time.Instant; import java.util.UUID;
public record PublicConsentSessionRecord(UUID sessionId,String flowHmac,UUID requestId,UUID linkId,Instant expiresAt,Instant createdAt) {}
