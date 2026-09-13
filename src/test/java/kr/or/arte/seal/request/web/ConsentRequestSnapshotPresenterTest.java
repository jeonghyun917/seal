package kr.or.arte.seal.request.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentRequestSnapshotPresenterTest {
    @Test
    void convertsImmutableSnapshotIntoReadableKoreanView() {
        String json = """
                {"schemaVersion":1,"institution":"한국문화예술교육진흥원","requestCode":"CR-20260827-TEST",
                 "processingActivityVersionId":"00000000-0000-0000-0000-000000000011",
                 "retentionPolicyVersionId":"00000000-0000-0000-0000-000000000012",
                 "retentionDisplayText":"동의 제출 후 1년","templateVersionId":"00000000-0000-0000-0000-000000000013",
                 "templateContentHash":"abc123","title":"교육 참여 개인정보 동의","introductionHtml":"<p>안내문입니다.</p>",
                 "expiresAt":"2026-09-10T09:00:00Z","withdrawalNotice":"언제든 철회할 수 있습니다.",
                 "processingActivityName":"교육 참여자 관리","processingPurpose":"교육 안내 및 운영",
                 "items":[{"order":1,"code":"COLLECT-USE","type":"COLLECTION_USE","title":"개인정보 수집·이용 동의",
                 "required":true,"purpose":"교육 안내","retentionText":"1년","refusalNotice":"거부 시 참여가 제한될 수 있습니다.",
                 "thirdPartyRecipient":"","dataItems":[{"id":"00000000-0000-0000-0000-000000000021","order":1,"name":"이름"},
                 {"id":"00000000-0000-0000-0000-000000000022","order":2,"name":"이메일"}]}]}
                """;

        var view = new ConsentRequestSnapshotPresenter(new ObjectMapper()).present(json);

        assertThat(view.title()).isEqualTo("교육 참여 개인정보 동의");
        assertThat(view.requestCode()).isEqualTo("CR-20260827-TEST");
        assertThat(view.expiresAtDisplay()).isEqualTo("2026-09-10 18:00");
        assertThat(view.items()).singleElement().satisfies(item -> {
            assertThat(item.typeLabel()).isEqualTo("개인정보 수집·이용");
            assertThat(item.required()).isTrue();
            assertThat(item.dataItemNames()).containsExactly("이름", "이메일");
        });
        assertThat(view.snapshotHash()).matches("[0-9a-f]{64}");
    }
}