package kr.or.arte.seal.request.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.arte.seal.template.application.PublishedTemplateView;
import kr.or.arte.seal.template.domain.ConsentItemType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequestSnapshotFactoryTest {
    @Test
    void freezesCanonicalContentWithoutDirectIdentifiers() {
        RequestSnapshotFactory factory = new RequestSnapshotFactory(new ObjectMapper());
        RequestSnapshotSource source = new RequestSnapshotSource("한국문화예술교육진흥원",
                "CR-20260824-ABCDEFGH", UUID.randomUUID(), UUID.randomUUID(),
                "동의 제출 후 3년", UUID.randomUUID(), "a".repeat(64),
                Instant.parse("2026-08-25T04:00:00Z"), "동의 철회는 담당 부서에 요청할 수 있습니다.",
                new PublishedTemplateView("교육 개인정보 동의서", "<p>내용을 확인하세요.</p>",
                        "교육 프로그램", "교육 프로그램 운영", List.of(
                        new PublishedTemplateView.Item(1, "COLLECT", ConsentItemType.COLLECTION_USE,
                                "개인정보 수집 이용 동의", true, "교육 프로그램 운영 목적",
                                "동의 제출 후 3년", "거부 시 신청이 제한될 수 있습니다.", null,
                                List.of("이름", "이메일")))));

        RequestSnapshot snapshot = factory.create(source);

        assertThat(snapshot.canonicalJson()).contains("templateContentHash", "COLLECT");
        assertThat(snapshot.canonicalJson()).doesNotContain("홍길동", "p@example.test");
        assertThat(snapshot.snapshotHash()).matches("[0-9a-f]{64}");
        assertThat(snapshot.items()).extracting(RequestSnapshot.Item::order).containsExactly(1);
    }
}
