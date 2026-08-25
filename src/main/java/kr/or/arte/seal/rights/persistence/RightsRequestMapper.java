package kr.or.arte.seal.rights.persistence;
import kr.or.arte.seal.rights.application.*;import kr.or.arte.seal.rights.domain.*;import org.apache.ibatis.annotations.*;import java.time.*;import java.util.*;
@Mapper public interface RightsRequestMapper {
 int insert(@Param("row")RightsRequestRecord row,@Param("actorId")UUID actorId);List<RightsRequestSummary> search(@Param("actorId")UUID actor,@Param("status")RightsRequestStatus status,@Param("limit")int limit,@Param("offset")int offset);Optional<RightsRequestRecord> findScoped(@Param("actorId")UUID actor,@Param("id")UUID id);Optional<RightsRequestRecord> findScopedForUpdate(@Param("actorId")UUID actor,@Param("id")UUID id);
 int transition(@Param("id")UUID id,@Param("from")RightsRequestStatus from,@Param("to")RightsRequestStatus to,@Param("actor")UUID actor,@Param("channel")String channel,@Param("verificationNote")byte[] verificationNote,@Param("verificationNoteKeyId")String verificationNoteKeyId,
                @Param("decisionCode")String decisionCode,@Param("resultNote")byte[] resultNote,@Param("resultNoteKeyId")String resultNoteKeyId,@Param("now")Instant now,@Param("version")long version);
 void event(@Param("eventId")UUID eventId,@Param("id")UUID id,@Param("from")RightsRequestStatus from,@Param("to")RightsRequestStatus to,@Param("actor")UUID actor,@Param("reason")String reason,@Param("now")Instant now);
 void withdrawal(@Param("id")UUID id,@Param("rightsId")UUID rightsId,@Param("responseId")UUID responseId,@Param("actor")UUID actor,@Param("now")Instant now);Optional<UUID> responseId(@Param("requestId")UUID requestId);
 int linkRetentionJob(@Param("rightsId")UUID rightsId,@Param("jobId")UUID jobId,@Param("version")long version);
}
