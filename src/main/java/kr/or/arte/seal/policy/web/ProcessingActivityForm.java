package kr.or.arte.seal.policy.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.or.arte.seal.policy.domain.activity.LegalBasisType;

import java.util.UUID;

public class ProcessingActivityForm {
    @NotBlank @Pattern(regexp = "^[A-Z0-9-]{2,30}$") private String activityCode;
    @NotBlank @Size(max = 100) private String activityName;
    @NotNull private UUID ownerOrganizationUnitId;
    @NotBlank @Size(min = 10, max = 1000) private String processingPurpose;
    @NotNull private LegalBasisType legalBasisType = LegalBasisType.CONSENT;
    @NotBlank @Size(min = 5, max = 1000) private String legalBasisDetail;
    @NotNull private UUID retentionPolicyVersionId;
    @NotBlank @Size(max = 100) private String itemName1;
    @NotBlank @Size(max = 500) private String necessityReason1;
    private boolean required1 = true;
    @Size(max = 100) private String itemName2;
    @Size(max = 500) private String necessityReason2;
    private boolean required2;
    @Size(max = 100) private String itemName3;
    @Size(max = 500) private String necessityReason3;
    private boolean required3;
    public String getActivityCode(){return activityCode;} public void setActivityCode(String v){activityCode=v;}
    public String getActivityName(){return activityName;} public void setActivityName(String v){activityName=v;}
    public UUID getOwnerOrganizationUnitId(){return ownerOrganizationUnitId;} public void setOwnerOrganizationUnitId(UUID v){ownerOrganizationUnitId=v;}
    public String getProcessingPurpose(){return processingPurpose;} public void setProcessingPurpose(String v){processingPurpose=v;}
    public LegalBasisType getLegalBasisType(){return legalBasisType;} public void setLegalBasisType(LegalBasisType v){legalBasisType=v;}
    public String getLegalBasisDetail(){return legalBasisDetail;} public void setLegalBasisDetail(String v){legalBasisDetail=v;}
    public UUID getRetentionPolicyVersionId(){return retentionPolicyVersionId;} public void setRetentionPolicyVersionId(UUID v){retentionPolicyVersionId=v;}
    public String getItemName1(){return itemName1;} public void setItemName1(String v){itemName1=v;} public String getNecessityReason1(){return necessityReason1;} public void setNecessityReason1(String v){necessityReason1=v;} public boolean isRequired1(){return required1;} public void setRequired1(boolean v){required1=v;}
    public String getItemName2(){return itemName2;} public void setItemName2(String v){itemName2=v;} public String getNecessityReason2(){return necessityReason2;} public void setNecessityReason2(String v){necessityReason2=v;} public boolean isRequired2(){return required2;} public void setRequired2(boolean v){required2=v;}
    public String getItemName3(){return itemName3;} public void setItemName3(String v){itemName3=v;} public String getNecessityReason3(){return necessityReason3;} public void setNecessityReason3(String v){necessityReason3=v;} public boolean isRequired3(){return required3;} public void setRequired3(boolean v){required3=v;}
}
