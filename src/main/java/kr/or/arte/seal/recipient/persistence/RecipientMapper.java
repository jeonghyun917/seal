package kr.or.arte.seal.recipient.persistence;

import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface RecipientMapper {
    boolean canAccessActiveOrganization(@Param("actorId") UUID actorId,
                                        @Param("organizationId") UUID organizationId);
    List<RecipientStoredRecord> findActiveByEmailHmac(@Param("organizationId") UUID organizationId,
                                                       @Param("emailLookupHmac") String emailLookupHmac,
                                                       @Param("excludedRecipientId") UUID excludedRecipientId);
    int insert(@Param("record") RecipientStoredRecord record, @Param("actorId") UUID actorId,
               @Param("now") Instant now);
    List<RecipientStoredRecord> findScoped(@Param("actorId") UUID actorId,
                                           @Param("recipientId") UUID recipientId);
    int update(@Param("record") RecipientStoredRecord record, @Param("actorId") UUID actorId,
               @Param("now") Instant now, @Param("expectedVersion") long expectedVersion);
    List<RecipientStoredRecord> searchScoped(@Param("actorId") UUID actorId,
                                             @Param("emailLookupHmac") String emailLookupHmac,
                                             @Param("collectionBasis") String collectionBasis,
                                             @Param("status") RecipientStatus status, @Param("limit") int limit,
                                             @Param("offset") int offset);
}
