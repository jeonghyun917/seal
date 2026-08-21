package kr.or.arte.seal.admin.config;

import kr.or.arte.seal.admin.application.AdminAccountRepository;
import kr.or.arte.seal.admin.application.AdminAuthenticationService;
import kr.or.arte.seal.admin.application.AdminAuthenticator;
import kr.or.arte.seal.admin.domain.LoginLockPolicy;
import kr.or.arte.seal.admin.domain.PasswordPolicy;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.UUID;

@Configuration
public class AdminSecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    LoginLockPolicy loginLockPolicy(AdminSecurityProperties properties) {
        return new LoginLockPolicy(
                properties.maxLoginFailures(),
                properties.temporaryLockDuration()
        );
    }

    @Bean
    PasswordPolicy adminPasswordPolicy() {
        return new PasswordPolicy(12, 128);
    }

    @Bean
    AdminAuthenticator adminAuthenticator(
            AdminAccountRepository accountRepository,
            AuditEventWriter auditEventWriter,
            PasswordEncoder passwordEncoder,
            LoginLockPolicy loginLockPolicy
    ) {
        String dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        return new AdminAuthenticationService(
                accountRepository,
                auditEventWriter,
                passwordEncoder,
                loginLockPolicy,
                dummyPasswordHash
        );
    }
}
