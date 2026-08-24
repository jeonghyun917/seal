package kr.or.arte.seal.request.application;

import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConsentRequestQueryService {
    private final ConsentRequestRepository repository;
    public ConsentRequestQueryService(ConsentRequestRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<ConsentRequestSummary> search(UUID actorId, ConsentRequestStatus status) {
        return repository.searchScoped(actorId, status).stream().map(row -> new ConsentRequestSummary(
                row.requestId(), row.requestCode(), row.recipientId(), row.status(), row.expiresAt(),
                row.createdAt(), row.versionNo())).toList();
    }

    @Transactional(readOnly = true)
    public ConsentRequestRecord require(UUID actorId, UUID requestId) {
        return repository.findScoped(actorId, requestId).orElseThrow(RequestNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public String requireSnapshotJson(UUID actorId, UUID requestId) {
        return repository.findSnapshotJsonScoped(actorId, requestId).orElseThrow(RequestNotFoundException::new);
    }
}
