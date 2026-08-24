package kr.or.arte.seal.delivery.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MailLocalProfileConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(MailPropertiesConfiguration.class)
            .withPropertyValues(
                    "spring.profiles.active=local",
                    "seal.mail.enabled=true",
                    "seal.mail.port=25",
                    "seal.mail.ssl-enabled=false",
                    "seal.mail.from-address=seal@arte.or.kr",
                    "spring.mail.port=25",
                    "spring.mail.properties.mail.smtp.ssl.enable=false",
                    "spring.mail.properties.mail.smtp.starttls.enable=false");

    @Test
    void localProfileCanEnablePort25PlainTextRelay() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            MailRelayProperties properties = context.getBean(MailRelayProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.port()).isEqualTo(25);
            assertThat(properties.sslEnabled()).isFalse();
            assertThat(properties.fromAddress()).isEqualTo("seal@arte.or.kr");

            assertThat(context.getEnvironment().getProperty("spring.mail.port", Integer.class)).isEqualTo(25);
            assertThat(context.getEnvironment()
                    .getProperty("spring.mail.properties.mail.smtp.ssl.enable", Boolean.class)).isFalse();
            assertThat(context.getEnvironment()
                    .getProperty("spring.mail.properties.mail.smtp.starttls.enable", Boolean.class)).isFalse();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MailRelayProperties.class)
    static class MailPropertiesConfiguration {
    }
}
