package kr.or.arte.seal.template.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentTemplateVersionTest {

    private static final UUID AUTHOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID APPROVER = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PUBLISHER = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final String HTML = "<p>개인정보 처리 내용을 확인해 주세요.</p>";
    private static final String HASH = "a".repeat(64);

    @Test
    void authorCannotApproveOwnTemplateVersion() {
        ConsentTemplateVersion requested = validDraft().requestReview(AUTHOR);

        assertThatThrownBy(() -> requested.approve(AUTHOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("작성자와 승인자는 달라야 합니다.");
    }

    @Test
    void optionalItemCannotBeRequired() {
        assertThatThrownBy(() -> item(ConsentItemType.OPTIONAL, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선택 동의 항목은 필수로 설정할 수 없습니다.");
    }

    @Test
    void onlyThirdPartyItemCanHaveRecipient() {
        assertThatThrownBy(() -> item(ConsentItemType.COLLECTION_USE, true, "제공받는 기관"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("제3자 제공 항목만 제공받는 자를 입력해야 합니다.");
    }

    @Test
    void publishedVersionCannotBeEdited() {
        ConsentTemplateVersion published = validDraft()
                .requestReview(AUTHOR)
                .approve(APPROVER)
                .publish(PUBLISHER, HASH);

        assertThatThrownBy(() -> published.changeContent("변경된 개인정보 동의서", HTML, List.of(validItem())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("초안 상태에서만 수정할 수 있습니다.");
    }

    @Test
    void followsReviewApprovalPublishAndRetireLifecycle() {
        ConsentTemplateVersion requested = validDraft().requestReview(AUTHOR);
        ConsentTemplateVersion approved = requested.approve(APPROVER);
        ConsentTemplateVersion published = approved.publish(PUBLISHER, HASH);
        ConsentTemplateVersion retired = published.retire(PUBLISHER);

        assertThat(requested.workflowStatus()).isEqualTo(TemplateWorkflowStatus.REVIEW_REQUESTED);
        assertThat(approved.workflowStatus()).isEqualTo(TemplateWorkflowStatus.APPROVED);
        assertThat(published.workflowStatus()).isEqualTo(TemplateWorkflowStatus.PUBLISHED);
        assertThat(published.contentHash()).isEqualTo(HASH);
        assertThat(retired.workflowStatus()).isEqualTo(TemplateWorkflowStatus.RETIRED);
    }

    @Test
    void rejectsDuplicateItemOrderAndCode() {
        ConsentItemDefinition first = validItem();
        ConsentItemDefinition duplicate = new ConsentItemDefinition(
                UUID.randomUUID(), 1, first.code(), ConsentItemType.OPTIONAL,
                "선택 정보 수집 이용 동의", false, "맞춤형 안내 제공 목적",
                "서비스 종료 후 1년", "동의하지 않아도 기본 서비스 이용에 제한이 없습니다.",
                null, List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> ConsentTemplateVersion.draft(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, "개인정보 수집 이용 동의서", HTML, List.of(first, duplicate), AUTHOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동의 항목 순서와 코드는 중복될 수 없습니다.");
    }

    private ConsentTemplateVersion validDraft() {
        return ConsentTemplateVersion.draft(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, "개인정보 수집 이용 동의서", HTML, List.of(validItem()), AUTHOR);
    }

    private ConsentItemDefinition validItem() {
        return item(ConsentItemType.COLLECTION_USE, true, null);
    }

    private ConsentItemDefinition item(ConsentItemType type, boolean required, String recipient) {
        return new ConsentItemDefinition(
                UUID.randomUUID(), 1, "COLLECT-GENERAL", type,
                "개인정보 수집 이용 동의", required, "교육 프로그램 신청 및 운영 목적",
                "이용 종료 후 3년", "동의를 거부할 수 있으나 서비스 제공이 제한될 수 있습니다.",
                recipient, List.of(UUID.randomUUID()));
    }
}
