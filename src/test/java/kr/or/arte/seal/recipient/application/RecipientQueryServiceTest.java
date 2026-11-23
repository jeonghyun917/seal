package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientQueryServiceTest {
    @Test void emailPageStopsDecryptingAfterFindingThePageAndNextRow() {
        var rows=java.util.stream.IntStream.range(0,250).mapToObj(i->active(UUID.randomUUID(),"기관","담당자")).toList();
        var cipher=org.mockito.Mockito.spy(cipherReturning("홍길동","user@example.com"));
        var result=new RecipientQueryService(repositoryReturning(rows),cipher)
                .searchPage(UUID.randomUUID(),"EXAMPLE",null,1,20);
        assertThat(result.items()).hasSize(20);
        assertThat(result.hasNext()).isTrue();
        org.mockito.Mockito.verify(cipher,org.mockito.Mockito.times(42)).decrypt(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
    }

    @Test void bulkPickerReturnsOnlyTheLimitPlusOneForTruncationDetection() {
        var rows=java.util.stream.IntStream.range(0,1500).mapToObj(i->active(UUID.randomUUID(),"기관","담당자")).toList();
        var result=new RecipientQueryService(repositoryReturning(rows),cipherReturning("홍길동","user@example.com"))
                .searchPicker(UUID.randomUUID(),"example");
        assertThat(result).hasSize(1001);
    }

    @Test
    void blankPickerPageUsesRepositoryPagingWithoutLoadingEveryRecipient() {
        UUID actorId = UUID.randomUUID();
        List<RecipientStoredRecord> records = new ArrayList<>();
        for (int i = 0; i < 45; i++) records.add(active(UUID.randomUUID(), "예술교육팀", "담당자"));
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger requestedLimit = new AtomicInteger();
        AtomicInteger requestedOffset = new AtomicInteger();
        RecipientRepository repository = new RecipientRepository() {
            @Override public boolean canAccessActiveOrganization(UUID actor, UUID organization) { return true; }
            @Override public Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID organization, String hash, UUID excluded) { return Optional.empty(); }
            @Override public void insert(RecipientStoredRecord row, UUID actor, Instant now) { }
            @Override public Optional<RecipientStoredRecord> findScoped(UUID actor, UUID recipient) { return Optional.empty(); }
            @Override public boolean update(RecipientStoredRecord row, UUID actor, Instant now, long version) { return false; }
            @Override public List<RecipientStoredRecord> searchScoped(UUID actor, String hash, RecipientStatus status) {
                throw new AssertionError("전체 목록 조회를 호출해서는 안 됩니다.");
            }
            @Override public List<RecipientStoredRecord> searchScoped(UUID actor, String hash, RecipientStatus status, int limit, int offset) {
                calls.incrementAndGet(); requestedLimit.set(limit); requestedOffset.set(offset);
                return records.stream().skip(offset).limit(limit).toList();
            }
        };
        RecipientQueryService service = new RecipientQueryService(repository, cipherReturning("홍길동", "user@example.com"));

        var result = service.searchPickerPage(actorId, "", 2, 20);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(requestedLimit.get()).isEqualTo(21);
        assertThat(requestedOffset.get()).isEqualTo(20);
        assertThat(result.items()).hasSize(20);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void pickerKeywordSearchCanFindRecipientsBeyondTheFirstThousand() {
        UUID actorId = UUID.randomUUID();
        UUID matchingId = UUID.randomUUID();
        List<RecipientStoredRecord> records = new ArrayList<>();
        for (int i = 0; i < 1000; i++) records.add(active(UUID.randomUUID(), "일반 부서", "담당자"));
        records.add(active(matchingId, "특별 예술교육팀", "담당자"));
        PersonalDataCipher cipher = new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) { throw new UnsupportedOperationException(); }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                return "name".equals(context.field()) ? "홍길동" : "user@example.com";
            }
        };
        RecipientQueryService service = new RecipientQueryService(repositoryReturning(records), cipher);

        var result = service.searchPickerPage(actorId, "특별", 1, 20);

        assertThat(result.items()).extracting(RecipientDetail::recipientId).containsExactly(matchingId);
    }

    @Test
    void pickerSearchFiltersVisibleFieldsAndReturnsRequestedPageOnly() {
        UUID actorId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        RecipientStoredRecord first = active(firstId, "예술교육팀", "교육팀장");
        RecipientStoredRecord second = active(secondId, "경영지원팀", "과장");
        Map<UUID, List<String>> personalData = Map.of(
                firstId, List.of("김예술", "artist@example.com"),
                secondId, List.of("박경영", "manager@example.com"));
        PersonalDataCipher cipher = new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) { throw new UnsupportedOperationException(); }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                return personalData.get(context.objectId()).get("name".equals(context.field()) ? 0 : 1);
            }
        };
        RecipientQueryService service = new RecipientQueryService(repositoryReturning(List.of(first, second)),
                cipher);

        var result = service.searchPickerPage(actorId, "예술", 1, 20);

        assertThat(result.items()).extracting(RecipientDetail::recipientId).containsExactly(firstId);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void pickerSearchIncludesRecipientCollectionBasis() {
        UUID actorId = UUID.randomUUID();
        UUID matchingId = UUID.randomUUID();
        RecipientStoredRecord matching = active(matchingId, "예술교육팀", "교육팀장",
                "2026년 교육 프로그램 신청서 접수");
        RecipientStoredRecord other = active(UUID.randomUUID(), "예술교육팀", "교육팀장",
                "외부 행사 명함 수령");
        PersonalDataCipher cipher = new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) { throw new UnsupportedOperationException(); }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                return "name".equals(context.field()) ? "홍길동" : "user@example.com";
            }
        };
        RecipientQueryService service = new RecipientQueryService(repositoryReturning(List.of(matching, other)),
                cipher);

        var result = service.searchPickerPage(actorId, "교육 프로그램 신청서", 1, 20);

        assertThat(result.items()).extracting(RecipientDetail::recipientId).containsExactly(matchingId);
    }

    @Test
    void listSearchIncludesRecipientCollectionBasis() {
        UUID actorId = UUID.randomUUID();
        UUID matchingId = UUID.randomUUID();
        RecipientStoredRecord matching = active(matchingId, "예술교육팀", "교육팀장",
                "2026년 교육 프로그램 신청서 접수");
        RecipientStoredRecord other = active(UUID.randomUUID(), "예술교육팀", "교육팀장",
                "외부 행사 명함 수령");
        PersonalDataCipher cipher = new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) { throw new UnsupportedOperationException(); }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                return "name".equals(context.field()) ? "홍길동" : "user@example.com";
            }
        };
        RecipientQueryService service = new RecipientQueryService(repositoryReturning(List.of(matching, other)),
                cipher);

        var result = service.searchPage(actorId, null, "교육 프로그램", null, 1, 20);

        assertThat(result.items()).extracting(RecipientDetail::recipientId).containsExactly(matchingId);
    }

    @Test
    void listEmailSearchMatchesAnyCaseInsensitiveSubstring() {
        UUID actorId = UUID.randomUUID();
        UUID matchingId = UUID.randomUUID();
        RecipientStoredRecord other = active(UUID.randomUUID(), "예술교육팀", "교육팀장");
        RecipientStoredRecord matching = active(matchingId, "경영지원팀", "과장");
        Map<UUID, List<String>> personalData = Map.of(
                other.recipientId(), List.of("김예술", "alpha@example.com"),
                matchingId, List.of("박경영", "assistant@domain.kr"));
        PersonalDataCipher cipher = new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) {
                throw new UnsupportedOperationException();
            }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                return personalData.get(context.objectId()).get("name".equals(context.field()) ? 0 : 1);
            }
        };
        RecipientQueryService service = new RecipientQueryService(repositoryReturning(List.of(other, matching)),
                cipher);

        var result = service.searchPage(actorId, "ANT@DOM", null, null, 1, 20);

        assertThat(result.items()).extracting(RecipientDetail::recipientId).containsExactly(matchingId);
    }

    @Test
    void destroyedRecipientIsReturnedAsTombstoneWithoutDecryptingNullPersonalData() {
        UUID actorId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        RecipientStoredRecord destroyed = new RecipientStoredRecord(recipientId, organizationId,
                null, null, null, null, "동의", RecipientStatus.valueOf("DESTROYED"), 7);
        RecipientQueryService service = new RecipientQueryService(repositoryReturning(destroyed),
                cipherThatMustNotBeCalled());

        RecipientDetail detail = service.find(actorId, recipientId);

        assertThat(detail.maskedName()).isEqualTo("파기 완료");
        assertThat(detail.maskedEmail()).isEqualTo("파기 완료");
        assertThat(detail.status().name()).isEqualTo("DESTROYED");
    }

    private static RecipientRepository repositoryReturning(RecipientStoredRecord record) {
        return repositoryReturning(List.of(record));
    }

    private static RecipientRepository repositoryReturning(List<RecipientStoredRecord> records) {
        return new RecipientRepository() {
            @Override public boolean canAccessActiveOrganization(UUID actorId, UUID organizationId) { return true; }
            @Override public Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID organizationId,
                    String emailLookupHmac, UUID excludedRecipientId) { return Optional.empty(); }
            @Override public void insert(RecipientStoredRecord row, UUID actorId, Instant now) { }
            @Override public Optional<RecipientStoredRecord> findScoped(UUID actorId, UUID recipientId) {
                return records.stream().findFirst();
            }
            @Override public boolean update(RecipientStoredRecord row, UUID actorId, Instant now,
                    long expectedVersion) { return false; }
            @Override public List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
                    RecipientStatus status) { return records; }
        };
    }

    private static RecipientStoredRecord active(UUID recipientId, String affiliation, String jobTitle) {
        return active(recipientId, affiliation, jobTitle, "등록 근거");
    }

    private static RecipientStoredRecord active(UUID recipientId, String affiliation, String jobTitle,
            String collectionBasis) {
        return new RecipientStoredRecord(recipientId, UUID.randomUUID(), affiliation, jobTitle,
                new byte[30], new byte[30], "v1", "a".repeat(64), collectionBasis,
                RecipientStatus.ACTIVE, 0);
    }

    private static PersonalDataCipher cipherThatMustNotBeCalled() {
        return new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) {
                throw new AssertionError("파기된 개인정보를 암호화해서는 안 됩니다.");
            }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                throw new AssertionError("파기된 개인정보를 복호화해서는 안 됩니다.");
            }
        };
    }

    private static PersonalDataCipher cipherReturning(String name, String email) {
        return new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) {
                throw new UnsupportedOperationException();
            }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                return "name".equals(context.field()) ? name : email;
            }
        };
    }
}
