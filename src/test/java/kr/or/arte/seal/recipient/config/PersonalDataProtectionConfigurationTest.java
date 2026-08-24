package kr.or.arte.seal.recipient.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalDataProtectionConfigurationTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsVersionedAesKeysAndSelectsActiveKey() throws Exception {
        Path keyFile = tempDir.resolve("data.keys");
        Files.writeString(keyFile, "v1=" + Base64.getEncoder().encodeToString(new byte[32]));

        LoadedPersonalDataKeys loaded = new PersonalDataKeyLoader().loadAesKeys(keyFile, "v1");

        assertThat(loaded.activeKeyId()).isEqualTo("v1");
        assertThat(loaded.keys()).containsOnlyKeys("v1");
        assertThat(loaded.keys().get("v1").getEncoded()).hasSize(32);
    }

    @Test
    void rejectsMissingActiveKeyAndShortHmacKey() throws Exception {
        Path keyFile = tempDir.resolve("data.keys");
        Files.writeString(keyFile, "v1=" + Base64.getEncoder().encodeToString(new byte[32]));
        Path hmacFile = tempDir.resolve("email.key");
        Files.write(hmacFile, new byte[16]);

        PersonalDataKeyLoader loader = new PersonalDataKeyLoader();

        assertThatThrownBy(() -> loader.loadAesKeys(keyFile, "v2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("활성 암호화 키");
        assertThatThrownBy(() -> loader.loadHmacKey(hmacFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
    }

    @Test
    void rejectsDuplicateKeyIdentifiers() throws Exception {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        Path keyFile = tempDir.resolve("data.keys");
        Files.writeString(keyFile, "v1=" + key + System.lineSeparator() + "v1=" + key);

        assertThatThrownBy(() -> new PersonalDataKeyLoader().loadAesKeys(keyFile, "v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("중복");
    }
}
