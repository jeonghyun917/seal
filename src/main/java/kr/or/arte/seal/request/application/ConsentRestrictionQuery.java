package kr.or.arte.seal.request.application;

import java.time.LocalDate;
import java.util.UUID;

public interface ConsentRestrictionQuery {
    boolean hasActiveRestriction(UUID recipientId, LocalDate today);
}
