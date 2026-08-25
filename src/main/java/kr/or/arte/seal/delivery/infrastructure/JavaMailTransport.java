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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class JavaMailTransport implements MailTransport {
    private static final Pattern SMTP_CODE = Pattern.compile("(?<!\\d)([45]\\d{2})(?!\\d)");
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
            if (message.messageId() != null) mimeMessage.setHeader("Message-ID", message.messageId());
            mailSender.send(mimeMessage);
        } catch (MailParseException | MessagingException | java.io.UnsupportedEncodingException exception) {
            throw new MailTransportException(false, "MAIL_MESSAGE_INVALID");
        } catch (MailException exception) {
            int code = smtpCode(exception);
            if (code >= 500) throw new MailTransportException(false, "SMTP_" + code);
            if (code >= 400) throw new MailTransportException(true, "SMTP_" + code);
            throw new MailTransportException(true, "SMTP_TEMPORARY_FAILURE");
        }
    }

    private static int smtpCode(Throwable root) {
        Set<Throwable> visited = new HashSet<>();
        Throwable current = root;
        while (current != null && visited.add(current)) {
            if (current.getMessage() != null) {
                var matcher = SMTP_CODE.matcher(current.getMessage());
                if (matcher.find()) return Integer.parseInt(matcher.group(1));
            }
            if (current instanceof MessagingException messaging && messaging.getNextException() != null
                    && visited.add(messaging.getNextException())) {
                int nested = smtpCode(messaging.getNextException());
                if (nested != 0) return nested;
            }
            current = current.getCause();
        }
        return 0;
    }
}
