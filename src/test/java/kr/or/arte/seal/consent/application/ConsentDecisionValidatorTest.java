package kr.or.arte.seal.consent.application;

import org.junit.jupiter.api.Test;
import java.util.List; import java.util.Map; import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentDecisionValidatorTest {
    @Test void rejectsMissingAndUnknownItemIdentifiers(){
        UUID expected=UUID.randomUUID(); UUID unknown=UUID.randomUUID();
        assertThatThrownBy(()->ConsentDecisionValidator.validate(List.of(expected),Map.of(unknown,ConsentDecision.AGREE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("모든 동의 항목");
    }
}
