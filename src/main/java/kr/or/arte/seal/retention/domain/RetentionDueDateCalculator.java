package kr.or.arte.seal.retention.domain;

import java.time.LocalDate;import java.time.temporal.ChronoUnit;
public final class RetentionDueDateCalculator {
 public LocalDate calculate(LocalDate anchor,long amount,ChronoUnit unit,long graceDays){requireAnchor(anchor);LocalDate base=switch(unit){case DAYS->anchor.plusDays(amount);case MONTHS->anchor.plusMonths(amount);case YEARS->anchor.plusYears(amount);default->throw new IllegalArgumentException("지원하지 않는 보유기간 단위입니다.");};return base.plusDays(graceDays);}
 public LocalDate requireAnchor(LocalDate anchor){if(anchor==null)throw new IllegalStateException("ANCHOR_REQUIRED: 보유기간 기산일이 필요합니다.");return anchor;}
}
