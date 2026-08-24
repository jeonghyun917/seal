package kr.or.arte.seal.delivery.application;

import com.fasterxml.jackson.databind.ObjectMapper;import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConsentMailRendererTest{
 @Test void rendersSnapshotWithoutFormsScriptsTrackingOrExternalResources(){String snapshot="""
 {"institution":"한국문화예술교육진흥원","requestCode":"CR-20260824-ABCDEFGH","title":"교육 참여 동의",
 "introductionHtml":"<p>안내문</p>","processingActivityName":"교육 신청","processingPurpose":"참여자 운영",
 "retentionDisplayText":"이용 종료 후 3년","withdrawalNotice":"담당 부서에 철회 요청","expiresAt":"2026-08-31T00:00:00Z",
 "items":[{"title":"필수 수집","purpose":"운영","retentionText":"3년","refusalNotice":"거부 가능","required":true}]}
 """;var result=new ConsentMailRenderer(new ObjectMapper()).render(snapshot,"김예술","https://consent.arte.or.kr/consent/safe-token");
  assertThat(result.html()).contains("교육 참여 동의","김예술","https://consent.arte.or.kr/consent/safe-token");
  assertThat(result.html()).doesNotContain("<form","<script","<img","tracking");assertThat(result.sha256()).hasSize(64);
 }
}
