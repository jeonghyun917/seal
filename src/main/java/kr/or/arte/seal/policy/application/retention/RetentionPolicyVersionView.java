package kr.or.arte.seal.policy.application.retention;

import kr.or.arte.seal.policy.domain.retention.DestructionMethod;
import kr.or.arte.seal.policy.domain.retention.RetentionPeriodUnit;
import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;

import java.util.UUID;

public record RetentionPolicyVersionView(UUID retentionPolicyVersionId, int versionNumber,
        String workflowStatus, String startEvent, int periodValue, String periodUnit,
        int gracePeriodDays, String destructionMethod, String createdByName,
        String reviewRequestedByName, String approvedByName, String rejectionComment) {
    public String startEventLabel() {
        return RetentionStartEvent.valueOf(startEvent).getLabel();
    }

    public String periodUnitLabel() {
        return RetentionPeriodUnit.valueOf(periodUnit).getLabel();
    }

    public String destructionMethodLabel() {
        return DestructionMethod.valueOf(destructionMethod).getLabel();
    }
}
