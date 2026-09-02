package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.domain.service.CartService.CartLine;
import com.unicornt.store.domain.service.CartService.CartView;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartItemResponse;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test of the pure static translation between the priced cart and the cart DTOs. */
class CartMapperTest {

    private static CartLine aLine() {
        return new CartLine(45L, 12, "Unicorn plush", "unicorn-plush",
                new BigDecimal("14990"), 2, new BigDecimal("29980"));
    }

    @Nested
    @DisplayName("toResponse(CartLine)")
    class LineMapping {

        @Test
        @DisplayName("passes every priced field of the line through unchanged")
        void mapsLine() {
            CartItemResponse response = CartMapper.toResponse(aLine());

            assertThat(response.id()).isEqualTo(45L);
            assertThat(response.productId()).isEqualTo(12);
            assertThat(response.productName()).isEqualTo("Unicorn plush");
            assertThat(response.imageBase()).isEqualTo("unicorn-plush");
            assertThat(response.unitPrice()).isEqualByComparingTo("14990");
            assertThat(response.quantity()).isEqualTo(2);
            assertThat(response.subtotal()).isEqualByComparingTo("29980");
        }
    }

    @Nested
    @DisplayName("toResponse(CartView)")
    class CartMapping {

        @Test
        @DisplayName("passes the aggregate itemCount and total through unchanged and maps every line")
        void mapsCart() {
            CartView cart = new CartView(List.of(aLine()), 3, new BigDecimal("44970"));

            CartResponse response = CartMapper.toResponse(cart);

            assertThat(response.itemCount()).isEqualTo(3);
            assertThat(response.total()).isEqualByComparingTo("44970");
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).id()).isEqualTo(45L);
        }

        @Test
        @DisplayName("returns an empty item list for an empty cart while still passing the aggregates through")
        void emptyCart() {
            CartView cart = new CartView(List.of(), 0, BigDecimal.ZERO);

            CartResponse response = CartMapper.toResponse(cart);

            assertThat(response.items()).isEmpty();
            assertThat(response.itemCount()).isZero();
            assertThat(response.total()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("maps several lines in order")
        void mapsSeveralLines() {
            CartLine second = new CartLine(46L, 99, "Sticker", "sticker",
                    new BigDecimal("990"), 1, new BigDecimal("990"));
            CartView cart = new CartView(List.of(aLine(), second), 3, new BigDecimal("30970"));

            CartResponse response = CartMapper.toResponse(cart);

            assertThat(response.items()).extracting(CartItemResponse::id)
                    .containsExactly(45L, 46L);
        }
    }
}
