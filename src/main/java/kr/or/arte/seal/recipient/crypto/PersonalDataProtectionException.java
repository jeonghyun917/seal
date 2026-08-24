package kr.or.arte.seal.recipient.crypto;

public class PersonalDataProtectionException extends RuntimeException {
    public PersonalDataProtectionException(String message) {
        super(message);
    }

    public PersonalDataProtectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
