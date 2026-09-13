package kr.or.arte.seal.statistics.application;

import java.util.List;

public record StatisticsSnapshot(
        List<StatusCount> requests,
        List<StatusCount> responses,
        List<StatusCount> responseWarnings,
        List<StatusCount> mail,
        List<StatusCount> evidence,
        List<StatusCount> evidenceWarnings,
        List<StatusCount> rights,
        List<StatusCount> rightsWarnings,
        List<StatusCount> retention,
        List<StatusCount> retentionWarnings
) {
    public StatisticsSnapshot {
        requests = List.copyOf(requests);
        responses = List.copyOf(responses);
        responseWarnings = List.copyOf(responseWarnings);
        mail = List.copyOf(mail);
        evidence = List.copyOf(evidence);
        evidenceWarnings = List.copyOf(evidenceWarnings);
        rights = List.copyOf(rights);
        rightsWarnings = List.copyOf(rightsWarnings);
        retention = List.copyOf(retention);
        retentionWarnings = List.copyOf(retentionWarnings);
    }
}
