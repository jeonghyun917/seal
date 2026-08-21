package kr.or.arte.seal.admin.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AdminAccountMapper {

    List<AdminAccountRow> findByLoginId(@Param("loginId") String loginId);

    int updateAuthenticationFailure(
            @Param("adminUserId") UUID adminUserId,
            @Param("consecutiveFailures") int consecutiveFailures,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("expectedVersion") long expectedVersion
    );

    int updateAuthenticationSuccess(
            @Param("adminUserId") UUID adminUserId,
            @Param("loggedInAt") Instant loggedInAt,
            @Param("expectedVersion") long expectedVersion
    );
}
