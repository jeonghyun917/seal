package kr.or.arte.seal.admin.application;

public enum PasswordChangeStatus {
    CHANGED,
    CURRENT_PASSWORD_INVALID,
    CONFIRMATION_MISMATCH,
    POLICY_VIOLATION,
    ACCOUNT_NOT_FOUND,
    CONCURRENT_UPDATE
}
