package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;

import java.time.Instant;
import java.util.UUID;

public record EvidencePackageListItem(UUID packageId, String requestCode, String templateTitle,
        String maskedName, String maskedEmail, String responseSummary,
        EvidenceGenerationStatus status, Instant queuedAt, Instant finalizedAt,
        int attemptCount, String lastErrorCode) {

    public static String summarizeResponse(int agreedCount, int decisionCount) {
        if (decisionCount <= 0) return "응답 정보 없음";
        if (agreedCount == decisionCount) return "전체 동의";
        if (agreedCount == 0) return "전체 미동의";
        return "일부 미동의";
    }
}
