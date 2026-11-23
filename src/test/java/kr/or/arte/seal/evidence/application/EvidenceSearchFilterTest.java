package kr.or.arte.seal.evidence.application;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class EvidenceSearchFilterTest {
 @Test void endDateIncludesTheEntireKoreanCalendarDay(){
  var filter=EvidenceSearchFilter.of("  교육  ",LocalDate.of(2026,9,1),LocalDate.of(2026,9,29));
  assertThat(filter.query()).isEqualTo("교육");
  assertThat(filter.fromInclusive()).isEqualTo(Instant.parse("2026-08-31T15:00:00Z"));
  assertThat(filter.untilExclusive()).isEqualTo(Instant.parse("2026-09-29T15:00:00Z"));
 }
 @Test void backwardsRangeIsRejected(){
  assertThatThrownBy(()->EvidenceSearchFilter.of(null,LocalDate.of(2026,9,30),LocalDate.of(2026,9,1)))
    .isInstanceOf(IllegalArgumentException.class);
 }
 @Test void blankFiltersRemainUnrestricted(){
  var filter=EvidenceSearchFilter.of(" ",null,null);
  assertThat(filter.query()).isNull();assertThat(filter.fromInclusive()).isNull();assertThat(filter.untilExclusive()).isNull();
 }
}
