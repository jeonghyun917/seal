package kr.or.arte.seal.delivery.infrastructure;

import jakarta.mail.MessagingException;
import kr.or.arte.seal.delivery.application.MailMessage;
import kr.or.arte.seal.delivery.application.MailTransport;
import kr.or.arte.seal.delivery.application.MailTransportException;
import kr.or.arte.seal.delivery.config.MailRelayProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JavaMailTransport implements MailTransport {
    private final JavaMailSender mailSender;
    private final MailRelayProperties properties;

    public JavaMailTransport(JavaMailSender mailSender, MailRelayProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(MailMessage message) {
        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.html(), true);
            mailSender.send(mimeMessage);
        } catch (MailParseException | MessagingException | java.io.UnsupportedEncodingException exception) {
            throw new MailTransportException(false, "MAIL_MESSAGE_INVALID");
        } catch (MailException exception) {
            throw new MailTransportException(true, "SMTP_TEMPORARY_FAILURE");
        }
    }
}
