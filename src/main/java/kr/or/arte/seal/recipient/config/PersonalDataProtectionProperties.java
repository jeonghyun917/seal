package kr.or.arte.seal.recipient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "seal.personal-data")
public record PersonalDataProtectionProperties(
        String activeKeyId,
        Path keysFile,
        Path emailIndexHmacKeyFile,
        String emailIndexHmacActiveKeyId
) {
}
