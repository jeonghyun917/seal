package kr.or.arte.seal.rights.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.or.arte.seal.rights.domain.RightsRequestType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class RightsRequestForm {
    private static final Set<RightsRequestType> CONSENT_REQUIRED = Set.of(
            RightsRequestType.ERASURE, RightsRequestType.RESTRICTION, RightsRequestType.WITHDRAWAL);

    private UUID organizationId;
    @NotNull private UUID recipientId;
    private UUID consentRequestId;
    @NotNull private RightsRequestType type;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate receivedDate;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate dueDate;
    @NotBlank @Size(max = 2000) private String detail;

    @AssertTrue(message = "처리기한은 접수일보다 빠를 수 없습니다.")
    public boolean isDueDateValid() {
        return receivedDate == null || dueDate == null || !dueDate.isBefore(receivedDate);
    }

    @AssertTrue(message = "삭제·처리제한·동의철회 요청은 동의요청 연결이 필요합니다.")
    public boolean isConsentRequestValid() {
        return type == null || !CONSENT_REQUIRED.contains(type) || consentRequestId != null;
    }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getRecipientId() { return recipientId; }
    public void setRecipientId(UUID recipientId) { this.recipientId = recipientId; }
    public UUID getConsentRequestId() { return consentRequestId; }
    public void setConsentRequestId(UUID consentRequestId) { this.consentRequestId = consentRequestId; }
    public RightsRequestType getType() { return type; }
    public void setType(RightsRequestType type) { this.type = type; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
