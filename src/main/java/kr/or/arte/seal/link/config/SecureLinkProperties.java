package kr.or.arte.seal.link.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.nio.file.Path;

@ConfigurationProperties(prefix = "seal.secure-link")
public record SecureLinkProperties(Path hmacKeyFile, String activeKeyId) { }
