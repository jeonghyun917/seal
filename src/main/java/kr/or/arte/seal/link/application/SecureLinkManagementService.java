package kr.or.arte.seal.link.application;

import kr.or.arte.seal.link.domain.SecureLinkStatus;
import kr.or.arte.seal.request.application.ConsentRequestRepository;
import kr.or.arte.seal.request.application.ConsentRestrictionQuery;
import kr.or.arte.seal.request.application.RequestBusinessConflictException;
import kr.or.arte.seal.request.application.RequestNotFoundException;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class SecureLinkManagementService {
    private final ConsentRequestRepository requests;
    private final SecureLinkRepository links;
    private final SecureTokenGenerator generator;
    private final ConsentRestrictionQuery restrictions;
    private final SecureLinkHasher hasher;
    private final Clock clock;

    public SecureLinkManagementService(ConsentRequestRepository requests, SecureLinkRepository links,
            SecureTokenGenerator generator, SecureLinkHasher hasher,
            ConsentRestrictionQuery restrictions, Clock clock) {
        this.requests = requests; this.links = links; this.generator = generator;
        this.hasher = hasher; this.restrictions = restrictions; this.clock = clock;
    }

    @Transactional
    public IssuedSecureLink activate(UUID actorId, UUID requestId, long versionNo) {
        var request = requests.findScopedForUpdate(actorId, requestId).orElseThrow(RequestNotFoundException::new);
        if (request.versionNo() != versionNo || request.status() != ConsentRequestStatus.READY) throw conflict();
        if (!request.expiresAt().isAfter(clock.instant())) throw new RequestBusinessConflictException("이미 만료된 요청입니다.");
        requireNotRestricted(request.recipientId());
        Issuance issuance = issue(requestId, actorId, request.expiresAt());
        if (!links.activateRequest(requestId, issuance.linkId(), actorId, clock.instant(), versionNo)) throw conflict();
        requests.insertEvent(requestId, "ACTIVATED", "ADMIN", actorId, null, clock.instant());
        return new IssuedSecureLink(issuance.linkId(), issuance.token(), request.expiresAt());
    }

    @Transactional
    public IssuedSecureLink reissue(UUID actorId, UUID requestId, long versionNo) {
        var request = requests.findScopedForUpdate(actorId, requestId).orElseThrow(RequestNotFoundException::new);
        if (request.versionNo() != versionNo || request.status() != ConsentRequestStatus.ACTIVE) throw conflict();
        if (!request.expiresAt().isAfter(clock.instant())) throw new RequestBusinessConflictException("이미 만료된 요청입니다.");
        requireNotRestricted(request.recipientId());
        links.revokeCurrent(requestId, actorId, clock.instant(), "REISSUED");
        Issuance issuance = issue(requestId, actorId, request.expiresAt());
        if (!links.replaceCurrentLink(requestId, issuance.linkId(), actorId, clock.instant(), versionNo)) throw conflict();
        requests.insertEvent(requestId, "LINK_REISSUED", "ADMIN", actorId, null, clock.instant());
        return new IssuedSecureLink(issuance.linkId(), issuance.token(), request.expiresAt());
    }

    private void requireNotRestricted(UUID recipientId) {
        LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        if (restrictions.hasActiveRestriction(recipientId, today)) {
            throw new RequestBusinessConflictException("처리제한 중인 대상자의 링크를 발급할 수 없습니다.");
        }
    }

    private Issuance issue(UUID requestId, UUID actorId, java.time.Instant expiresAt) {
        DuplicateKeyException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            String token = generator.generate(); UUID id = UUID.randomUUID();
            try {
                links.insert(new SecureLinkInsert(id, requestId, hasher.hmac(token),
                        SecureLinkStatus.ISSUED, expiresAt, actorId, clock.instant()));
                return new Issuance(id, token);
            } catch (DuplicateKeyException collision) { last = collision; }
        }
        throw new IllegalStateException("보안 링크 토큰을 발급할 수 없습니다.", last);
    }

    private static RequestBusinessConflictException conflict() {
        return new RequestBusinessConflictException("요청 상태가 변경되었습니다. 최신 내용을 다시 불러오세요.");
    }
    private record Issuance(UUID linkId, String token) { }
}
