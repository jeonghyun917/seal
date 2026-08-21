package kr.or.arte.seal.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("seal.admin-security")
public record AdminSecurityProperties(int maxLoginFailures, Duration temporaryLockDuration) {

    public AdminSecurityProperties {
        if (maxLoginFailures < 1) {
            throw new IllegalArgumentException("maxLoginFailures must be positive");
        }
        if (temporaryLockDuration == null
                || temporaryLockDuration.isZero()
                || temporaryLockDuration.isNegative()) {
            throw new IllegalArgumentException("temporaryLockDuration must be positive");
        }
    }
}
