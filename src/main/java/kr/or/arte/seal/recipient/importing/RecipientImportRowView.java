package kr.or.arte.seal.recipient.importing;
public record RecipientImportRowView(int rowNumber,String maskedName,String maskedEmail,
                                     RecipientImportRowStatus status,String errorCodes) {}
