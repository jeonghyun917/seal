package kr.or.arte.seal.request.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ConsentRequestForm {
    @NotNull private UUID recipientId;
    @NotNull private UUID templateVersionId;
    @NotBlank private String expiresAt;
    @NotNull private UUID idempotencyKey = UUID.randomUUID();
    private long versionNo;
    public UUID getRecipientId(){return recipientId;} public void setRecipientId(UUID v){recipientId=v;}
    public UUID getTemplateVersionId(){return templateVersionId;} public void setTemplateVersionId(UUID v){templateVersionId=v;}
    public String getExpiresAt(){return expiresAt;} public void setExpiresAt(String v){expiresAt=v;}
    public UUID getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(UUID v){idempotencyKey=v;}
    public long getVersionNo(){return versionNo;} public void setVersionNo(long v){versionNo=v;}
}
