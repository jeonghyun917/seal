package kr.or.arte.seal.rights.domain;

import java.util.*;

public final class RightsRequestTransition {
 private RightsRequestTransition(){}
 private static final Map<RightsRequestStatus,Set<RightsRequestStatus>> ALLOWED=Map.of(
  RightsRequestStatus.RECEIVED,Set.of(RightsRequestStatus.IDENTITY_VERIFIED,RightsRequestStatus.CANCELLED),
  RightsRequestStatus.IDENTITY_VERIFIED,Set.of(RightsRequestStatus.IN_REVIEW,RightsRequestStatus.CANCELLED),
  RightsRequestStatus.IN_REVIEW,Set.of(RightsRequestStatus.APPROVED,RightsRequestStatus.REJECTED,RightsRequestStatus.CANCELLED),
  RightsRequestStatus.APPROVED,Set.of(RightsRequestStatus.COMPLETED,RightsRequestStatus.CANCELLED),
  RightsRequestStatus.REJECTED,Set.of(RightsRequestStatus.COMPLETED));
 public static RightsRequestStatus next(RightsRequestStatus from,RightsRequestStatus to){if(!ALLOWED.getOrDefault(from,Set.of()).contains(to))throw new IllegalStateException("허용되지 않은 권리요청 상태 전이입니다: "+from+" -> "+to);return to;}
 public static void requireSeparateDecision(UUID creator,UUID decider){if(Objects.equals(creator,decider))throw new IllegalArgumentException("등록자와 결정자는 달라야 합니다.");}
}
