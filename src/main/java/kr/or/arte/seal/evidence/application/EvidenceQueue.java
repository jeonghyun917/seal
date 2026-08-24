package kr.or.arte.seal.evidence.application;

import java.time.Instant;
import java.util.UUID;

@FunctionalInterface
public interface EvidenceQueue { void enqueue(UUID responseId, UUID requestId, Instant queuedAt); }
