package kr.or.arte.seal.recipient.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmPersonalDataCipherTest {

    private static final UUID RECIPIENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ORGANIZATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void encryptsSamePlaintextDifferentlyAndRejectsDifferentAad() {
        PersonalDataCipher cipher = cipher();
        var context = new PersonalDataContext("recipient", RECIPIENT_ID, ORGANIZATION_ID, "email");

        EncryptedValue first = cipher.encrypt(context, "person@example.test");
        EncryptedValue second = cipher.encrypt(context, "person@example.test");

        assertThat(first.keyId()).isEqualTo("v1");
        assertThat(first.bytes()).isNotEqualTo(second.bytes());
        assertThat(new String(first.bytes(), UTF_8)).doesNotContain("person@example.test");
        assertThat(cipher.decrypt(context, first)).isEqualTo("person@example.test");
        assertThatThrownBy(() -> cipher.decrypt(
                new PersonalDataContext("recipient", OTHER_ID, ORGANIZATION_ID, "email"), first))
                .isInstanceOf(PersonalDataProtectionException.class);
    }

    @Test
    void encryptedValueDefensivelyCopiesBytes() {
        byte[] source = {1, 2, 3};
        EncryptedValue value = new EncryptedValue("v1", source);
        source[0] = 9;
        byte[] returned = value.bytes();
        returned[1] = 9;

        assertThat(value.bytes()).containsExactly(1, 2, 3);
    }

    private static PersonalDataCipher cipher() {
        byte[] keyBytes = new byte[32];
        for (int index = 0; index < keyBytes.length; index++) {
            keyBytes[index] = (byte) (index + 1);
        }
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        return new AesGcmPersonalDataCipher(Map.of("v1", key), "v1", new SecureRandom());
    }
}
