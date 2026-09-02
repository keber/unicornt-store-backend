package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductCreateRequest;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductResponse;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test of the pure static translation between product records and the product entity. */
class ProductMapperTest {

    // ---------------------------------------------------------------
    // Object mothers
    // ---------------------------------------------------------------

    private static ProductEntity aProductEntity() {
        ProductEntity entity = new ProductEntity();
        entity.setId(42);
        entity.setName("Unicorn hoodie");
        entity.setDescription("Cotton hoodie");
        entity.setImageBase("hoodie-unicorn");
        entity.setPrice(25990);
        entity.setCategoryId(3);
        entity.setCategoryName("Hoodies");
        entity.setProductTypeId(1);
        entity.setProductTypeName("Apparel");
        entity.setActive(true);
        return entity;
    }

    private static ProductCreateRequest aCreateRequest(String name, Integer price, Integer categoryId,
                                                       Integer productTypeId, Boolean active) {
        return new ProductCreateRequest(name, "desc", "img", price, categoryId, productTypeId, active);
    }

    private static ProductUpdateRequest anUpdateRequest(String name, Integer price, Integer categoryId,
                                                        Integer productTypeId, Boolean active) {
        return new ProductUpdateRequest(name, "desc", "img", price, categoryId, productTypeId, active);
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("copies every field of the entity onto the response record")
        void mapsAllFields() {
            ProductResponse response = ProductMapper.toResponse(aProductEntity());

            assertThat(response.id()).isEqualTo(42);
            assertThat(response.name()).isEqualTo("Unicorn hoodie");
            assertThat(response.description()).isEqualTo("Cotton hoodie");
            assertThat(response.imageBase()).isEqualTo("hoodie-unicorn");
            assertThat(response.price()).isEqualTo(25990);
            assertThat(response.categoryId()).isEqualTo(3);
            assertThat(response.categoryName()).isEqualTo("Hoodies");
            assertThat(response.productTypeId()).isEqualTo(1);
            assertThat(response.productTypeName()).isEqualTo("Apparel");
            assertThat(response.active()).isTrue();
        }

        @Test
        @DisplayName("preserves an inactive product")
        void preservesInactive() {
            ProductEntity entity = aProductEntity();
            entity.setActive(false);

            assertThat(ProductMapper.toResponse(entity).active()).isFalse();
        }

        @Test
        @DisplayName("renders unset transient names as the empty string the entity exposes")
        void unsetTransientNames() {
            ProductEntity entity = new ProductEntity();
            entity.setName("bare");

            ProductResponse response = ProductMapper.toResponse(entity);

            assertThat(response.categoryName()).isEmpty();
            assertThat(response.productTypeName()).isEmpty();
            assertThat(response.description()).isEmpty();
            assertThat(response.imageBase()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toEntity(ProductCreateRequest)")
    class ToEntityFromCreate {

        @Test
        @DisplayName("trims the name and passes the remaining fields through")
        void trimsName() {
            ProductEntity entity = ProductMapper.toEntity(
                    aCreateRequest("  Unicorn hoodie  ", 25990, 3, 1, true));

            assertThat(entity.getName()).isEqualTo("Unicorn hoodie");
            assertThat(entity.getPrice()).isEqualTo(25990);
            assertThat(entity.getCategoryId()).isEqualTo(3);
            assertThat(entity.getProductTypeId()).isEqualTo(1);
            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("keeps a null name as null")
        void nullName() {
            ProductEntity entity = ProductMapper.toEntity(
                    aCreateRequest(null, 1, 1, 1, true));

            assertThat(entity.getName()).isNull();
        }

        @Test
        @DisplayName("maps null price, categoryId and productTypeId to zero")
        void nullNumericsBecomeZero() {
            ProductEntity entity = ProductMapper.toEntity(
                    aCreateRequest("x", null, null, null, true));

            assertThat(entity.getPrice()).isZero();
            assertThat(entity.getCategoryId()).isZero();
            assertThat(entity.getProductTypeId()).isZero();
        }

        @Test
        @DisplayName("defaults a null active flag to true")
        void nullActiveBecomesTrue() {
            ProductEntity entity = ProductMapper.toEntity(
                    aCreateRequest("x", 1, 1, 1, null));

            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("preserves an explicit false active flag")
        void falseActivePreserved() {
            ProductEntity entity = ProductMapper.toEntity(
                    aCreateRequest("x", 1, 1, 1, false));

            assertThat(entity.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("toEntity(ProductUpdateRequest)")
    class ToEntityFromUpdate {

        @Test
        @DisplayName("trims the name and passes the remaining fields through")
        void trimsName() {
            ProductEntity entity = ProductMapper.toEntity(
                    anUpdateRequest("  Unicorn hoodie  ", 25990, 3, 1, true));

            assertThat(entity.getName()).isEqualTo("Unicorn hoodie");
            assertThat(entity.getPrice()).isEqualTo(25990);
            assertThat(entity.getCategoryId()).isEqualTo(3);
            assertThat(entity.getProductTypeId()).isEqualTo(1);
            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("keeps a null name as null")
        void nullName() {
            ProductEntity entity = ProductMapper.toEntity(
                    anUpdateRequest(null, 1, 1, 1, true));

            assertThat(entity.getName()).isNull();
        }

        @Test
        @DisplayName("maps null price, categoryId and productTypeId to zero")
        void nullNumericsBecomeZero() {
            ProductEntity entity = ProductMapper.toEntity(
                    anUpdateRequest("x", null, null, null, true));

            assertThat(entity.getPrice()).isZero();
            assertThat(entity.getCategoryId()).isZero();
            assertThat(entity.getProductTypeId()).isZero();
        }

        @Test
        @DisplayName("defaults a null active flag to true")
        void nullActiveBecomesTrue() {
            ProductEntity entity = ProductMapper.toEntity(
                    anUpdateRequest("x", 1, 1, 1, null));

            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("preserves an explicit false active flag")
        void falseActivePreserved() {
            ProductEntity entity = ProductMapper.toEntity(
                    anUpdateRequest("x", 1, 1, 1, false));

            assertThat(entity.isActive()).isFalse();
        }
    }
}
