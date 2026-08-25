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

import java.util.*;

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
        if (exactEmail == null || exactEmail.isBlank()) {
            return repository.searchScoped(actorId, null, status).stream().map(this::toDetail).toList();
        }
        Map<UUID, RecipientDetail> found = new LinkedHashMap<>();
        for (String hmac : hasher.candidates(EmailNormalizer.normalize(exactEmail))) {
            repository.searchScoped(actorId, hmac, status).stream().map(this::toDetail)
                    .forEach(detail -> found.putIfAbsent(detail.recipientId(), detail));
        }
        return List.copyOf(found.values());
    }

    public kr.or.arte.seal.common.web.AdminPageResult<RecipientDetail> searchPage(UUID actorId, String exactEmail,
            RecipientStatus status, int page, int size) {
        int safePage=Math.max(1,page), safeSize=Math.max(1,Math.min(100,size)), offset=(safePage-1)*safeSize;
        List<RecipientDetail> rows;
        if (exactEmail == null || exactEmail.isBlank()) {
            rows=repository.searchScoped(actorId,null,status,safeSize+1,offset).stream().map(this::toDetail).toList();
        } else {
            rows=search(actorId,exactEmail,status).stream().skip(offset).limit(safeSize+1L).toList();
        }
        return kr.or.arte.seal.common.web.AdminPageResult.of(rows,safePage,safeSize);
    }

    private RecipientDetail toDetail(RecipientStoredRecord record) {
        if (record.status() == RecipientStatus.DESTROYED) {
            return new RecipientDetail(record.recipientId(), record.organizationUnitId(),
                    "파기 완료", "파기 완료", record.collectionBasis(), record.status(),
                    record.versionNo());
        }
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
