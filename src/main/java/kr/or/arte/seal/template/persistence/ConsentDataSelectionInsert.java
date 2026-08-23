package kr.or.arte.seal.template.persistence;

import java.util.UUID;

public record ConsentDataSelectionInsert(
        UUID itemId,
        UUID processingActivityVersionId,
        UUID processingDataItemId
) {
}
