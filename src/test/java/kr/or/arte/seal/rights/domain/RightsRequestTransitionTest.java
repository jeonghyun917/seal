package kr.or.arte.seal.rights.domain;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class RightsRequestTransitionTest {
 @Test void followsVerifiedReviewApprovedCompletedPath(){
  assertThat(RightsRequestTransition.next(RightsRequestStatus.RECEIVED,RightsRequestStatus.IDENTITY_VERIFIED)).isEqualTo(RightsRequestStatus.IDENTITY_VERIFIED);
  assertThat(RightsRequestTransition.next(RightsRequestStatus.IDENTITY_VERIFIED,RightsRequestStatus.IN_REVIEW)).isEqualTo(RightsRequestStatus.IN_REVIEW);
  assertThat(RightsRequestTransition.next(RightsRequestStatus.IN_REVIEW,RightsRequestStatus.APPROVED)).isEqualTo(RightsRequestStatus.APPROVED);
  assertThat(RightsRequestTransition.next(RightsRequestStatus.APPROVED,RightsRequestStatus.COMPLETED)).isEqualTo(RightsRequestStatus.COMPLETED);
 }
 @Test void rejectsReviewBeforeIdentityVerification(){assertThatThrownBy(()->RightsRequestTransition.next(RightsRequestStatus.RECEIVED,RightsRequestStatus.IN_REVIEW)).isInstanceOf(IllegalStateException.class);}
 @Test void rejectsCompletionBeforeApproval(){assertThatThrownBy(()->RightsRequestTransition.next(RightsRequestStatus.IN_REVIEW,RightsRequestStatus.COMPLETED)).isInstanceOf(IllegalStateException.class);}
 @Test void rejectedRequestCanCompleteAfterNotification(){assertThat(RightsRequestTransition.next(RightsRequestStatus.REJECTED,RightsRequestStatus.COMPLETED)).isEqualTo(RightsRequestStatus.COMPLETED);}
 @Test void requiresDifferentCreatorAndDecisionMaker(){UUID actor=UUID.randomUUID();assertThatThrownBy(()->RightsRequestTransition.requireSeparateDecision(actor,actor)).isInstanceOf(IllegalArgumentException.class);}
}
