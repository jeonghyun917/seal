package kr.or.arte.seal.recipient.importing;

import java.util.UUID;

public record RecipientImportRowRecord(UUID batchId, int rowNumber, String affiliationName,
        String jobTitle, String collectionBasis, byte[] nameCiphertext, byte[] emailCiphertext,
        String keyId, String emailHmac, String maskedName, String maskedEmail,
        RecipientImportRowStatus status, String errorCodes, UUID importedRecipientId) {
    public RecipientImportRowRecord {
        nameCiphertext = nameCiphertext == null ? null : nameCiphertext.clone();
        emailCiphertext = emailCiphertext == null ? null : emailCiphertext.clone();
    }
    @Override public byte[] nameCiphertext(){return nameCiphertext==null?null:nameCiphertext.clone();}
    @Override public byte[] emailCiphertext(){return emailCiphertext==null?null:emailCiphertext.clone();}
    public RecipientImportRowRecord imported(UUID id){return new RecipientImportRowRecord(batchId,rowNumber,null,null,
            null,null,null,null,null,maskedName,maskedEmail,RecipientImportRowStatus.IMPORTED,null,id);}
}