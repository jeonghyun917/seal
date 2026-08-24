package kr.or.arte.seal.delivery.config;

import kr.or.arte.seal.delivery.application.MailOutboxRepository;
import kr.or.arte.seal.delivery.application.MailOutboxWorker;
import kr.or.arte.seal.delivery.application.MailTransport;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MailRelayProperties.class)
public class MailDeliveryConfiguration {

    @Bean
    MailOutboxWorker mailOutboxWorker(MailOutboxRepository repository, MailTransport transport,
            PersonalDataCipher cipher, MailRelayProperties properties, Clock clock) {
        return new MailOutboxWorker(repository, transport, cipher, properties.enabled(), clock);
    }
}
