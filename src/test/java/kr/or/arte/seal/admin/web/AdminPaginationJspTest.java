package kr.or.arte.seal.admin.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPaginationJspTest {
    @Test
    void linksUseTheOriginalControllerPathAfterJspForward() throws Exception {
        String jsp = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/fragments/admin/pagination.jsp"));

        assertThat(jsp).contains("jakarta.servlet.forward.servlet_path");
        assertThat(jsp).contains("value=\"${paginationPath}\"");
        assertThat(jsp).doesNotContain(
                "previousPageUrl\" value=\"${pageContext.request.servletPath}\"",
                "nextPageUrl\" value=\"${pageContext.request.servletPath}\"");
    }
}
