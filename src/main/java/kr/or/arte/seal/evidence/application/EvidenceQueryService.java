package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import kr.or.arte.seal.evidence.infrastructure.*;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.zip.*;

@Service
public class EvidenceQueryService {
    private final EvidenceRepository repository;
    private final LocalEvidenceStorage storage;
    private final String keyFile;
    private final String activeKeyId;
    private final Clock clock;
    private final AuditEventWriter audit;
    private final RecipientQueryService recipients;

    public EvidenceQueryService(EvidenceRepository repository, @Value("${seal.evidence.root}") String root,
            @Value("${seal.evidence.hmac-key-file:}") String keyFile,
            @Value("${seal.evidence.hmac-active-key-id:v1}") String activeKeyId,
            Clock clock, AuditEventWriter audit, RecipientQueryService recipients) {
        this.repository = repository;
        this.storage = new LocalEvidenceStorage(Path.of(root));
        this.keyFile = keyFile;
        this.activeKeyId = activeKeyId;
        this.clock = clock;
        this.audit = audit;
        this.recipients = recipients;
    }

    public List<EvidencePackageListItem> search(UUID actor, EvidenceGenerationStatus status) {
        return repository.search(actor, status).stream().map(item -> listItem(actor, item)).toList();
    }

    public kr.or.arte.seal.common.web.AdminPageResult<EvidencePackageListItem> searchPage(UUID actor,
            EvidenceGenerationStatus status,int page,int size){
        int safePage=Math.max(1,page),safeSize=Math.max(1,Math.min(100,size)),offset=(safePage-1)*safeSize;
        return kr.or.arte.seal.common.web.AdminPageResult.of(
                repository.search(actor,status,safeSize+1,offset).stream()
                        .map(item -> listItem(actor, item)).toList(),safePage,safeSize);
    }

    private EvidencePackageListItem listItem(UUID actor, EvidencePackageSummary item) {
        var recipient = recipients.find(actor, item.recipientId());
        return new EvidencePackageListItem(item.packageId(), item.requestCode(), item.templateTitle(),
                recipient.maskedName(), recipient.maskedEmail(),
                EvidencePackageListItem.summarizeResponse(item.agreedCount(), item.decisionCount()),
                item.status(), item.queuedAt(), item.finalizedAt(), item.attemptCount(), item.lastErrorCode());
    }

    public EvidencePackageDetail require(UUID actor, UUID id) {
        return repository.findScoped(actor, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "증적을 찾을 수 없습니다."));
    }

    public java.util.Optional<EvidencePackageDetail> findByRequest(UUID actor, UUID requestId) {
        return repository.findByRequestScoped(actor, requestId);
    }

    public void requeue(UUID actor, UUID id) {
        Instant now = clock.instant();
        repository.requeue(actor, id, now);
        audit(actor, id, "EVIDENCE_REQUEUE", "SUCCESS", null, now);
    }

    public EvidenceVerificationResult verify(UUID actor, UUID id) {
        EvidencePackageDetail detail = require(actor, id);
        EvidenceVerificationResult result = verify(detail);
        Instant now = clock.instant();
        repository.recordVerification(id, actor, result, now);
        audit(actor, id, "EVIDENCE_VERIFY", result == EvidenceVerificationResult.VALID ? "SUCCESS" : "FAILURE",
                result.name(), now);
        return result;
    }

    public byte[] download(UUID actor, UUID id) {
        EvidencePackageDetail detail = require(actor, id);
        EvidenceVerificationResult result = verify(detail);
        Instant now = clock.instant();
        repository.recordVerification(id, actor, result, now);
        if (result != EvidenceVerificationResult.VALID) {
            audit(actor, id, "EVIDENCE_DOWNLOAD", "FAILURE", result.name(), now);
            throw new IllegalStateException("무결성 검증에 실패한 증적입니다.");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                for (var file : detail.files()) {
                    zip.putNextEntry(new ZipEntry(file.fileName()));
                    zip.write(storage.read(file.relativePath()));
                    zip.closeEntry();
                }
            }
            audit(actor, id, "EVIDENCE_DOWNLOAD", "SUCCESS", null, now);
            return bytes.toByteArray();
        } catch (Exception exception) {
            audit(actor, id, "EVIDENCE_DOWNLOAD", "FAILURE", "ZIP_CREATION_FAILED", now);
            throw new IllegalStateException("증적 ZIP을 만들 수 없습니다.", exception);
        }
    }

    private EvidenceVerificationResult verify(EvidencePackageDetail detail) {
        if (keyFile == null || keyFile.isBlank()) throw new IllegalStateException("증적 HMAC 키가 설정되지 않았습니다.");
        Map<String, byte[]> all = new HashMap<>();
        for (var file : detail.files()) all.put(file.fileName(), storage.read(file.relativePath()));
        byte[] manifest = all.remove("manifest.json");
        all.remove("manifest.hmac");
        if (manifest == null) return EvidenceVerificationResult.MISSING_FILE;
        return new EvidencePackageVerifier(new EvidenceHmacSigner(Path.of(keyFile), activeKeyId),
                detail.hmacKeyId()).verify(manifest, detail.manifestHmac(), all);
    }

    private void audit(UUID actor, UUID id, String action, String result, String reason, Instant now) {
        audit.append(new AuditEventCommand(now, "ADMIN", actor, action, "EVIDENCE_PACKAGE", id.toString(),
                result, reason, UUID.randomUUID(), Map.of()));
    }
}
