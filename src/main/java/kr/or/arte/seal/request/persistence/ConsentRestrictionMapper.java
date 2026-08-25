package kr.or.arte.seal.request.persistence;

import kr.or.arte.seal.request.application.ConsentRestrictionQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.UUID;

@Mapper
public interface ConsentRestrictionMapper extends ConsentRestrictionQuery {
    @Override
    @Select("""
            SELECT EXISTS(
                SELECT 1
                  FROM seal.legal_hold h
                  JOIN seal.consent_request r USING (consent_request_id)
                 WHERE r.recipient_id = #{recipientId}
                   AND h.active
                   AND h.hold_reason_code = 'RIGHTS_RESTRICTION'
                   AND (h.expires_on IS NULL OR h.expires_on >= #{today})
            )
            """)
    boolean hasActiveRestriction(@Param("recipientId") UUID recipientId,
            @Param("today") LocalDate today);
}
