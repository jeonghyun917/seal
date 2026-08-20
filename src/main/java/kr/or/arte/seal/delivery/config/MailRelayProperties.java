package kr.or.arte.seal.delivery.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "seal.mail")
public record MailRelayProperties(
        boolean enabled,
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        boolean sslEnabled,
        boolean authEnabled,
        @NotBlank @Email String fromAddress,
        @NotBlank String fromName) {

    @AssertTrue(message = "활성화된 메일 발신 주소는 @arte.or.kr 도메인이어야 합니다.")
    public boolean isArteSenderWhenEnabled() {
        return !enabled || fromAddress.toLowerCase().endsWith("@arte.or.kr");
    }

    @AssertTrue(message = "활성화된 SMTP 릴레이는 465 포트의 SSL/TLS와 무인증 방식을 사용해야 합니다.")
    public boolean isSecureUnauthenticatedRelayWhenEnabled() {
        return !enabled || (port == 465 && sslEnabled && !authEnabled);
    }
}
