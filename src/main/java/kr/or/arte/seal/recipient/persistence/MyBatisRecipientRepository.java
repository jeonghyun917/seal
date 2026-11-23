package kr.or.arte.seal.recipient.persistence;

import kr.or.arte.seal.recipient.application.RecipientRepository;
import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisRecipientRepository implements RecipientRepository {
    private final RecipientMapper mapper;

    public MyBatisRecipientRepository(RecipientMapper mapper) { this.mapper = mapper; }

    @Override public List<RecipientStoredRecord> findManyScoped(UUID actorId, List<UUID> ids) {
        return ids.isEmpty() ? List.of() : mapper.findManyScoped(actorId, ids);
    }

    @Override
    public boolean canAccessActiveOrganization(UUID actorId, UUID organizationId) {
        return mapper.canAccessActiveOrganization(actorId, organizationId);
    }

    @Override
    public Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID organizationId,
            String emailLookupHmac, UUID excludedRecipientId) {
        return mapper.findActiveByEmailHmac(organizationId, emailLookupHmac, excludedRecipientId)
                .stream().findFirst();
    }

    @Override
    public void insert(RecipientStoredRecord record, UUID actorId, Instant now) {
        if (mapper.insert(record, actorId, now) != 1) throw new IllegalStateException("대상자 저장에 실패했습니다.");
    }

    @Override
    public Optional<RecipientStoredRecord> findScoped(UUID actorId, UUID recipientId) {
        return mapper.findScoped(actorId, recipientId).stream().findFirst();
    }

    @Override
    public boolean update(RecipientStoredRecord record, UUID actorId, Instant now, long expectedVersion) {
        return mapper.update(record, actorId, now, expectedVersion) == 1;
    }

    @Override
    public List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
                                                    RecipientStatus status) {
        return mapper.searchScoped(actorId, emailLookupHmac, null, status, 100, 0);
    }
    @Override
    public List<RecipientStoredRecord> searchScoped(UUID actorId, String hmac, String collectionBasis,
            RecipientStatus status) {
        return mapper.searchScoped(actorId, hmac, collectionBasis, status, 100, 0);
    }
    public List<RecipientStoredRecord> searchScoped(UUID actorId,String hmac,RecipientStatus status,int limit,int offset){return mapper.searchScoped(actorId,hmac,null,status,limit,offset);}
    @Override
    public List<RecipientStoredRecord> searchScoped(UUID actorId, String hmac, String collectionBasis,
            RecipientStatus status, int limit, int offset) {
        return mapper.searchScoped(actorId, hmac, collectionBasis, status, limit, offset);
    }
}
