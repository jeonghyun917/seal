package kr.or.arte.seal.retention.domain;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class RetentionJobTransitionTest {
 @Test void followsDualControlLifecycle(){assertThat(RetentionJobTransition.next(RetentionJobStatus.DUE,RetentionJobStatus.PENDING_APPROVAL)).isEqualTo(RetentionJobStatus.PENDING_APPROVAL);assertThat(RetentionJobTransition.next(RetentionJobStatus.PENDING_APPROVAL,RetentionJobStatus.APPROVED)).isEqualTo(RetentionJobStatus.APPROVED);assertThat(RetentionJobTransition.next(RetentionJobStatus.EXECUTED,RetentionJobStatus.CONFIRMED)).isEqualTo(RetentionJobStatus.CONFIRMED);}
 @Test void separatesRequesterApproverAndExecutorConfirmer(){UUID actor=UUID.randomUUID();assertThatThrownBy(()->RetentionJobTransition.requireDifferent(actor,actor,"APPROVER")).isInstanceOf(IllegalArgumentException.class);}
 @Test void activeHoldBlocksApproval(){assertThatThrownBy(()->RetentionJobTransition.requireNoHold(true)).isInstanceOf(IllegalStateException.class);}
}
