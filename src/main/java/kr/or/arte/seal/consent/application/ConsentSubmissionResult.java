package kr.or.arte.seal.consent.application;
import java.time.Instant;import java.util.UUID;
public record ConsentSubmissionResult(UUID responseId,ConsentResponseStatus status,boolean existing,Instant submittedAt) {}
