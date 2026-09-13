package kr.or.arte.seal.request.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class AdminContextJspTest {
    @Test void listsExposeQAndMaskedBusinessContext() throws Exception {
        String requests = jsp("requests/list.jsp");
        String mail = jsp("mail/list.jsp");
        assertThat(requests).contains("name=\"q\"", "maskedName()", "maskedEmail()", "templateName()");
        assertThat(mail).contains("name=\"q\"", "maskedName()", "maskedEmail()", "requestCode()");
    }

    @Test void detailsExposeScopedContextAndConfirmationContract() throws Exception {
        assertThat(jsp("requests/detail.jsp")).contains("/admin/evidence/request/${request.requestId()}");
        assertThat(jsp("retention/detail.jsp")).contains("파기 대상", "파기 결과",
                "data-confirm-title", "data-confirm-message");
        assertThat(jsp("rights/detail.jsp")).contains("대상자", "접수일 / 처리기한", "rights-type");
        assertThat(jsp("evidence/detail.jsp")).contains("증적 핵심 정보", "동의서명", "동의 요청번호",
                "증적 상태", "생성 요청 일시", "생성 완료 일시", "포함 파일", "<details", "기술 정보 보기");
    }

    private static String jsp(String name) throws Exception {
        return Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/admin").resolve(name));
    }
}
