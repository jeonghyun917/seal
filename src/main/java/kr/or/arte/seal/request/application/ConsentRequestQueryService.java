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
                row.createdAt(), row.versionNo(),row.templateVersionId())).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsentRequestSummary> searchByRecipient(UUID actorId, UUID recipientId) {
        return repository.searchScopedByRecipient(actorId, recipientId).stream().map(row -> new ConsentRequestSummary(
                row.requestId(), row.requestCode(), row.recipientId(), row.status(), row.expiresAt(),
                row.createdAt(), row.versionNo(),row.templateVersionId())).toList();
    }

    @Transactional(readOnly = true)
    public kr.or.arte.seal.common.web.AdminPageResult<ConsentRequestSummary> searchPage(UUID actorId,
            ConsentRequestStatus status,int page,int size){
        return searchPage(actorId, status, null, page, size);
    }

    @Transactional(readOnly = true)
    public kr.or.arte.seal.common.web.AdminPageResult<ConsentRequestSummary> searchPage(UUID actorId,
            ConsentRequestStatus status, String query, int page, int size){
        int safePage=Math.max(1,page),safeSize=Math.max(1,Math.min(100,size)),offset=(safePage-1)*safeSize;
        String normalized = query == null || query.isBlank() ? null : query.strip();
        var rows=repository.searchScoped(actorId,status,normalized,safeSize+1,offset).stream().map(row -> new ConsentRequestSummary(
                row.requestId(),row.requestCode(),row.recipientId(),row.status(),row.expiresAt(),row.createdAt(),row.versionNo(),row.templateVersionId())).toList();
        return kr.or.arte.seal.common.web.AdminPageResult.of(rows,safePage,safeSize);
    }

    public ConsentRequestRecord require(UUID actorId, UUID requestId) {
        return repository.findScoped(actorId, requestId).orElseThrow(RequestNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public String requireSnapshotJson(UUID actorId, UUID requestId) {
        return repository.findSnapshotJsonScoped(actorId, requestId).orElseThrow(RequestNotFoundException::new);
    }
}
