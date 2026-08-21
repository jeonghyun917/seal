package kr.or.arte.seal.admin.application;

public interface BootstrapAdminRepository {

    long countAdministrators();

    void create(BootstrapAdminCommand command);
}
