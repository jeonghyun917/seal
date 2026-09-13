package kr.or.arte.seal.admin.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminHomeJspTest {

    @Test
    void showsScopedWorkItemsWithoutFixedHealthOrBuildClaims() throws Exception {
        String jsp = Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/admin/home.jsp"));

        assertThat(jsp).contains("현재 처리할 일", "현재 미처리 상태", "전체 기간", "내 조직 조회 범위 기준", "${workItem.href()}");
        assertThat(jsp).doesNotContain("정상 운영", "구축 완료");
    }
}
