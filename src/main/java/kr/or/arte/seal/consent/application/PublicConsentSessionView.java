package kr.or.arte.seal.consent.application;
import kr.or.arte.seal.request.domain.ConsentRequestStatus; import java.time.Instant; import java.util.UUID;
public record PublicConsentSessionView(UUID sessionId,UUID requestId,UUID linkId,String sessionStatus,Instant expiresAt,String snapshotJson,String snapshotHash,ConsentRequestStatus requestStatus) {}
