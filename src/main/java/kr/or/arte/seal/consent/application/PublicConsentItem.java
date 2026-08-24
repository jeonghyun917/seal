package kr.or.arte.seal.consent.application;
import java.util.UUID;
public record PublicConsentItem(UUID itemId,int order,String title,boolean required,String purpose,String retentionText,String refusalNotice) {}
