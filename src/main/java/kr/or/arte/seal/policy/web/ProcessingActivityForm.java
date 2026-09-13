package kr.or.arte.seal.policy.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.or.arte.seal.policy.domain.activity.LegalBasisType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessingActivityForm {
    @NotBlank @Size(max = 100) private String activityName;
    @NotNull private UUID ownerOrganizationUnitId;
    @NotBlank @Size(min = 10, max = 1000) private String processingPurpose;
    @NotNull private LegalBasisType legalBasisType = LegalBasisType.CONSENT;
    @NotBlank @Size(min = 5, max = 1000) private String legalBasisDetail;
    @NotNull private UUID retentionPolicyVersionId;
    @Valid
    @Size(min = 1, max = 50, message = "개인정보 항목은 1개 이상 50개 이하로 등록해 주세요.")
    private List<ProcessingDataItemForm> items =
            new ArrayList<>(List.of(new ProcessingDataItemForm(true)));
    public String getActivityName(){return activityName;} public void setActivityName(String v){activityName=v;}
    public UUID getOwnerOrganizationUnitId(){return ownerOrganizationUnitId;} public void setOwnerOrganizationUnitId(UUID v){ownerOrganizationUnitId=v;}
    public String getProcessingPurpose(){return processingPurpose;} public void setProcessingPurpose(String v){processingPurpose=v;}
    public LegalBasisType getLegalBasisType(){return legalBasisType;} public void setLegalBasisType(LegalBasisType v){legalBasisType=v;}
    public String getLegalBasisDetail(){return legalBasisDetail;} public void setLegalBasisDetail(String v){legalBasisDetail=v;}
    public UUID getRetentionPolicyVersionId(){return retentionPolicyVersionId;} public void setRetentionPolicyVersionId(UUID v){retentionPolicyVersionId=v;}
    public List<ProcessingDataItemForm> getItems(){return items;}
    public void setItems(List<ProcessingDataItemForm> items){this.items=items;}
}
