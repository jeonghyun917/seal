package kr.or.arte.seal.delivery.infrastructure;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import kr.or.arte.seal.delivery.application.MailMessage;
import kr.or.arte.seal.delivery.config.MailRelayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaMailMessageIdTest {
    @Test
    void sendsTheProvidedStableMessageIdHeader() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        AtomicReference<String> sentMessageId = new AtomicReference<>();
        doAnswer(invocation -> {
            MimeMessage mimeMessage = invocation.getArgument(0);
            sentMessageId.set(mimeMessage.getHeader("Message-ID", null));
            return null;
        }).when(sender).send(any(MimeMessage.class));
        var properties = new MailRelayProperties(true, "relay.example", 25, false, false,
                "seal@arte.or.kr", "SEAL");

        new JavaMailTransport(sender, properties).send(
                new MailMessage("a@example.com", "subject", "<p>body</p>", "<seal-fixed@arte.or.kr>"));

        assertThat(sentMessageId.get()).isEqualTo("<seal-fixed@arte.or.kr>");
    }
}
