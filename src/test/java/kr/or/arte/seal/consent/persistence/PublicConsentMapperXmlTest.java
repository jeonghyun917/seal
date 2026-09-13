package kr.or.arte.seal.consent.persistence;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
class PublicConsentMapperXmlTest {
 @Test void publicItemsComeFromFrozenRequestSnapshotTables(){
  String xml=read();
  assertThat(xml).contains("seal.consent_request_item_data","d.item_name","i.third_party_recipient")
          .doesNotContain("consent_template_item i");
 }
 @Test void idempotentReceiptUsesTheOriginalSubmissionTime(){assertThat(read()).contains("TRUE existing,submitted_at");}
 private static String read(){try{return Files.readString(Path.of("src/main/resources/mybatis/mappers/consent/PublicConsentMapper.xml"),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException(e);}}
}
