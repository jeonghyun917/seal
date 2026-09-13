package kr.or.arte.seal.recipient.importing;

import java.util.List;

public record RecipientWorkbookRow(int rowNumber, String affiliationName, String jobTitle,
                                   String name, String normalizedEmail, String collectionBasis,
                                   List<String> errors) {
    public RecipientWorkbookRow {
        errors = List.copyOf(errors);
    }
    public boolean valid() { return errors.isEmpty(); }
}