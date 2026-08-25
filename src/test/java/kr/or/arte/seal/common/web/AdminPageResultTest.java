package kr.or.arte.seal.common.web;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPageResultTest {
    @Test
    void usesOneLookaheadRowToExposeStablePreviousAndNextNavigation() {
        var fetched = IntStream.rangeClosed(1, 21).boxed().toList();

        AdminPageResult<Integer> page = AdminPageResult.of(fetched, 2, 20);

        assertThat(page.items()).hasSize(20).doesNotContain(21);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.offset()).isEqualTo(20);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
}
