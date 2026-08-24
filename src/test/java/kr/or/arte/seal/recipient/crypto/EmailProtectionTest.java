package kr.or.arte.seal.recipient.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailProtectionTest {

    @ParameterizedTest
    @CsvSource({
            "' Person@Example.TEST ','person@example.test'",
            "'A@B.TEST','a@b.test'",
            "'Ｔｅｓｔ@EXAMPLE.TEST','ｔｅｓｔ@example.test'"
    })
    void normalizesEmailBeforeHmac(String raw, String expected) {
        assertThat(EmailNormalizer.normalize(raw)).isEqualTo(expected);
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> EmailNormalizer.normalize("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
    }

    @Test
    void hmacIsDeterministicLowercaseHexAndKeyDependent() {
        byte[] firstKey = new byte[32];
        byte[] secondKey = new byte[32];
        firstKey[0] = 1;
        secondKey[0] = 2;
        EmailLookupHasher first = new HmacEmailLookupHasher(firstKey);
        EmailLookupHasher second = new HmacEmailLookupHasher(secondKey);

        String firstHash = first.hmac("person@example.test");

        assertThat(firstHash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(first.hmac("person@example.test")).isEqualTo(firstHash);
        assertThat(second.hmac("person@example.test")).isNotEqualTo(firstHash);
    }

    @Test
    void masksWithoutExposingFullIdentifiers() {
        assertThat(PersonalDataMasker.name("홍길동")).isEqualTo("홍*동");
        assertThat(PersonalDataMasker.name("김철")).isEqualTo("김*");
        assertThat(PersonalDataMasker.email("person@example.test"))
                .isEqualTo("p*****@e******.test");
    }
}
