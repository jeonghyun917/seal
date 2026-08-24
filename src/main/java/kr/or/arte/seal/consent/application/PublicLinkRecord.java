package kr.or.arte.seal.consent.application;
import kr.or.arte.seal.link.domain.SecureLinkStatus; import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import java.time.Instant; import java.util.UUID;
public record PublicLinkRecord(UUID linkId,UUID requestId,SecureLinkStatus linkStatus,Instant expiresAt,ConsentRequestStatus requestStatus) {}
