package kr.or.arte.seal.evidence.application;

import java.util.List;

public record GeneratedEvidence(String relativePath, String manifestSha256, String manifestHmac,
        String hmacKeyId, List<EvidenceFileMetadata> files) { public GeneratedEvidence { files = List.copyOf(files); } }
