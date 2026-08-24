package kr.or.arte.seal.recipient.crypto;

import java.text.Normalizer;
import java.util.Locale;

public final class EmailNormalizer {
    private EmailNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }
        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
        if (normalized.length() > 254) {
            throw new IllegalArgumentException("이메일은 254자 이하여야 합니다.");
        }
        return normalized;
    }
}
