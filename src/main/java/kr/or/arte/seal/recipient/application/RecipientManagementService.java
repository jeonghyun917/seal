package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.crypto.EmailLookupHasher;
import kr.or.arte.seal.recipient.crypto.EmailNormalizer;
import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import kr.or.arte.seal.recipient.crypto.PersonalDataMasker;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class RecipientManagementService {
    private final RecipientRepository repository;
    private final PersonalDataCipher cipher;
    private final EmailLookupHasher hasher;
    private final AuditEventWriter auditWriter;
    private final Clock clock;

    public RecipientManagementService(RecipientRepository repository, PersonalDataCipher cipher,
            EmailLookupHasher hasher, AuditEventWriter auditWriter, Clock clock) {
        this.repository = repository;
        this.cipher = cipher;
        this.hasher = hasher;
        this.auditWriter = auditWriter;
        this.clock = clock;
    }

    @Transactional
    public UUID create(CreateRecipientCommand command) {
        if (command.actorId() == null || command.organizationUnitId() == null
                || !repository.canAccessActiveOrganization(command.actorId(), command.organizationUnitId())) {
            throw notFound();
        }
        String name = validName(command.name());
        String email = validEmail(command.email());
        String basis = validBasis(command.collectionBasis());
        String emailHmac = hasher.hmac(EmailNormalizer.normalize(email));
        rejectDuplicate(command.organizationUnitId(), EmailNormalizer.normalize(email), null, email);

        UUID id = UUID.randomUUID();
        EncryptedValue encryptedName = cipher.encrypt(context(id, command.organizationUnitId(), "name"), name);
        EncryptedValue encryptedEmail = cipher.encrypt(context(id, command.organizationUnitId(), "email"), email);
        if (!encryptedName.keyId().equals(encryptedEmail.keyId())) {
            throw new IllegalStateException("개인정보 암호화 키 버전이 일치하지 않습니다.");
        }
        repository.insert(new RecipientStoredRecord(id, command.organizationUnitId(),
                encryptedName.bytes(), encryptedEmail.bytes(), encryptedName.keyId(), emailHmac,
                basis, RecipientStatus.ACTIVE, 0), command.actorId(), clock.instant());
        audit("RECIPIENT_CREATE", id, command.actorId(), command.organizationUnitId(), RecipientStatus.ACTIVE);
        return id;
    }

    @Transactional
    public void update(UUID recipientId, UpdateRecipientCommand command) {
        RecipientStoredRecord current = repository.findScoped(command.actorId(), recipientId).orElseThrow(
                RecipientManagementService::notFound);
        requireNotDestroyed(current);
        String oldName = decrypt(current, "name", current.nameCiphertext());
        String oldEmail = decrypt(current, "email", current.emailCiphertext());
        String name = blank(command.name()) ? oldName : validName(command.name());
        String email = blank(command.email()) ? oldEmail : validEmail(command.email());
        String basis = validBasis(command.collectionBasis());
        String emailHmac = hasher.hmac(EmailNormalizer.normalize(email));
        rejectDuplicate(current.organizationUnitId(), EmailNormalizer.normalize(email), recipientId, email);

        EncryptedValue encryptedName = cipher.encrypt(context(recipientId, current.organizationUnitId(), "name"), name);
        EncryptedValue encryptedEmail = cipher.encrypt(context(recipientId, current.organizationUnitId(), "email"), email);
        RecipientStoredRecord updated = new RecipientStoredRecord(recipientId, current.organizationUnitId(),
                encryptedName.bytes(), encryptedEmail.bytes(), encryptedName.keyId(), emailHmac,
                basis, current.status(), command.versionNo() + 1);
        if (!repository.update(updated, command.actorId(), clock.instant(), command.versionNo())) throw conflict();
        audit("RECIPIENT_UPDATE", recipientId, command.actorId(), current.organizationUnitId(), current.status());
    }

    @Transactional
    public void changeStatus(UUID recipientId, UUID actorId, RecipientStatus status, long versionNo) {
        RecipientStoredRecord current = repository.findScoped(actorId, recipientId).orElseThrow(
                RecipientManagementService::notFound);
        requireNotDestroyed(current);
        if (status == RecipientStatus.ACTIVE) {
            String email = decrypt(current, "email", current.emailCiphertext());
            rejectDuplicate(current.organizationUnitId(), EmailNormalizer.normalize(email), recipientId, email);
        }
        RecipientStoredRecord updated = new RecipientStoredRecord(recipientId, current.organizationUnitId(),
                current.nameCiphertext(), current.emailCiphertext(), current.encryptionKeyId(),
                current.emailLookupHmac(), current.collectionBasis(), status, versionNo + 1);
        if (!repository.update(updated, actorId, clock.instant(), versionNo)) throw conflict();
        audit("RECIPIENT_STATUS_CHANGE", recipientId, actorId, current.organizationUnitId(), status);
    }

    private void rejectDuplicate(UUID orgId, String normalizedEmail, UUID excludedId, String email) {
        for (String hmac : hasher.candidates(normalizedEmail)) {
            var existing = repository.findActiveByEmailHmac(orgId, hmac, excludedId);
            if (existing.isPresent()) {
                throw new DuplicateRecipientException(existing.get().recipientId(), PersonalDataMasker.email(email));
            }
        }
    }

    private String decrypt(RecipientStoredRecord record, String field, byte[] value) {
        return cipher.decrypt(context(record.recipientId(), record.organizationUnitId(), field),
                new EncryptedValue(record.encryptionKeyId(), value));
    }


    private static void requireNotDestroyed(RecipientStoredRecord record) {
        if (record.status() == RecipientStatus.DESTROYED) {
            throw new IllegalStateException("파기 완료된 대상자는 변경할 수 없습니다.");
        }
    }
    private void audit(String action, UUID id, UUID actorId, UUID orgId, RecipientStatus status) {
        auditWriter.append(new AuditEventCommand(clock.instant(), "ADMIN", actorId, action,
                "RECIPIENT", id.toString(), "SUCCESS", null, UUID.randomUUID(),
                Map.of("organizationUnitId", orgId.toString(), "status", status.name())));
    }

    private static PersonalDataContext context(UUID id, UUID orgId, String field) {
        return new PersonalDataContext("RECIPIENT", id, orgId, field);
    }

    private static String validName(String value) {
        if (blank(value)) throw new IllegalArgumentException("대상자 이름을 입력해 주세요.");
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        if (normalized.length() > 100) throw new IllegalArgumentException("대상자 이름은 100자 이하여야 합니다.");
        return normalized;
    }

    private static String validEmail(String value) {
        if (blank(value)) throw new IllegalArgumentException("이메일을 입력해 주세요.");
        String email = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        int at = email.indexOf('@');
        if (email.length() > 254 || at < 1 || at != email.lastIndexOf('@')
                || at == email.length() - 1 || email.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("올바른 이메일 형식을 입력해 주세요.");
        }
        return email;
    }

    private static String validBasis(String value) {
        if (blank(value) || value.strip().length() < 5 || value.strip().length() > 500) {
            throw new IllegalArgumentException("수집 근거는 5~500자로 입력해 주세요.");
        }
        return value.strip();
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND); }
    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "다른 변경과 충돌했습니다. 최신 내용을 다시 불러온 뒤 재시도해 주세요.");
    }
}
