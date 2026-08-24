package kr.or.arte.seal.link.application;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecureTokenGeneratorTest {
    @Test
    void generatesThirtyTwoRandomBytesAsUnpaddedBase64Url() {
        String token = new SecureTokenGenerator(new SecureRandom()).generate();
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
    }
}
