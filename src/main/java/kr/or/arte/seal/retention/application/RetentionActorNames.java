package kr.or.arte.seal.retention.application;

public record RetentionActorNames(String requestedBy, String approvedBy,
        String executedBy, String confirmedBy) { }
