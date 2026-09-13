package kr.or.arte.seal.retention.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.common.web.AdminPageResult;
import kr.or.arte.seal.recipient.application.RecipientDetail;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.request.application.ConsentRequestSummary;
import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import kr.or.arte.seal.retention.application.RetentionActorNames;
import kr.or.arte.seal.retention.application.RetentionJobService;
import kr.or.arte.seal.template.application.PublishedConsentTemplateOption;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RetentionJobControllerTest {
    @Test void requestOptionsReturnScopedPagedReadableItems() {
        UUID actor=UUID.randomUUID(), requestId=UUID.randomUUID(), recipientId=UUID.randomUUID(), versionId=UUID.randomUUID();
        RetentionJobService jobs=mock(RetentionJobService.class);
        RecipientQueryService recipients=mock(RecipientQueryService.class);
        ConsentRequestQueryService requests=mock(ConsentRequestQueryService.class);
        PublishedConsentTemplateQuery templates=mock(PublishedConsentTemplateQuery.class);
        var summary=new ConsentRequestSummary(requestId,"CR-2026-001",recipientId, ConsentRequestStatus.ACTIVE,
                Instant.now(),Instant.now(),1);
        when(requests.searchPage(actor,null,"교육",2,20)).thenReturn(new AdminPageResult<>(List.of(summary),2,20,true));
        ConsentRequestRecord record=mock(ConsentRequestRecord.class); when(record.templateVersionId()).thenReturn(versionId);
        when(requests.require(actor,requestId)).thenReturn(record);
        when(recipients.find(actor,recipientId)).thenReturn(new RecipientDetail(recipientId,UUID.randomUUID(),"교육팀","담당",
                "김*나","ki***@example.com","업무", RecipientStatus.ACTIVE,0));
        when(templates.findVersionOption(actor,versionId)).thenReturn(Optional.of(
                new PublishedConsentTemplateOption(versionId,"TPL","교육 동의서","교육",1)));
        var controller=new RetentionJobController(jobs,recipients,requests,templates);

        var result=controller.requestOptions("교육",2,principal(actor));

        assertThat(result.page()).isEqualTo(2); assertThat(result.hasNext()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item->{assertThat(item.requestId()).isEqualTo(requestId);
            assertThat(item.requestCode()).isEqualTo("CR-2026-001"); assertThat(item.recipientLabel()).isEqualTo("김*나 · 교육팀");
            assertThat(item.templateName()).isEqualTo("교육 동의서");});
        verify(requests).searchPage(actor,null,"교육",2,20);
    }

    @Test void detailPublishesScopedActorDisplayNames() {
        UUID actor=UUID.randomUUID(), jobId=UUID.randomUUID();
        RetentionJobService jobs=mock(RetentionJobService.class); RecipientQueryService recipients=mock(RecipientQueryService.class);
        ConsentRequestQueryService requests=mock(ConsentRequestQueryService.class); PublishedConsentTemplateQuery templates=mock(PublishedConsentTemplateQuery.class);
        var item=mock(kr.or.arte.seal.retention.application.RetentionJobRecord.class);
        when(item.recipientId()).thenReturn(UUID.randomUUID()); when(item.consentRequestId()).thenReturn(UUID.randomUUID());
        when(jobs.require(actor,jobId)).thenReturn(item);
        when(requests.require(actor,item.consentRequestId())).thenReturn(mock(ConsentRequestRecord.class));
        var names=new RetentionActorNames("요청 담당자","승인 담당자","실행 담당자",null);
        when(jobs.actorNames(actor,jobId)).thenReturn(names);
        var model=new ConcurrentModel();

        new RetentionJobController(jobs,recipients,requests,templates).detail(jobId,principal(actor),model);

        assertThat(model.getAttribute("actorNames")).isEqualTo(names);
    }

    private static AdminPrincipal principal(UUID actor){return new AdminPrincipal(actor,"retention","보존 관리자",UUID.randomUUID(),false);}
}
