package kr.or.arte.seal.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSecurityConfigurationTest {

    @Test
    void productionPasswordEncoderUsesBcryptStrengthTwelve() {
        PasswordEncoder encoder = new AdminSecurityConfiguration().passwordEncoder();

        assertThat(encoder.encode("verification-only-password")).startsWith("$2a$12$");
    }
}
