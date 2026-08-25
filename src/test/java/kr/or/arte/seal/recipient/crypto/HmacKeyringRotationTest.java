package kr.or.arte.seal.recipient.crypto;

import kr.or.arte.seal.link.application.SecureLinkHasher;
import kr.or.arte.seal.recipient.config.LoadedHmacKeys;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class HmacKeyringRotationTest {
    @Test
    void activeAndPreviousKeysProduceLookupCandidates() {
        byte[] v1 = "11111111111111111111111111111111".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] v2 = "22222222222222222222222222222222".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var keys = new LinkedHashMap<String, byte[]>();
        keys.put("v1", v1);
        keys.put("v2", v2);
        LoadedHmacKeys rotated = new LoadedHmacKeys("v2", keys);

        HmacEmailLookupHasher oldEmail = new HmacEmailLookupHasher(v1);
        HmacEmailLookupHasher newEmail = new HmacEmailLookupHasher(rotated);
        assertThat(newEmail.candidates("user@example.com"))
                .containsExactlyInAnyOrder(newEmail.hmac("user@example.com"), oldEmail.hmac("user@example.com"));

        SecureLinkHasher oldLink = new SecureLinkHasher(v1);
        SecureLinkHasher newLink = new SecureLinkHasher(rotated);
        assertThat(newLink.candidates("token"))
                .containsExactlyInAnyOrder(newLink.hmac("token"), oldLink.hmac("token"));
    }
}
