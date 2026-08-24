package kr.or.arte.seal.recipient.importing;

import java.util.List;

public record ParsedRecipientWorkbook(List<RecipientWorkbookRow> rows) {
    public ParsedRecipientWorkbook { rows = List.copyOf(rows); }
}
