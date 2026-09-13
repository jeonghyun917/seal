package kr.or.arte.seal.consent.application;

import kr.or.arte.seal.common.web.DateTimeLabels;
import java.time.Instant;
import java.util.List;

public record PublicConsentReceipt(String title, Instant submittedAt, List<Item> items,
        String withdrawalNotice) {
    public PublicConsentReceipt { items = List.copyOf(items); }
    public String submittedAtLabel() { return DateTimeLabels.format(submittedAt); }
    public record Item(String title, String decisionLabel) { }
}
