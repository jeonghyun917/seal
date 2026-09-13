package kr.or.arte.seal.consent.web;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
class PublicConsentJspTest {
 @Test void responseShowsFrozenPersonalDataAndRecipientAndRestoresSelection() throws Exception {String jsp=read("respond.jsp");assertThat(jsp).contains("개인정보 항목","제공받는 자","decisions[item.itemId()] == 'AGREE'","decisions[item.itemId()] == 'DISAGREE'");}
 @Test void completionShowsReceiptTitleTimeDecisionsAndWithdrawalGuidance() throws Exception {String jsp=read("complete.jsp");assertThat(jsp).contains("receipt.title()","receipt.submittedAtLabel()","receipt.items()","receipt.withdrawalNotice()");}
 private static String read(String name)throws Exception{return Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/consent",name),StandardCharsets.UTF_8);}
}
