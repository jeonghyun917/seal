package kr.or.arte.seal.retention.application;
public record RetentionExecutionResult(int mailCount,int fileCount,boolean recipientPiiDestroyed){ }
