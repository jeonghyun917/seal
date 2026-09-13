package kr.or.arte.seal.template.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConsentTemplateForm {
    @NotBlank @Size(max = 100)
    private String templateName;
    @NotNull
    private UUID processingActivityVersionId;
    @NotBlank @Size(min = 5, max = 200)
    private String title;
    @NotBlank @Size(min = 10, max = 8000)
    private String introductionHtml;
    @Valid @NotEmpty
    private List<ConsentItemForm> items = new ArrayList<>();
    private long templateVersionNo;
    private long contentVersionNo;

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public UUID getProcessingActivityVersionId() { return processingActivityVersionId; }
    public void setProcessingActivityVersionId(UUID processingActivityVersionId) {
        this.processingActivityVersionId = processingActivityVersionId;
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIntroductionHtml() { return introductionHtml; }
    public void setIntroductionHtml(String introductionHtml) { this.introductionHtml = introductionHtml; }
    public List<ConsentItemForm> getItems() { return items; }
    public void setItems(List<ConsentItemForm> items) { this.items = items; }
    public long getTemplateVersionNo() { return templateVersionNo; }
    public void setTemplateVersionNo(long templateVersionNo) { this.templateVersionNo = templateVersionNo; }
    public long getContentVersionNo() { return contentVersionNo; }
    public void setContentVersionNo(long contentVersionNo) { this.contentVersionNo = contentVersionNo; }
}
