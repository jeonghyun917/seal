package kr.or.arte.seal.retention.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import static org.assertj.core.api.Assertions.*;

class RetentionDueDateCalculatorTest {
 @Test void preservesJavaCalendarEndOfMonthRules(){var c=new RetentionDueDateCalculator();assertThat(c.calculate(LocalDate.of(2024,2,29),1,ChronoUnit.YEARS,0)).isEqualTo(LocalDate.of(2025,2,28));assertThat(c.calculate(LocalDate.of(2026,1,31),1,ChronoUnit.MONTHS,3)).isEqualTo(LocalDate.of(2026,3,3));}
 @Test void refusesToGuessMissingAnchor(){assertThatThrownBy(()->new RetentionDueDateCalculator().requireAnchor(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("ANCHOR_REQUIRED");}
}
