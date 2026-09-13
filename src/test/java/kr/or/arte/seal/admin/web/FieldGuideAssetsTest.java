package kr.or.arte.seal.admin.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldGuideAssetsTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void sharedAdminHeaderLoadsTheFieldGuideComponent() throws Exception {
        String header = read(WEBAPP.resolve("WEB-INF/jsp/fragments/admin/header.jsp"));

        assertThat(header).contains("tagdir=\"/WEB-INF/tags\"");
        assertThat(header).contains("<guide:fieldGuides/>");
    }

    @Test
    void fieldGuideIsAccessibleAndSupportsMouseKeyboardAndTouch() throws Exception {
        String tag = read(WEBAPP.resolve("WEB-INF/tags/fieldGuides.tag"));
        String script = read(WEBAPP.resolve("resources/js/field-guide.js"));
        String css = read(WEBAPP.resolve("resources/css/app.css"));

        assertThat(tag).contains("field-guide.js");
        assertThat(script).contains("aria-expanded", "aria-describedby", "Escape", "pointerdown", "root.closest?.(\"form\")", "field-guide-label-line", "event.detail > 0", "matchMedia(\"(hover: hover)\")");
        assertThat(script).contains("작성 가이드", "예시)");
        assertThat(css).contains(".field-guide-trigger", ".field-guide-tooltip", ".field-guide-label-line", ":focus-within", "@media (hover: hover)");
    }

    @Test
    void guideCatalogCoversThePrimaryRegistrationFlows() throws Exception {
        String script = read(WEBAPP.resolve("resources/js/field-guide.js"));
        List<String> requiredGuideKeys = List.of(
                "organizationCode", "policyCode", "templateCode",
                "items[].purpose", "name", "collectionBasis", "recipientId",
                "expiresAt", "detail", "loginId", "roleCode", "permissionIds",
                "activityName", "ownerOrganizationUnitId", "retentionPolicyVersionId",
                "legalBasisType", "legalBasisDetail", "processingPurpose",
                "itemName#", "necessityReason#", "required#"
        );

        assertThat(requiredGuideKeys).allSatisfy(key -> assertThat(script).contains("\"" + key + "\""));
        assertThat(script).contains("정보주체 동의", "법령상 의무", "공공업무 수행", "계약 체결·이행", "기타");
    }

    @Test
    void activityTerminologyIsPlainAndExistingTooltipStructureIsPreserved() throws Exception {
        String form = read(WEBAPP.resolve("WEB-INF/jsp/admin/processing-activities/form.jsp"));
        String script = read(WEBAPP.resolve("resources/js/field-guide.js"));
        String rows = read(WEBAPP.resolve("resources/js/processing-activity-items.js"));
        assertThat(form).contains("개인정보 이용 업무 등록", "업무 이름", "개인정보 이용 근거",
                "근거 상세 설명", "개인정보 이용 목적", "수집·이용할 개인정보 항목",
                "이 항목이 필요한 이유");
        assertThat(form).doesNotContain("처리업무", "처리 근거", "처리 목적", "필요성 설명");
        assertThat(rows).contains("이 항목이 필요한 이유").doesNotContain("필요성 설명");
        assertThat(script).contains("field-guide-tooltip", "작성 가이드", "예시)",
                "개인정보를 어떤 업무에 사용하는지", "교육 신청 시 개인정보 수집·이용 동의를 받음");
        for (String view : List.of("fragments/admin/sidebar.jsp", "admin/processing-activities/list.jsp",
                "admin/processing-activities/detail.jsp", "admin/templates/form.jsp")) {
            assertThat(read(WEBAPP.resolve("WEB-INF/jsp/" + view)))
                    .contains("개인정보 이용 업무").doesNotContain("처리업무");
        }
    }
    private static String read(Path path) throws Exception {
        return Files.readString(path);
    }
}
