package kr.or.arte.seal.template.application;

import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityReference;
import kr.or.arte.seal.template.domain.ConsentItemType;
import kr.or.arte.seal.template.domain.ConsentTemplateVersion;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TemplatePolicyValidator {

    public void validateAgainst(ConsentTemplateVersion version,
                                ApprovedProcessingActivityReference policy) {
        if (!version.processingActivityId().equals(policy.activityId())
                || !version.processingActivityVersionId().equals(policy.versionId())) {
            throw new TemplateBusinessConflictException("승인 처리업무 버전이 변경되었습니다.");
        }
        Set<UUID> approvedItemIds = new HashSet<>();
        policy.dataItems().forEach(item -> approvedItemIds.add(item.processingDataItemId()));
        for (int index = 0; index < version.items().size(); index++) {
            var item = version.items().get(index);
            if (item.order() != index + 1) {
                throw new TemplateBusinessConflictException("동의 항목 순서는 1부터 연속되어야 합니다.");
            }
            if (!approvedItemIds.containsAll(item.processingDataItemIds())) {
                throw new TemplateBusinessConflictException(
                        "승인 처리업무와 일치하지 않는 개인정보 항목이 있습니다.");
            }
            if (item.type() != ConsentItemType.THIRD_PARTY_PROVISION
                    && !item.retentionText().equals(policy.retentionDisplayText())) {
                throw new TemplateBusinessConflictException("보유기간 정책이 변경되었습니다.");
            }
        }
    }
}
