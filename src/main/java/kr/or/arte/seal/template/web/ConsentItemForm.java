package kr.or.arte.seal.template.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.or.arte.seal.template.domain.ConsentItemType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConsentItemForm {
    @NotBlank @Pattern(regexp = "^[A-Z0-9-]{2,30}$")
    private String code;
    @NotNull
    private ConsentItemType type = ConsentItemType.COLLECTION_USE;
    @NotBlank @Size(min = 5, max = 200)
    private String title;
    private boolean required = true;
    @NotBlank @Size(min = 5, max = 1000)
    private String purpose;
    @Size(max = 1000)
    private String retentionText = "보유정책에서 자동 적용";
    @NotBlank @Size(min = 10, max = 1000)
    private String refusalNotice;
    @Size(max = 500)
    private String thirdPartyRecipient;
    @NotEmpty
    private List<UUID> processingDataItemIds = new ArrayList<>();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public ConsentItemType getType() { return type; }
    public void setType(ConsentItemType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getRetentionText() { return retentionText; }
    public void setRetentionText(String retentionText) { this.retentionText = retentionText; }
    public String getRefusalNotice() { return refusalNotice; }
    public void setRefusalNotice(String refusalNotice) { this.refusalNotice = refusalNotice; }
    public String getThirdPartyRecipient() { return thirdPartyRecipient; }
    public void setThirdPartyRecipient(String thirdPartyRecipient) { this.thirdPartyRecipient = thirdPartyRecipient; }
    public List<UUID> getProcessingDataItemIds() { return processingDataItemIds; }
    public void setProcessingDataItemIds(List<UUID> processingDataItemIds) {
        this.processingDataItemIds = processingDataItemIds;
    }
}
