package kr.or.arte.seal.recipient.importing;

public record RecipientImportRowView(int rowNumber, String affiliationName, String jobTitle,
                                     String collectionBasis, String maskedName, String maskedEmail,
                                     RecipientImportRowStatus status, String errorCodes) {
}