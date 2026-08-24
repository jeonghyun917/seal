package kr.or.arte.seal.link.persistence;

import kr.or.arte.seal.link.application.SecureLinkInsert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.UUID;

@Mapper
public interface SecureLinkMapper {
    int insert(@Param("value") SecureLinkInsert value);
    int revokeCurrent(@Param("requestId") UUID requestId, @Param("actorId") UUID actorId,
                      @Param("now") Instant now, @Param("reason") String reason);
    int activateRequest(@Param("requestId") UUID requestId, @Param("linkId") UUID linkId,
                        @Param("actorId") UUID actorId, @Param("now") Instant now,
                        @Param("versionNo") long versionNo);
    int replaceCurrentLink(@Param("requestId") UUID requestId, @Param("linkId") UUID linkId,
                           @Param("actorId") UUID actorId, @Param("now") Instant now,
                           @Param("versionNo") long versionNo);
    int expireCurrent(@Param("requestId") UUID requestId, @Param("now") Instant now);
}
