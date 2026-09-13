package kr.or.arte.seal.admin.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUiAccessibilityAssetsTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void sharedAdminChromeSupportsSkipNavigationAndMobileDrawer() throws Exception {
        String header = read("WEB-INF/jsp/fragments/admin/header.jsp");
        String sidebar = read("WEB-INF/jsp/fragments/admin/sidebar.jsp");
        String script = read("resources/js/admin-ui.js");

        assertThat(header).contains("class=\"skip-link\"", "class=\"admin-menu-button\"",
                "aria-controls=\"adminPrimaryNav\"", "admin-ui.js?v=");
        assertThat(sidebar).contains("id=\"adminPrimaryNav\"", "tabindex=\"-1\"");
        assertThat(script).contains("admin-main", "aria-current", "admin-nav-open", "Escape");
    }

    @Test
    void commonUiConnectsErrorsAndDialogNamesForAssistiveTechnology() throws Exception {
        String script = read("resources/js/admin-ui.js");
        String css = read("resources/css/app.css");

        assertThat(script).contains("aria-describedby", "aria-invalid",
                "role", "alert", "aria-busy", "aria-labelledby", "returnFocus")
                .doesNotContain("form-error-summary", "form.prepend(summary)");
        assertThat(css).doesNotContain(".form-error-summary");
    }

    @Test
    void compactLayoutAvoidsBrowserZoomAndKeepsReadableInteractionSizes() throws Exception {
        String css = read("resources/css/app.css");

        assertThat(css).doesNotContain("zoom:");
        assertThat(css).contains("--admin-control-min-size: 44px", ".admin-main {",
                "width: 100%", "width: min(320px, calc(100vw - 48px))",
                "@media (prefers-reduced-motion: reduce)");
        for (String stylesheet : List.of("app.css", "admin-data.css", "public-consent.css",
                "recipient-request.css", "statistics.css", "template.css", "retention.css")) {
            assertThat(read("resources/css/" + stylesheet))
                    .as(stylesheet + " must not render helper text below 12px")
                    .doesNotContain("font-size: .68rem", "font-size:.68rem", "font-size: .7rem",
                            "font-size:.7rem", "font-size: .72rem", "font-size:.72rem",
                            "font-size: .74rem", "font-size:.74rem");
        }
    }

    @Test
    void publicConsentChoicesExposeAVisibleSelectedState() throws Exception {
        String css = read("resources/css/public-consent.css");

        assertThat(css).contains(".choices label:has(input:checked)", "선택됨",
                ".choices label:has(input:focus-visible)", "prefers-reduced-motion");
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
