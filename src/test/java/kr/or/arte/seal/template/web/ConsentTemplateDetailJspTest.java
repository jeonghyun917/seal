package kr.or.arte.seal.template.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentTemplateDetailJspTest {

    @Test
    void iteratesRecordVersionsThroughItsAccessor() throws Exception {
        String jsp = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/templates/detail.jsp"));

        assertThat(jsp)
                .contains("items=\"${template.versions()}\"")
                .doesNotContain("items=\"${template.versions}\"");
    }
}
