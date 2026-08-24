package kr.or.arte.seal.recipient.importing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RecipientImportProperties.class,ClamAvProperties.class})
public class RecipientImportConfiguration {
    @Bean RecipientWorkbookParser recipientWorkbookParser(RecipientImportProperties p){return new RecipientWorkbookParser(p.maxRows());}
    @Bean UploadMalwareScanner uploadMalwareScanner(ClamAvProperties p){return p.enabled()?new ClamAvUploadMalwareScanner(p):new DisabledUploadMalwareScanner();}
}
