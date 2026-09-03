package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for the {@code products} table. */
public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, Integer> {

    /** JPQL behind {@link #search}, exposed so a test can parse and run it against the database. */
    String SEARCH_QUERY = """
            select p from ProductJpaEntity p
            where (:q = '' or lower(p.name) like lower(concat('%', :q, '%'))
                           or lower(p.description) like lower(concat('%', :q, '%')))
              and (:category = '' or p.categoryId in (
                    select c.id from CategoryJpaEntity c
                    where lower(c.slug) = lower(:category) or lower(c.name) = lower(:category)))
            """;

    /**
     * Catalog search behind {@code GET /api/v1/products}. Both filters are optional and are
     * disabled by passing an empty string, which keeps the parameter types unambiguous for the
     * driver. {@code category} matches a category slug or name, case-insensitively; {@code q}
     * matches the product name or description.
     */
    @Query(SEARCH_QUERY)
    Page<ProductJpaEntity> search(@Param("category") String category,
                                 @Param("q") String q,
                                 Pageable pageable);
}
