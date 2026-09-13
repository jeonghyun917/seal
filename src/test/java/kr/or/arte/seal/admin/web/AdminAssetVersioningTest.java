package kr.or.arte.seal.admin.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAssetVersioningTest {
    @Test
    void everyAdminDataStylesheetReferenceHasACacheBustingVersion() throws Exception {
        Path root = Path.of("src/main/webapp/WEB-INF/jsp/admin");
        List<Path> pages;
        try (var paths = Files.walk(root)) {
            pages = paths.filter(path -> path.toString().endsWith(".jsp"))
                    .filter(path -> read(path).contains("admin-data.css"))
                    .toList();
        }

        assertThat(pages).isNotEmpty();
        assertThat(pages).allSatisfy(path ->
                assertThat(read(path)).containsPattern("admin-data\\.css\\?v=[0-9-]+"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}