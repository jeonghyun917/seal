package kr.or.arte.seal.consent.application;
import java.time.Instant;import java.util.List;
public record PublicConsentPage(String institution,String title,String introductionHtml,String processingActivityName,
        String processingPurpose,String retentionDisplayText,String withdrawalNotice,Instant expiresAt,List<PublicConsentItem> items){public PublicConsentPage{items=List.copyOf(items);}}
