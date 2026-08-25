package kr.or.arte.seal.delivery.infrastructure;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import kr.or.arte.seal.delivery.application.MailMessage;
import kr.or.arte.seal.delivery.application.MailTransportException;
import kr.or.arte.seal.delivery.config.MailRelayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaMailTransportTest {
    @Test
    void classifiesSmtp451AsTemporary() {
        MailTransportException exception = sendFailure("451 temporary failure");
        assertThat(exception.temporary()).isTrue();
        assertThat(exception.reasonCode()).isEqualTo("SMTP_451");
    }

    @Test
    void classifiesSmtp550AsPermanent() {
        MailTransportException exception = sendFailure("550 recipient rejected");
        assertThat(exception.temporary()).isFalse();
        assertThat(exception.reasonCode()).isEqualTo("SMTP_550");
    }

    private MailTransportException sendFailure(String smtpMessage) {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException(smtpMessage)).when(sender).send(any(MimeMessage.class));
        var properties = new MailRelayProperties(true, "relay.example", 25, false, false,
                "seal@arte.or.kr", "SEAL");
        var transport = new JavaMailTransport(sender, properties);

        return (MailTransportException) assertThatThrownBy(
                () -> transport.send(new MailMessage("a@example.com", "subject", "<p>body</p>")))
                .isInstanceOf(MailTransportException.class)
                .actual();
    }
}
