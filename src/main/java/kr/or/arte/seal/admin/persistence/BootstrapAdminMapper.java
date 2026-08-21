package kr.or.arte.seal.admin.persistence;

import kr.or.arte.seal.admin.application.BootstrapAdminCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BootstrapAdminMapper {

    long countAdministrators();

    int insertAdministrator(@Param("admin") BootstrapAdminCommand command);

    int assignSystemAdminRole(@Param("admin") BootstrapAdminCommand command);

    int assignRootOrganizationScope(@Param("admin") BootstrapAdminCommand command);
}
