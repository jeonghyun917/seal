package kr.or.arte.seal.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class StatusLabelViewCoverageTest {
    private static final Pattern RAW_STATUS_OUTPUT = Pattern.compile(
            "<c:out\\s+value=\\\"\\$\\{(?!seal:statusLabel)[^}]*(?:status|Status)\\(\\)[^}]*}\\\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_STATUS_OPTION = Pattern.compile(
            ">\\$\\{(?:s|state)}(?:</c:out>)?</option>", Pattern.CASE_INSENSITIVE);

    @Test
    void adminViewsDoNotRenderRawStatusCodes() throws IOException {
        Path views = Path.of("src/main/webapp/WEB-INF/jsp/admin");
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(views)) {
            files.filter(path -> path.toString().endsWith(".jsp")).forEach(path -> {
                try {
                    String content = Files.readString(path);
                    if (RAW_STATUS_OUTPUT.matcher(content).find() || RAW_STATUS_OPTION.matcher(content).find()) {
                        violations.add(views.relativize(path).toString());
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }

        assertThat(violations).as("영문 상태 코드를 직접 출력하는 관리자 JSP").isEmpty();
    }
}
