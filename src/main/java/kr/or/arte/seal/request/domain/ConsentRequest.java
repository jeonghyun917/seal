package kr.or.arte.seal.request.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConsentRequest(UUID requestId, String requestCode, UUID organizationUnitId,
                             UUID recipientId, UUID templateVersionId, UUID createdBy,
                             ConsentRequestStatus status, Instant expiresAt, UUID snapshotId,
                             UUID currentSecureLinkId, long versionNo) {
    private static final Duration MINIMUM_READY_TIME = Duration.ofHours(1);

    public ConsentRequest {
        Objects.requireNonNull(requestId); Objects.requireNonNull(requestCode);
        Objects.requireNonNull(organizationUnitId); Objects.requireNonNull(recipientId);
        Objects.requireNonNull(templateVersionId); Objects.requireNonNull(createdBy);
        Objects.requireNonNull(status); Objects.requireNonNull(expiresAt);
    }

    public static ConsentRequest draft(UUID id, String code, UUID orgId, UUID recipientId,
            UUID templateVersionId, UUID createdBy, Instant expiresAt) {
        return new ConsentRequest(id, code, orgId, recipientId, templateVersionId, createdBy,
                ConsentRequestStatus.DRAFT, expiresAt, null, null, 0);
    }

    public ConsentRequest finalizeAt(Instant now, UUID newSnapshotId) {
        require(ConsentRequestStatus.DRAFT, "초안 요청만 확정할 수 있습니다.");
        if (Duration.between(now, expiresAt).compareTo(MINIMUM_READY_TIME) < 0) {
            throw new RequestValidationException("요청 확정 시 만료까지 최소 1시간이 남아 있어야 합니다.");
        }
        return copy(ConsentRequestStatus.READY, Objects.requireNonNull(newSnapshotId), null);
    }

    public ConsentRequest activate(Instant now, UUID secureLinkId) {
        require(ConsentRequestStatus.READY, "확정된 요청만 활성화할 수 있습니다.");
        requireNotExpired(now);
        return copy(ConsentRequestStatus.ACTIVE, snapshotId, Objects.requireNonNull(secureLinkId));
    }

    public ConsentRequest complete(Instant now) {
        require(ConsentRequestStatus.ACTIVE, "활성 요청만 완료할 수 있습니다.");
        requireNotExpired(now);
        return copy(ConsentRequestStatus.COMPLETED, snapshotId, currentSecureLinkId);
    }

    public ConsentRequest cancel() {
        if (status == ConsentRequestStatus.COMPLETED || status == ConsentRequestStatus.EXPIRED
                || status == ConsentRequestStatus.CANCELLED) {
            throw new RequestValidationException("종료된 요청은 취소할 수 없습니다.");
        }
        return copy(ConsentRequestStatus.CANCELLED, snapshotId, currentSecureLinkId);
    }

    public ConsentRequest expire(Instant now) {
        if (expiresAt.isAfter(now) || (status != ConsentRequestStatus.READY && status != ConsentRequestStatus.ACTIVE)) {
            throw new RequestValidationException("만료 대상 요청이 아닙니다.");
        }
        return copy(ConsentRequestStatus.EXPIRED, snapshotId, currentSecureLinkId);
    }

    private void require(ConsentRequestStatus expected, String message) {
        if (status != expected) throw new RequestValidationException(message);
    }

    private void requireNotExpired(Instant now) {
        if (!expiresAt.isAfter(now)) throw new RequestValidationException("이미 만료된 요청입니다.");
    }

    private ConsentRequest copy(ConsentRequestStatus changed, UUID snapshot, UUID link) {
        return new ConsentRequest(requestId, requestCode, organizationUnitId, recipientId,
                templateVersionId, createdBy, changed, expiresAt, snapshot, link, versionNo + 1);
    }
}
