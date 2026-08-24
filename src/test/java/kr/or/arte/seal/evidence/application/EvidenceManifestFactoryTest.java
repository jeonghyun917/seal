package kr.or.arte.seal.evidence.application;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceManifestFactoryTest {
    @Test
    void createsStableCanonicalManifestRegardlessOfInputOrder() {
        var first = new LinkedHashMap<String, byte[]>();
        first.put("request.json", "요청".getBytes(StandardCharsets.UTF_8));
        first.put("decisions.json", "결정".getBytes(StandardCharsets.UTF_8));
        var second = new LinkedHashMap<String, byte[]>();
        second.put("decisions.json", "결정".getBytes(StandardCharsets.UTF_8));
        second.put("request.json", "요청".getBytes(StandardCharsets.UTF_8));

        EvidenceManifestFactory factory = new EvidenceManifestFactory();

        assertThat(factory.create(first).canonicalJson()).isEqualTo(factory.create(second).canonicalJson());
        assertThat(new String(factory.create(first).canonicalJson(), StandardCharsets.UTF_8))
                .contains("decisions.json").contains("request.json").contains("sha256");
    }
}
