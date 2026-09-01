package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs {@link ProductRepository#SEARCH_QUERY} against an in-memory database so a typo in the
 * JPQL fails the build instead of the application start-up. The full Spring Data slice is not
 * available on this test classpath, so the persistence unit is bootstrapped by hand.
 */
class ProductSearchQueryTest {

    private static EntityManagerFactory factory;

    @BeforeAll
    static void bootstrap() {
        factory = new HibernatePersistenceConfiguration("catalog-search-test")
                .managedClass(ProductEntity.class)
                .managedClass(CategoryEntity.class)
                .jdbcUrl("jdbc:h2:mem:catalog-search;DB_CLOSE_DELAY=-1")
                .jdbcDriver("org.h2.Driver")
                .jdbcUsername("sa")
                .jdbcPassword("")
                .schemaToolingAction(org.hibernate.tool.schema.Action.CREATE_DROP)
                .createEntityManagerFactory();
    }

    @AfterAll
    static void shutdown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("Empty filters select every product")
    void emptyFiltersSelectEverything() {
        seed();
        assertThat(run("", "")).extracting(ProductEntity::getName)
                .containsExactlyInAnyOrder("Unicorn hoodie", "Dragon mug");
    }

    @Test
    @DisplayName("The category filter matches a slug and a name, case insensitively")
    void categoryFilterMatchesSlugAndName() {
        seed();
        assertThat(run("hoodies", "")).extracting(ProductEntity::getName)
                .containsExactly("Unicorn hoodie");
        assertThat(run("HOODIES", "")).hasSize(1);
        assertThat(run("Hoodies", "")).hasSize(1);
        assertThat(run("unknown", "")).isEmpty();
    }

    @Test
    @DisplayName("The free text filter matches the name and the description")
    void freeTextMatchesNameAndDescription() {
        seed();
        assertThat(run("", "unicorn")).extracting(ProductEntity::getName)
                .containsExactly("Unicorn hoodie");
        assertThat(run("", "ceramic")).extracting(ProductEntity::getName)
                .containsExactly("Dragon mug");
        assertThat(run("", "nothing here")).isEmpty();
    }

    private List<ProductEntity> run(String category, String q) {
        try (EntityManager em = factory.createEntityManager()) {
            return em.createQuery(ProductRepository.SEARCH_QUERY, ProductEntity.class)
                    .setParameter("category", category)
                    .setParameter("q", q)
                    .getResultList();
        }
    }

    private void seed() {
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("delete from ProductEntity").executeUpdate();
            em.createQuery("delete from CategoryEntity").executeUpdate();
            em.persist(category("Hoodies", "hoodies"));
            em.persist(category("Mugs", "mugs"));
            em.getTransaction().commit();

            em.getTransaction().begin();
            List<CategoryEntity> categories = em
                    .createQuery("select c from CategoryEntity c order by c.slug", CategoryEntity.class)
                    .getResultList();
            int hoodies = categories.stream().filter(c -> c.getSlug().equals("hoodies"))
                    .findFirst().orElseThrow().getId();
            int mugs = categories.stream().filter(c -> c.getSlug().equals("mugs"))
                    .findFirst().orElseThrow().getId();
            em.persist(product("Unicorn hoodie", "Cotton hoodie", hoodies));
            em.persist(product("Dragon mug", "Ceramic mug", mugs));
            em.getTransaction().commit();
        }
    }

    private CategoryEntity category(String name, String slug) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(name);
        entity.setSlug(slug);
        return entity;
    }

    private ProductEntity product(String name, String description, int categoryId) {
        ProductEntity entity = new ProductEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setPrice(1000);
        entity.setCategoryId(categoryId);
        entity.setProductTypeId(1);
        entity.setActive(true);
        return entity;
    }
}
