package kr.or.arte.seal.request.application;

import java.util.List;

public record RequestSnapshot(String canonicalJson, String snapshotHash,
                              String templateContentHash, List<Item> items) {
    public RequestSnapshot { items = List.copyOf(items); }

    public record Item(int order, String code, String type, String title, boolean required,
                       String purpose, String retentionText, String refusalNotice,
                       String thirdPartyRecipient, List<DataItem> dataItems) {
        public Item { dataItems = List.copyOf(dataItems); }
    }

    public record DataItem(java.util.UUID id, int order, String name) { }
}
