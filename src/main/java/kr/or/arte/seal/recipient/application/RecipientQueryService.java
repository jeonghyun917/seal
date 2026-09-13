package kr.or.arte.seal.recipient.application;

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

    public RecipientQueryService(RecipientRepository repository, PersonalDataCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    public RecipientDetail find(UUID actorId, UUID recipientId) {
        return toDetail(repository.findScoped(actorId, recipientId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public List<RecipientDetail> search(UUID actorId, String emailKeyword, RecipientStatus status) {
        return search(actorId, emailKeyword, null, status);
    }

    public List<RecipientDetail> search(UUID actorId, String emailKeyword, String collectionBasis,
            RecipientStatus status) {
        String basis = normalizeKeyword(collectionBasis);
        String normalizedEmailKeyword = normalizeEmailKeyword(emailKeyword);
        if (normalizedEmailKeyword == null) {
            return repository.searchScoped(actorId, null, basis, status).stream().map(this::toDetail).toList();
        }
        return scopedCandidates(actorId, basis, status).stream()
                .map(record -> toDetailWhenEmailContains(record, normalizedEmailKeyword))
                .flatMap(Optional::stream)
                .toList();
    }

    public kr.or.arte.seal.common.web.AdminPageResult<RecipientDetail> searchPage(UUID actorId, String exactEmail,
            RecipientStatus status, int page, int size) {
        return searchPage(actorId, exactEmail, null, status, page, size);
    }

    public kr.or.arte.seal.common.web.AdminPageResult<RecipientDetail> searchPage(UUID actorId, String exactEmail,
            String collectionBasis, RecipientStatus status, int page, int size) {
        int safePage=Math.max(1,page), safeSize=Math.max(1,Math.min(100,size)), offset=(safePage-1)*safeSize;
        List<RecipientDetail> rows;
        String basis = normalizeKeyword(collectionBasis);
        if (exactEmail == null || exactEmail.isBlank()) {
            rows=repository.searchScoped(actorId,null,basis,status,safeSize+1,offset).stream()
                    .map(this::toDetail).toList();
        } else {
            rows=search(actorId,exactEmail,basis,status).stream().skip(offset).limit(safeSize+1L).toList();
        }
        return kr.or.arte.seal.common.web.AdminPageResult.of(rows,safePage,safeSize);
    }

    public List<RecipientDetail> searchPicker(UUID actorId, String keyword) {
        String normalized = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        return scanPickerMatches(actorId, normalized, 0, Integer.MAX_VALUE);
    }

    public kr.or.arte.seal.common.web.AdminPageResult<RecipientDetail> searchPickerPage(
            UUID actorId, String keyword, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        int offset = (safePage - 1) * safeSize;
        String normalized = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        List<RecipientDetail> rows = normalized.isEmpty()
                ? repository.searchScoped(actorId, null, RecipientStatus.ACTIVE, safeSize + 1, offset)
                        .stream().map(this::toDetail).toList()
                : scanPickerMatches(actorId, normalized, offset, safeSize + 1);
        return kr.or.arte.seal.common.web.AdminPageResult.of(rows, safePage, safeSize);
    }

    private List<RecipientDetail> scanPickerMatches(UUID actorId, String normalized, int matchesToSkip,
            int resultLimit) {
        List<RecipientDetail> found = new ArrayList<>();
        int remainingToSkip = matchesToSkip;
        for (int sourceOffset = 0; ; sourceOffset += 100) {
            List<RecipientStoredRecord> batch = repository.searchScoped(actorId, null,
                    RecipientStatus.ACTIVE, 100, sourceOffset);
            for (RecipientStoredRecord record : batch) {
                Optional<RecipientDetail> match = toPickerDetailWhenMatches(record, normalized);
                if (match.isEmpty()) continue;
                if (remainingToSkip > 0) { remainingToSkip--; continue; }
                found.add(match.get());
                if (found.size() >= resultLimit) return List.copyOf(found);
            }
            if (batch.size() < 100) return List.copyOf(found);
        }
    }

    private Optional<RecipientDetail> toPickerDetailWhenMatches(RecipientStoredRecord record, String normalized) {
        String name = decrypt(record, "name", record.nameCiphertext());
        String email = decrypt(record, "email", record.emailCiphertext());
        RecipientDetail detail = toDetail(record, name, email);
        String searchable = String.join(" ", value(record.affiliationName()), value(record.jobTitle()),
                value(name), value(email), value(record.collectionBasis())).toLowerCase(Locale.ROOT);
        return searchable.contains(normalized) ? Optional.of(detail) : Optional.empty();
    }

    private static String value(String value) { return value == null ? "" : value; }
    private static String normalizeKeyword(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
    private static String normalizeEmailKeyword(String value) {
        String keyword = normalizeKeyword(value);
        return keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
    }

    private List<RecipientStoredRecord> scopedCandidates(UUID actorId, String collectionBasis,
            RecipientStatus status) {
        List<RecipientStoredRecord> found = new ArrayList<>();
        for (int offset = 0; ; offset += 100) {
            List<RecipientStoredRecord> batch = repository.searchScoped(actorId, null,
                    collectionBasis, status, 100, offset);
            found.addAll(batch);
            if (batch.size() < 100) break;
        }
        return List.copyOf(found);
    }

    private Optional<RecipientDetail> toDetailWhenEmailContains(RecipientStoredRecord record,
            String normalizedEmailKeyword) {
        if (record.status() == RecipientStatus.DESTROYED) return Optional.empty();
        String email = decrypt(record, "email", record.emailCiphertext());
        if (!email.toLowerCase(Locale.ROOT).contains(normalizedEmailKeyword)) return Optional.empty();
        String name = decrypt(record, "name", record.nameCiphertext());
        return Optional.of(toDetail(record, name, email));
    }

    private RecipientDetail toDetail(RecipientStoredRecord record) {
        if (record.status() == RecipientStatus.DESTROYED) {
            return new RecipientDetail(record.recipientId(), record.organizationUnitId(),
                    null, null, "파기 완료", "파기 완료", record.collectionBasis(), record.status(),
                    record.versionNo());
        }
        String name = decrypt(record, "name", record.nameCiphertext());
        String email = decrypt(record, "email", record.emailCiphertext());
        return toDetail(record, name, email);
    }

    private RecipientDetail toDetail(RecipientStoredRecord record, String name, String email) {
        return new RecipientDetail(record.recipientId(), record.organizationUnitId(),
                record.affiliationName(), record.jobTitle(), PersonalDataMasker.name(name), PersonalDataMasker.email(email),
                record.collectionBasis(), record.status(), record.versionNo());
    }

    private String decrypt(RecipientStoredRecord record, String field, byte[] bytes) {
        return cipher.decrypt(new PersonalDataContext("RECIPIENT",
                        record.recipientId(), record.organizationUnitId(), field),
                new EncryptedValue(record.encryptionKeyId(), bytes));
    }
}
