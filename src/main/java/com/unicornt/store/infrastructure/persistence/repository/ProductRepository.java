package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {

    List<ProductEntity> findByNameContainingIgnoreCase(String name, Sort sort);

    Page<ProductEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    long countByNameContainingIgnoreCase(String name);

    List<ProductEntity> findByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId, Sort sort);

    Page<ProductEntity> findByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId, Pageable pageable);

    long countByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId);

    /** JPQL behind {@link #search}, exposed so a test can parse and run it against the database. */
    String SEARCH_QUERY = """
            select p from ProductEntity p
            where (:q = '' or lower(p.name) like lower(concat('%', :q, '%'))
                           or lower(p.description) like lower(concat('%', :q, '%')))
              and (:category = '' or p.categoryId in (
                    select c.id from CategoryEntity c
                    where lower(c.slug) = lower(:category) or lower(c.name) = lower(:category)))
            """;

    /**
     * Catalog search used by {@code GET /api/v1/products}. Both filters are optional and are
     * disabled by passing an empty string, which keeps the parameter types unambiguous for the
     * driver. {@code category} matches a category slug or a category name, case insensitively;
     * {@code q} matches the product name or description.
     */
    @Query(SEARCH_QUERY)
    Page<ProductEntity> search(@Param("category") String category,
                               @Param("q") String q,
                               Pageable pageable);
}
