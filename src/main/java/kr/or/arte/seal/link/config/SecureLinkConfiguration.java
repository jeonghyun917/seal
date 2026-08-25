package kr.or.arte.seal.link.config;

import kr.or.arte.seal.link.application.SecureLinkHasher;
import kr.or.arte.seal.link.application.SecureTokenGenerator;
import kr.or.arte.seal.recipient.config.HmacKeyringLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
@EnableConfigurationProperties(SecureLinkProperties.class)
public class SecureLinkConfiguration {
    @Bean SecureTokenGenerator secureTokenGenerator() { return new SecureTokenGenerator(new SecureRandom()); }
    @Bean SecureLinkHasher secureLinkHasher(SecureLinkProperties properties) {
        return new SecureLinkHasher(new HmacKeyringLoader().load(properties.hmacKeyFile(), properties.activeKeyId()));
    }
}
