package kr.or.arte.seal.admin.config;

import kr.or.arte.seal.admin.application.BootstrapAdminCommand;
import kr.or.arte.seal.admin.application.BootstrapAdminRepository;
import kr.or.arte.seal.admin.domain.PasswordPolicy;
import kr.or.arte.seal.admin.domain.PasswordValidationResult;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9.-]{4,50}$");

    private final BootstrapAdminRepository repository;
    private final BootstrapAdminProperties properties;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final Supplier<UUID> uuidSupplier;

    @Autowired
    public BootstrapAdminRunner(
            BootstrapAdminRepository repository,
            BootstrapAdminProperties properties,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder
    ) {
        this(repository, properties, passwordPolicy, passwordEncoder, UUID::randomUUID);
    }

    BootstrapAdminRunner(
            BootstrapAdminRepository repository,
            BootstrapAdminProperties properties,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            Supplier<UUID> uuidSupplier
    ) {
        this.repository = repository;
        this.properties = properties;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.uuidSupplier = uuidSupplier;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.complete() || repository.countAdministrators() > 0) {
            return;
        }

        String loginId = properties.id().strip().toLowerCase(Locale.ROOT);
        String displayName = properties.name().strip();
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches() || displayName.isEmpty()) {
            throw new IllegalStateException("bootstrap administrator ID or name is invalid");
        }

        PasswordValidationResult validation = passwordPolicy.validate(loginId, properties.password());
        if (!validation.valid()) {
            throw new IllegalStateException("bootstrap administrator password violates the configured policy");
        }

        repository.create(new BootstrapAdminCommand(
                uuidSupplier.get(),
                uuidSupplier.get(),
                loginId,
                displayName,
                passwordEncoder.encode(properties.password()),
                true
        ));
    }
}
