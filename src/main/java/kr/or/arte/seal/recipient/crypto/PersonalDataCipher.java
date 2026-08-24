package kr.or.arte.seal.recipient.crypto;

public interface PersonalDataCipher {
    EncryptedValue encrypt(PersonalDataContext context, String plaintext);

    String decrypt(PersonalDataContext context, EncryptedValue value);
}
