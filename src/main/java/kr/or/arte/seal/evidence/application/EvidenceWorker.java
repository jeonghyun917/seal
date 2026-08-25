package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;

import java.time.*;
import java.util.Map;
import java.util.UUID;

public final class EvidenceWorker {
    private final EvidenceRepository repository;
    private final EvidenceArtifactGenerator generator;
    private final boolean enabled;
    private final Clock clock;
    private final AuditEventWriter audit;

    public EvidenceWorker(EvidenceRepository repository, EvidenceArtifactGenerator generator,
            boolean enabled, Clock clock) {
        this(repository, generator, enabled, clock, command -> { });
    }

    public EvidenceWorker(EvidenceRepository repository, EvidenceArtifactGenerator generator,
            boolean enabled, Clock clock, AuditEventWriter audit) {
        this.repository = repository;
        this.generator = generator;
        this.enabled = enabled;
        this.clock = clock;
        this.audit = audit;
    }

    public int processBatch(int limit) {
        if (!enabled) return 0;
        Instant now = clock.instant();
        var items = repository.claimDue(now, limit, now.minus(Duration.ofMinutes(10)));
        for (var item : items) process(item, now);
        return items.size();
    }

    private void process(EvidenceWorkItem item, Instant now) {
        try {
            repository.finalized(item.packageId(), item.rowVersion(), generator.generate(item), now);
        } catch (RuntimeException exception) {
            repository.failed(item.packageId(), item.rowVersion(), "EVIDENCE_GENERATION_FAILED", now);
            audit(item, "EVIDENCE_GENERATE", "FAILURE", "EVIDENCE_GENERATION_FAILED", now);
            return;
        }
        audit(item, "EVIDENCE_GENERATE", "SUCCESS", null, now);
    }

    private void audit(EvidenceWorkItem item, String action, String result, String reason, Instant now) {
        audit.append(new AuditEventCommand(now, "SYSTEM", null, action, "EVIDENCE_PACKAGE",
                item.packageId().toString(), result, reason, UUID.randomUUID(),
                Map.of("requestId", item.requestId().toString())));
    }
}
