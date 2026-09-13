package kr.or.arte.seal.policy.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionPolicyFormJspTest {
    @Test
    void explainsSubmissionDateFallbackAndDestructionScope() throws Exception {
        String jsp = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/retention-policies/form.jsp"));

        assertThat(jsp)
                .contains("응답한 수신자", "실제 응답 제출일부터")
                .contains("미응답 수신자", "동의 요청 만료일부터")
                .contains("응답한 수신자 파기 항목", "미응답 수신자 파기 항목")
                .contains("수신자 이름·이메일·소속·직책")
                .contains("저장된 발송 이메일 주소와 메일 본문")
                .contains("생성된 동의 증적 파일")
                .contains("응답·증적 파일은 생성되지 않으므로 파기 대상 없음")
                .contains("파기 방식 선택 가이드")
                .contains("개인정보 삭제", "일반적인 선택")
                .contains("익명 처리", "통계 이력 유지")
                .contains("파일 완전 삭제", "증적 파일 포함")
                .contains("어떤 방식을 선택해도 이름·이메일 등 개인정보")
                .contains("최소 감사기록은 파기 증명과 이력 추적을 위해 남을 수 있습니다");
    }

    @Test
    void groupsRetentionValueAndUnitTogether() throws Exception {
        String jsp = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/jsp/admin/retention-policies/form.jsp"));

        int groupStart = jsp.indexOf("<div class=\"retention-period-fields\">");
        int groupEnd = jsp.indexOf("</div>\n<aside class=\"retention-scope-note", groupStart);

        assertThat(groupStart).isGreaterThanOrEqualTo(0);
        assertThat(groupEnd).isGreaterThan(groupStart);
        assertThat(jsp.substring(groupStart, groupEnd))
                .contains("path=\"periodValue\"", "path=\"periodUnit\"");
    }
}
