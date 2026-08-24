package kr.or.arte.seal.template.application;

import kr.or.arte.seal.policy.application.activity.ApprovedDataItemReference;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityReference;
import kr.or.arte.seal.template.domain.ConsentItemDefinition;
import kr.or.arte.seal.template.domain.ConsentItemType;
import kr.or.arte.seal.template.domain.ConsentTemplateVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TemplatePreviewRendererTest {

    @Test
    void previewUsesStructuredItemAndProcessingDataOrder() {
        UUID firstData = UUID.randomUUID();
        UUID secondData = UUID.randomUUID();
        ConsentItemDefinition second = item(2, "OPTIONAL", false, List.of(secondData));
        ConsentItemDefinition first = item(1, "REQUIRED", true, List.of(firstData, secondData));
        ConsentTemplateVersion version = ConsentTemplateVersion.draft(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, "교육 프로그램 개인정보 동의서",
                "<p>개인정보 처리 내용을 확인해 주세요.</p>", List.of(second, first), UUID.randomUUID());
        ConsentTemplateDetail detail = new ConsentTemplateDetail(version.consentTemplateId(),
                "TPL-EDU", "교육 프로그램 동의서", version.processingActivityId(),
                "교육 프로그램 운영", "ACTIVE", null, 0, version);
        ApprovedProcessingActivityReference policy = new ApprovedProcessingActivityReference(
                version.processingActivityId(), version.processingActivityVersionId(), UUID.randomUUID(),
                "교육 프로그램 운영", "교육 프로그램 참여자 등록 및 운영 목적", "CONSENT",
                UUID.randomUUID(), "동의 제출 후 3년",
                List.of(new ApprovedDataItemReference(firstData, 1, "성명", true),
                        new ApprovedDataItemReference(secondData, 2, "이메일", true)));

        PublishedTemplateView view = new TemplatePreviewRenderer().toView(detail, policy);

        assertThat(view.items()).extracting(PublishedTemplateView.Item::order)
                .containsExactly(1, 2);
        assertThat(view.items().get(0).dataItemNames()).containsExactly("성명", "이메일");
        assertThat(view.processingPurpose()).isEqualTo("교육 프로그램 참여자 등록 및 운영 목적");
    }

    private ConsentItemDefinition item(int order, String code, boolean required, List<UUID> dataIds) {
        return new ConsentItemDefinition(UUID.randomUUID(), order, code,
                required ? ConsentItemType.COLLECTION_USE : ConsentItemType.OPTIONAL,
                required ? "필수 개인정보 수집 동의" : "선택 개인정보 수집 동의", required,
                "교육 프로그램 운영 목적", "동의 제출 후 3년",
                "동의를 거부할 수 있으며 일부 서비스가 제한될 수 있습니다.", null, dataIds);
    }
}
