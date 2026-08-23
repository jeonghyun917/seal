package kr.or.arte.seal.template.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateContentHasherTest {

    private final TemplateContentHasher hasher = new TemplateContentHasher(new ObjectMapper());

    @Test
    void hashIsStableAcrossMapInsertionOrderAndCrLf() {
        Map<String, Object> reversed = new LinkedHashMap<>();
        reversed.put("title", "동의서\r\n제목");
        reversed.put("items", List.of(Map.of("order", 1, "name", "이메일")));

        Map<String, Object> sorted = new LinkedHashMap<>();
        sorted.put("items", List.of(Map.of("name", "이메일", "order", 1)));
        sorted.put("title", "동의서\n제목");

        assertThat(hasher.sha256(reversed)).isEqualTo(hasher.sha256(sorted));
    }

    @Test
    void semanticallyDifferentContentHasDifferentHash() {
        assertThat(hasher.sha256(Map.of("title", "동의서 A")))
                .isNotEqualTo(hasher.sha256(Map.of("title", "동의서 B")));
    }

    @Test
    void returnsLowercaseSha256Hex() {
        assertThat(hasher.sha256(Map.of("title", "동의서")))
                .matches("[0-9a-f]{64}");
    }
}
