package kr.or.arte.seal.link.application;

import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.request.application.ConsentRequestRepository;
import kr.or.arte.seal.request.application.RequestSnapshot;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecureLinkManagementServiceTest {
    private static final UUID ACTOR = UUID.randomUUID(), REQUEST = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-24T06:00:00Z");

    @Test
    void activationPersistsOnlyHmacAndReturnsRawTokenOnce() {
        FakeRequestRepository requests = new FakeRequestRepository(ConsentRequestStatus.READY);
        FakeLinkRepository links = new FakeLinkRepository();
        SecureLinkManagementService service = new SecureLinkManagementService(requests, links,
                new SecureTokenGenerator(new SecureRandom()), new SecureLinkHasher(new byte[32]),
                (recipientId, today) -> false,
                Clock.fixed(NOW, ZoneOffset.UTC));

        IssuedSecureLink issued = service.activate(ACTOR, REQUEST, 0);

        assertThat(issued.token()).hasSize(43);
        assertThat(links.insert.tokenHmac()).hasSize(64).isNotEqualTo(issued.token());
        assertThat(links.insert.toString()).doesNotContain(issued.token(), links.insert.tokenHmac());
    }

    private static final class FakeLinkRepository implements SecureLinkRepository {
        SecureLinkInsert insert;
        @Override public void insert(SecureLinkInsert value) { insert = value; }
        @Override public void revokeCurrent(UUID requestId, UUID actorId, Instant now, String reason) { }
        @Override public boolean activateRequest(UUID requestId, UUID linkId, UUID actorId, Instant now, long version) { return true; }
        @Override public boolean replaceCurrentLink(UUID requestId, UUID linkId, UUID actorId, Instant now, long version) { return true; }
    }

    private static final class FakeRequestRepository implements ConsentRequestRepository {
        private final ConsentRequestRecord row;
        FakeRequestRepository(ConsentRequestStatus status) {
            row = new ConsentRequestRecord(REQUEST, "CR-20260824-ABCDEFGH", UUID.randomUUID(),
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), UUID.randomUUID(), status, NOW.plusSeconds(7200),
                    UUID.randomUUID(), null, UUID.randomUUID(), ACTOR, NOW, 0);
        }
        @Override public Optional<ConsentRequestRecord> findByIdempotency(UUID a, UUID o, UUID k){return Optional.empty();}
        @Override public void insert(ConsentRequestRecord r, UUID a, Instant n){}
        @Override public Optional<ConsentRequestRecord> findScoped(UUID a, UUID id){return Optional.of(row);}
        @Override public Optional<ConsentRequestRecord> findScopedForUpdate(UUID a, UUID id){return Optional.of(row);}
        @Override public void insertSnapshot(UUID r, UUID s, RequestSnapshot v, UUID a, Instant n){}
        @Override public boolean markReady(UUID r, UUID s, UUID a, Instant n, long v){return false;}
        @Override public boolean cancel(UUID r, UUID a, Instant n, String reason, long v){return false;}
        @Override public void insertEvent(UUID r, String e, String t, UUID a, String reason, Instant n){}
        @Override public List<ConsentRequestRecord> searchScoped(UUID a, ConsentRequestStatus s){return List.of();}
    }
}
