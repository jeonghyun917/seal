package kr.or.arte.seal.recipient.importing;

import java.util.List;

public record RecipientWorkbookRow(int rowNumber, String name, String normalizedEmail,
                                   List<String> errors) {
    public RecipientWorkbookRow {
        errors = List.copyOf(errors);
    }
    public boolean valid() { return errors.isEmpty(); }
}
