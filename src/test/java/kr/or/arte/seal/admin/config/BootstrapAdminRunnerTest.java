package kr.or.arte.seal.admin.config;

import kr.or.arte.seal.admin.application.BootstrapAdminCommand;
import kr.or.arte.seal.admin.application.BootstrapAdminRepository;
import kr.or.arte.seal.admin.domain.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapAdminRunnerTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void emptyRepositoryAndCompleteConfigurationCreateOneTemporaryAdministrator() throws Exception {
        RecordingRepository repository = new RecordingRepository(0);
        BootstrapAdminRunner runner = runner(
                repository,
                new BootstrapAdminProperties("seal-admin", "최초 관리자", "safe-password-12")
        );

        runner.run(new DefaultApplicationArguments());

        assertThat(repository.commands).hasSize(1);
        BootstrapAdminCommand command = repository.commands.get(0);
        assertThat(command.adminUserId()).isEqualTo(USER_ID);
        assertThat(command.loginId()).isEqualTo("seal-admin");
        assertThat(command.displayName()).isEqualTo("최초 관리자");
        assertThat(command.mustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches("safe-password-12", command.passwordHash())).isTrue();
    }

    @Test
    void existingAdministratorPreventsAdditionalBootstrapAccount() throws Exception {
        RecordingRepository repository = new RecordingRepository(1);

        runner(repository, new BootstrapAdminProperties(
                "seal-admin", "최초 관리자", "safe-password-12"
        )).run(new DefaultApplicationArguments());

        assertThat(repository.commands).isEmpty();
    }

    @Test
    void incompleteConfigurationDoesNotCreateAccount() throws Exception {
        RecordingRepository repository = new RecordingRepository(0);

        runner(repository, new BootstrapAdminProperties(
                "seal-admin", "최초 관리자", null
        )).run(new DefaultApplicationArguments());

        assertThat(repository.commands).isEmpty();
    }

    private BootstrapAdminRunner runner(
            BootstrapAdminRepository repository,
            BootstrapAdminProperties properties
    ) {
        return new BootstrapAdminRunner(
                repository,
                properties,
                new PasswordPolicy(12, 128),
                passwordEncoder,
                () -> USER_ID
        );
    }

    private static final class RecordingRepository implements BootstrapAdminRepository {
        private final long count;
        private final List<BootstrapAdminCommand> commands = new ArrayList<>();

        private RecordingRepository(long count) {
            this.count = count;
        }

        @Override
        public long countAdministrators() {
            return count;
        }

        @Override
        public void create(BootstrapAdminCommand command) {
            commands.add(command);
        }
    }
}
