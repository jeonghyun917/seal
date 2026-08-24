package kr.or.arte.seal.evidence.application;

public record EvidenceFileMetadata(String type, String fileName, String relativePath, long size, String sha256) { }
