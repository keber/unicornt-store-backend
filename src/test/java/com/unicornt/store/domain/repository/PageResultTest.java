package com.unicornt.store.domain.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult")
class PageResultTest {

    @Test
    @DisplayName("computes the total number of pages, rounding up")
    void totalPagesRoundsUp() {
        assertThat(new PageResult<>(List.of("a"), 0, 20, 3).totalPages()).isEqualTo(1);
        assertThat(new PageResult<>(List.of("a"), 0, 2, 3).totalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of(), 0, 20, 0).totalPages()).isZero();
    }

    @Test
    @DisplayName("reports zero pages when the page size is not positive")
    void guardsAgainstZeroSize() {
        assertThat(new PageResult<>(List.of(), 0, 0, 5).totalPages()).isZero();
    }

    @Test
    @DisplayName("defensively copies its content")
    void copiesContent() {
        List<String> source = new ArrayList<>(List.of("a", "b"));

        PageResult<String> page = new PageResult<>(source, 0, 20, 2);
        source.add("c");

        assertThat(page.content()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("exposes page, size and total")
    void accessors() {
        PageResult<String> page = new PageResult<>(List.of("a"), 2, 10, 21);

        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(3);
    }
}
