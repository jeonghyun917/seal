package kr.or.arte.seal.evidence.infrastructure;

import kr.or.arte.seal.evidence.application.EvidenceWorkItem;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenHtmlEvidencePdfRendererTest {
    @Test
    void rendersAllConsentInformationInReadableKorean() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String snapshot = """
                {
                  "institution":"한국문화예술교육진흥원",
                  "title":"교육 프로그램 개인정보 동의",
                  "processingActivityName":"교육 프로그램 신청 접수",
                  "processingPurpose":"교육 신청 확인과 안내 메일 발송",
                  "retentionDisplayText":"수신자 응답 제출일로부터 1년",
                  "introductionHtml":"<p>신청 처리를 위해 다음 개인정보를 처리합니다.</p>",
                  "withdrawalNotice":"담당자에게 요청하여 언제든 동의를 철회할 수 있습니다.",
                  "templateContentHash":"template-content-sha256",
                  "items":[
                    {
                      "title":"개인정보 수집·이용 동의",
                      "type":"COLLECTION_USE",
                      "required":true,
                      "purpose":"교육 신청자 식별 및 안내",
                      "retentionText":"응답 제출일로부터 1년",
                      "refusalNotice":"동의를 거부할 수 있으나 신청이 제한될 수 있습니다.",
                      "dataItems":["이름","이메일","소속","직책"]
                    },
                    {
                      "title":"제3자 제공 동의",
                      "type":"THIRD_PARTY_PROVISION",
                      "required":false,
                      "purpose":"교육 운영 지원",
                      "retentionText":"프로그램 종료 시까지",
                      "refusalNotice":"동의하지 않아도 기본 서비스는 이용할 수 있습니다.",
                      "thirdPartyRecipient":"교육 운영 대행사",
                      "dataItems":["이름","이메일"]
                    }
                  ]
                }
                """;
        EvidenceWorkItem item = new EvidenceWorkItem(
                packageId, UUID.randomUUID(), requestId, UUID.randomUUID(),
                "CR-20260903-ABCDEFGH", "FORM-2026-001", "교육 프로그램 동의서", 3,
                "홍길동", "hong@example.org", "교육기획팀", "과장",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:01:02Z"),
                Instant.parse("2026-09-08T04:13:02Z"),
                snapshot, "request-snapshot-sha256", "SUBMITTED",
                Instant.parse("2026-09-03T04:13:02Z"),
                List.of("개인정보 수집·이용 동의: AGREE", "제3자 제공 동의: DISAGREE"),
                List.of("CREATED @ 2026-09-01T00:00:00Z", "SUBMITTED @ 2026-09-03T04:13:02Z"),
                1, 1);

        byte[] pdf = new OpenHtmlEvidencePdfRenderer().render(item);

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("개인정보 동의 증적")
                    .contains("동의 요청 정보")
                    .contains("수신자 정보")
                    .contains("개인정보 처리 안내")
                    .contains("항목별 동의 내용 및 결정")
                    .contains("처리 이력")
                    .contains("증적 정보")
                    .contains("한국문화예술교육진흥원")
                    .contains("CR-20260903-ABCDEFGH")
                    .contains("교육 프로그램 동의서")
                    .contains("홍길동")
                    .contains("hong@example.org")
                    .contains("교육기획팀")
                    .contains("2026-09-08 13:13:02")
                    .contains("교육 신청 확인과 안내 메일 발송")
                    .contains("이름, 이메일, 소속, 직책")
                    .contains("동의를 거부할 수 있으나")
                    .contains("교육 운영 대행사")
                    .contains("동의")
                    .contains("미동의")
                    .contains(packageId.toString())
                    .contains(requestId.toString())
                    .contains("request-snapshot-sha256")
                    .doesNotContain("AGREE")
                    .doesNotContain("DISAGREE");
        }
    }
}
