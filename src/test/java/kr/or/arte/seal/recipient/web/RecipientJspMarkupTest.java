package kr.or.arte.seal.recipient.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class RecipientJspMarkupTest {
    @Test
    void recipientTableViewsKeepValidJspAndHtmlTagOpeners() throws IOException {
        for (String relative : List.of("list.jsp", "import-preview.jsp")) {
            String content = Files.readString(Path.of(
                    "src/main/webapp/WEB-INF/jsp/admin/recipients", relative));

            assertThat(content).startsWith("<%@");
            assertThat(content).contains("<table", "<thead", "<tbody");
            assertThat(content).doesNotContain("t%@", "t!doctype", "tdiv", "tc:");
        }
    }

    @Test
    void recipientViewsShowJobTitleAndApprovedLabels() throws IOException {
        String form = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/recipients/form.jsp"));
        String list = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/recipients/list.jsp"));
        String detail = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/recipients/detail.jsp"));

        assertThat(form).contains("관리 기관", "path=\"jobTitle\"", "수신자 정보 등록 근거")
                .doesNotContain("관리 부서", "수신자 정보<br/>등록 근거");
        assertThat(list).contains("recipient.jobTitle()", "<th>직책</th>");
        assertThat(detail).contains("recipient.jobTitle()", "수신자 직책");
    }

    @Test
    void recipientListAcceptsAnEmailSubstringAsASearchKeyword() throws IOException {
        String list = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/recipients/list.jsp"));

        assertThat(list).contains("id=\"email\" type=\"search\"")
                .doesNotContain("id=\"email\" type=\"email\"");
    }
}
