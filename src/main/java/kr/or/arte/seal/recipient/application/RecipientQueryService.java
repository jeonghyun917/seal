package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.crypto.EmailLookupHasher;
import kr.or.arte.seal.recipient.crypto.EmailNormalizer;
import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import kr.or.arte.seal.recipient.crypto.PersonalDataMasker;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RecipientQueryService {
    private final RecipientRepository repository;
    private final PersonalDataCipher cipher;
    private final EmailLookupHasher hasher;

    public RecipientQueryService(RecipientRepository repository, PersonalDataCipher cipher,
                                 EmailLookupHasher hasher) {
        this.repository = repository;
        this.cipher = cipher;
        this.hasher = hasher;
    }

    public RecipientDetail find(UUID actorId, UUID recipientId) {
        return toDetail(repository.findScoped(actorId, recipientId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public List<RecipientDetail> search(UUID actorId, String exactEmail, RecipientStatus status) {
        String hmac = exactEmail == null || exactEmail.isBlank() ? null
                : hasher.hmac(EmailNormalizer.normalize(exactEmail));
        return repository.searchScoped(actorId, hmac, status).stream().map(this::toDetail).toList();
    }

    private RecipientDetail toDetail(RecipientStoredRecord record) {
        String name = decrypt(record, "name", record.nameCiphertext());
        String email = decrypt(record, "email", record.emailCiphertext());
        return new RecipientDetail(record.recipientId(), record.organizationUnitId(),
                PersonalDataMasker.name(name), PersonalDataMasker.email(email),
                record.collectionBasis(), record.status(), record.versionNo());
    }

    private String decrypt(RecipientStoredRecord record, String field, byte[] bytes) {
        return cipher.decrypt(new PersonalDataContext("RECIPIENT",
                        record.recipientId(), record.organizationUnitId(), field),
                new EncryptedValue(record.encryptionKeyId(), bytes));
    }
}
