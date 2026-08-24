package kr.or.arte.seal.request.application;

import java.util.UUID;

public record ConsentRequestCreateResult(UUID requestId, String requestCode, boolean existing) { }
