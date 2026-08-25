package kr.or.arte.seal.recipient.config;

import kr.or.arte.seal.recipient.crypto.AesGcmPersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.EmailLookupHasher;
import kr.or.arte.seal.recipient.crypto.HmacEmailLookupHasher;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
@EnableConfigurationProperties(PersonalDataProtectionProperties.class)
public class PersonalDataProtectionConfiguration {
    @Bean
    PersonalDataKeyLoader personalDataKeyLoader() {
        return new PersonalDataKeyLoader();
    }

    @Bean
    PersonalDataCipher personalDataCipher(PersonalDataProtectionProperties properties,
                                          PersonalDataKeyLoader loader) {
        LoadedPersonalDataKeys loaded = loader.loadAesKeys(
                properties.keysFile(), properties.activeKeyId());
        return new AesGcmPersonalDataCipher(loaded.keys(), loaded.activeKeyId(),
                new SecureRandom());
    }

    @Bean
    EmailLookupHasher emailLookupHasher(PersonalDataProtectionProperties properties,
                                        PersonalDataKeyLoader loader) {
        return new HmacEmailLookupHasher(new HmacKeyringLoader().load(
                properties.emailIndexHmacKeyFile(), properties.emailIndexHmacActiveKeyId()));
    }
}
