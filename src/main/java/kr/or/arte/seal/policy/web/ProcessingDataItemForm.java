package kr.or.arte.seal.policy.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProcessingDataItemForm {
    @NotBlank(message = "항목명을 입력해 주세요.")
    @Size(max = 100, message = "항목명은 100자 이하로 입력해 주세요.")
    private String itemName;

    @NotBlank(message = "필요한 이유를 입력해 주세요.")
    @Size(max = 500, message = "필요한 이유는 500자 이하로 입력해 주세요.")
    private String necessityReason;

    private boolean required;

    public ProcessingDataItemForm() {
    }

    public ProcessingDataItemForm(boolean required) {
        this.required = required;
    }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getNecessityReason() { return necessityReason; }
    public void setNecessityReason(String necessityReason) { this.necessityReason = necessityReason; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
