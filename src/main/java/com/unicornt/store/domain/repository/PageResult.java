package com.unicornt.store.domain.repository;

import java.util.List;

/**
 * A single page of a larger result set, expressed in domain terms only &mdash; no
 * Spring {@code Page} or {@code Pageable} crosses into {@code application} or
 * {@code domain}.
 *
 * @param content       the items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements total number of items across every page
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
