package kr.or.arte.seal.evidence.infrastructure;

import kr.or.arte.seal.evidence.application.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class EvidenceConfiguration {
    @Bean EvidenceWorker evidenceWorker(EvidenceRepository repository,EvidenceArtifactGenerator generator,
            @Value("${seal.evidence.worker-enabled:false}") boolean enabled,Clock clock){return new EvidenceWorker(repository,generator,enabled,clock);}
    @Bean EvidenceWorkerScheduler evidenceWorkerScheduler(EvidenceWorker worker){return new EvidenceWorkerScheduler(worker);}
}
