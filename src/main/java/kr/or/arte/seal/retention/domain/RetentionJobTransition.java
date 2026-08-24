package kr.or.arte.seal.retention.domain;

import java.util.*;
public final class RetentionJobTransition {
 private RetentionJobTransition(){}
 private static final Map<RetentionJobStatus,Set<RetentionJobStatus>> ALLOWED=Map.of(
  RetentionJobStatus.DUE,Set.of(RetentionJobStatus.PENDING_APPROVAL,RetentionJobStatus.HELD,RetentionJobStatus.CANCELLED),
  RetentionJobStatus.PENDING_APPROVAL,Set.of(RetentionJobStatus.APPROVED,RetentionJobStatus.HELD,RetentionJobStatus.CANCELLED),
  RetentionJobStatus.APPROVED,Set.of(RetentionJobStatus.EXECUTING,RetentionJobStatus.HELD),
  RetentionJobStatus.EXECUTING,Set.of(RetentionJobStatus.EXECUTED,RetentionJobStatus.FAILED),
  RetentionJobStatus.EXECUTED,Set.of(RetentionJobStatus.CONFIRMED));
 public static RetentionJobStatus next(RetentionJobStatus from,RetentionJobStatus to){if(!ALLOWED.getOrDefault(from,Set.of()).contains(to))throw new IllegalStateException("허용되지 않은 파기 상태 전이입니다: "+from+" -> "+to);return to;}
 public static void requireDifferent(UUID first,UUID second,String role){if(Objects.equals(first,second))throw new IllegalArgumentException(role+" 역할은 다른 관리자가 수행해야 합니다.");}
 public static void requireNoHold(boolean active){if(active)throw new IllegalStateException("LEGAL_HOLD_ACTIVE: 보존 보류 중입니다.");}
}
