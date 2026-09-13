package kr.or.arte.seal.delivery.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.delivery.application.MailOutboxDetail;
import kr.or.arte.seal.delivery.application.MailOutboxQueryService;
import kr.or.arte.seal.delivery.application.MailOutboxStatus;
import kr.or.arte.seal.delivery.config.MailRelayProperties;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.recipient.application.RecipientDetail;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailOutboxControllerTest {

    @Test
    void detailExposesBusinessRequestCodeInsteadOfOnlyInternalId() {
        UUID actorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        MailOutboxQueryService mailQuery = mock(MailOutboxQueryService.class);
        ConsentRequestQueryService requestQuery = mock(ConsentRequestQueryService.class);
        RecipientQueryService recipientQuery = mock(RecipientQueryService.class);
        MailOutboxDetail mail = new MailOutboxDetail(outboxId, requestId, MailOutboxStatus.QUEUED,
                "개인정보 동의", Instant.parse("2026-08-24T09:20:58Z"), null, null, 0, "<p>메일</p>");
        ConsentRequestRecord request = mock(ConsentRequestRecord.class);
        when(mailQuery.require(actorId, outboxId)).thenReturn(mail);
        when(requestQuery.require(actorId, requestId)).thenReturn(request);
        when(request.requestCode()).thenReturn("CR-20260824-ABCDEFGH");
        UUID recipientId = UUID.randomUUID();
        when(request.recipientId()).thenReturn(recipientId);
        RecipientDetail recipient = new RecipientDetail(recipientId, UUID.randomUUID(), "예술교육팀", "교육팀장",
                "김*술", "ar****@example.com", "신청서 접수", RecipientStatus.ACTIVE, 0);
        when(recipientQuery.find(actorId, recipientId)).thenReturn(recipient);
        MailRelayProperties properties = new MailRelayProperties(false, "localhost", 25, false,
                false, "seal@arte.or.kr", "ARTE");
        MailOutboxController controller = new MailOutboxController(mailQuery, requestQuery, recipientQuery, properties);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.detail(outboxId,
                new AdminPrincipal(actorId, "admin", "관리자", UUID.randomUUID(), false), model);

        assertThat(view).isEqualTo("admin/mail/detail");
        assertThat(model.getAttribute("requestCode")).isEqualTo("CR-20260824-ABCDEFGH");
        assertThat(model.getAttribute("mailItem")).isSameAs(mail);
        assertThat(model.getAttribute("recipient")).isSameAs(recipient);
        assertThat(model.getAttribute("scheduleView")).isEqualTo(
                new MailSchedulePresentation("발송 처리 예정", Instant.parse("2026-08-24T09:20:58Z")));
    }
}
