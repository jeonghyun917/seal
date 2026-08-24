package kr.or.arte.seal.consent.application;
import java.util.UUID;
public record ConsentSubmissionResult(UUID responseId,ConsentResponseStatus status,boolean existing) {}
