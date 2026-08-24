package kr.or.arte.seal.recipient.crypto;

public final class PersonalDataMasker {
    private PersonalDataMasker() {
    }

    public static String name(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }
        int[] codePoints = value.trim().codePoints().toArray();
        if (codePoints.length == 1) {
            return "*";
        }
        StringBuilder masked = new StringBuilder().appendCodePoint(codePoints[0]);
        int maskCount = codePoints.length == 2 ? 1 : codePoints.length - 2;
        masked.append("*".repeat(maskCount));
        if (codePoints.length > 2) {
            masked.appendCodePoint(codePoints[codePoints.length - 1]);
        }
        return masked.toString();
    }

    public static String email(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }
        String normalized = EmailNormalizer.normalize(value);
        int at = normalized.indexOf('@');
        if (at <= 0 || at == normalized.length() - 1) {
            return "***";
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        String domainName = dot > 0 ? domain.substring(0, dot) : domain;
        String suffix = dot > 0 ? domain.substring(dot) : "";
        return maskPart(local) + "@" + maskPart(domainName) + suffix;
    }

    private static String maskPart(String value) {
        if (value.length() <= 1) {
            return "*";
        }
        return value.charAt(0) + "*".repeat(value.length() - 1);
    }
}
