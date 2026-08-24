package kr.or.arte.seal.consent.application;
import java.time.Instant; import java.util.UUID;
public record PublicConsentFlow(String flowId,UUID requestId,Instant expiresAt) {}
