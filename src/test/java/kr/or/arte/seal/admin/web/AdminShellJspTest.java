package kr.or.arte.seal.admin.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminShellJspTest {
    @Test
    void everyStandardAdminPageUsesTheSharedHeaderAndSidebar() throws Exception {
        Path root = Path.of("src/main/webapp/WEB-INF/jsp/admin");
        List<Path> standardPages;
        try (var paths = Files.walk(root)) {
            standardPages = paths.filter(path -> path.toString().endsWith(".jsp"))
                    .filter(path -> read(path).contains("<body class=\"admin-body\""))
                    .toList();
        }

        assertThat(standardPages).isNotEmpty();
        assertThat(standardPages).allSatisfy(path -> {
            String jsp = read(path);
            assertThat(jsp).contains("fragments/admin/header.jsp", "fragments/admin/sidebar.jsp");
            assertThat(jsp).contains("<div class=\"admin-layout\">");
        });
    }

    @Test
    void sidebarUsesTheOriginalRequestPathForActiveMenuState() {
        String sidebar = read(Path.of("src/main/webapp/WEB-INF/jsp/fragments/admin/sidebar.jsp"));

        assertThat(sidebar).contains("jakarta.servlet.forward.request_uri");
        assertThat(sidebar).contains("pageContext.request.requestURI");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
