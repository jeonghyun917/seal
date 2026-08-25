package kr.or.arte.seal.evidence.application;

public final class EvidenceClaimLostException extends IllegalStateException {
    public EvidenceClaimLostException() {
        this("The evidence processing claim is no longer owned by this worker.");
    }

    public EvidenceClaimLostException(String message) {
        super(message);
    }
}
