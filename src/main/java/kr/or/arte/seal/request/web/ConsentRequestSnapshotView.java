package kr.or.arte.seal.request.web;

import java.util.List;

public record ConsentRequestSnapshotView(
        String institution,
        String requestCode,
        String title,
        String introductionHtml,
        String processingActivityName,
        String processingPurpose,
        String retentionDisplayText,
        String expiresAtDisplay,
        String withdrawalNotice,
        String processingActivityVersionId,
        String retentionPolicyVersionId,
        String templateVersionId,
        String templateContentHash,
        String snapshotHash,
        List<Item> items) {

    public ConsentRequestSnapshotView {
        items = List.copyOf(items);
    }

    public record Item(
            int order,
            String code,
            String typeLabel,
            String title,
            boolean required,
            String purpose,
            String retentionText,
            String refusalNotice,
            String thirdPartyRecipient,
            List<String> dataItemNames) {

        public Item {
            dataItemNames = List.copyOf(dataItemNames);
        }
    }
}