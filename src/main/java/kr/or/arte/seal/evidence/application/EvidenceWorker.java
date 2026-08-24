package kr.or.arte.seal.evidence.application;

import java.time.*;

public final class EvidenceWorker {
    private final EvidenceRepository repository; private final EvidenceArtifactGenerator generator;
    private final boolean enabled; private final Clock clock;
    public EvidenceWorker(EvidenceRepository repository, EvidenceArtifactGenerator generator, boolean enabled, Clock clock) {
        this.repository=repository; this.generator=generator; this.enabled=enabled; this.clock=clock;
    }
    public int processBatch(int limit) {
        if (!enabled) return 0;
        Instant now=clock.instant(); var items=repository.claimDue(now,limit,now.minus(Duration.ofMinutes(10)));
        for (var item:items) try { repository.finalized(item.packageId(),generator.generate(item),now); }
        catch (RuntimeException exception) { repository.failed(item.packageId(),"EVIDENCE_GENERATION_FAILED",now); }
        return items.size();
    }
}
