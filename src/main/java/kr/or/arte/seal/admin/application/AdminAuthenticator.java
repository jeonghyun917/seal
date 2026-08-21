package kr.or.arte.seal.admin.application;

import java.time.Instant;

@FunctionalInterface
public interface AdminAuthenticator {

    AuthenticationResult authenticate(String loginId, String password, Instant now);
}
