package kr.or.arte.seal.recipient.importing;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("seal.recipient-import")
public record RecipientImportProperties(int maxRows,int maxBytes) {}
