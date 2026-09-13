package kr.or.arte.seal.common.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDateTimeFormattingTest {
    private static final Pattern RAW_INSTANT_OUTPUT = Pattern.compile(
            "value=\\\"\\$\\{[A-Za-z][A-Za-z0-9]*\\.[A-Za-z][A-Za-z0-9]*At\\(\\)\\}\\\"");

    @Test
    void adminPagesDoNotRenderInstantAccessorsDirectly() throws Exception {
        Path root = Path.of("src/main/webapp/WEB-INF/jsp/admin");
        try (var files = Files.walk(root)) {
            var violations = files.filter(path -> path.toString().endsWith(".jsp"))
                    .filter(path -> {
                        try {
                            return RAW_INSTANT_OUTPUT.matcher(Files.readString(path)).find();
                        } catch (java.io.IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    })
                    .map(root::relativize)
                    .toList();

            assertThat(violations).isEmpty();
        }
    }
}
