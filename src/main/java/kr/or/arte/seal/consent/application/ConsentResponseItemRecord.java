package kr.or.arte.seal.consent.application;
import java.util.UUID;
public record ConsentResponseItemRecord(UUID responseId,UUID itemId,ConsentDecision decision,int order) {}
