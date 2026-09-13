package kr.or.arte.seal.policy.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.or.arte.seal.policy.domain.retention.DestructionMethod;
import kr.or.arte.seal.policy.domain.retention.RetentionPeriodUnit;
import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;

import java.util.UUID;

public class RetentionPolicyForm {
    @NotBlank @Size(max = 100) private String policyName;
    @NotNull(message = "소유 조직을 선택해 주세요") private UUID ownerOrganizationUnitId;
    @NotNull(message = "보유기간 시작 기준을 선택해 주세요") private RetentionStartEvent startEvent;
    @Min(1) @Max(100) private int periodValue = 1;
    @NotNull private RetentionPeriodUnit periodUnit = RetentionPeriodUnit.YEAR;
    @Min(0) @Max(365) private int gracePeriodDays;
    @NotNull private DestructionMethod destructionMethod = DestructionMethod.DELETE;

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public UUID getOwnerOrganizationUnitId() { return ownerOrganizationUnitId; }
    public void setOwnerOrganizationUnitId(UUID value) { this.ownerOrganizationUnitId = value; }
    public RetentionStartEvent getStartEvent() { return startEvent; }
    public void setStartEvent(RetentionStartEvent startEvent) { this.startEvent = startEvent; }
    public int getPeriodValue() { return periodValue; }
    public void setPeriodValue(int periodValue) { this.periodValue = periodValue; }
    public RetentionPeriodUnit getPeriodUnit() { return periodUnit; }
    public void setPeriodUnit(RetentionPeriodUnit periodUnit) { this.periodUnit = periodUnit; }
    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int value) { this.gracePeriodDays = value; }
    public DestructionMethod getDestructionMethod() { return destructionMethod; }
    public void setDestructionMethod(DestructionMethod value) { this.destructionMethod = value; }
}
