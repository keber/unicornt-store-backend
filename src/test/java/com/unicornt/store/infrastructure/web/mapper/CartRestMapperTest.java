package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.application.usecase.cart.MergeCartUseCase.IncomingItem;
import com.unicornt.store.application.usecase.cart.PricedCart;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartResponse;
import com.unicornt.store.infrastructure.web.dto.CartDtos.MergeCartItem;
import com.unicornt.store.infrastructure.web.dto.CartDtos.MergeCartRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartRestMapper")
class CartRestMapperTest {

    @Test
    @DisplayName("passes every priced field through and echoes the aggregates")
    void toResponseMapsEverything() {
        PricedCart cart = new PricedCart(List.of(
                new PricedCart.Line(12L, "Unicorn plush", "unicorn-plush",
                        Money.ofClp(14990), 2, Money.ofClp(29980)),
                new PricedCart.Line(20L, "Sticker", "sticker",
                        Money.ofClp(990), 1, Money.ofClp(990))),
                3, Money.ofClp(30970));

        CartResponse response = CartRestMapper.toResponse(cart);

        assertThat(response.itemCount()).isEqualTo(3);
        assertThat(response.total()).isEqualTo(30970);
        assertThat(response.items()).extracting("productId").containsExactly(12L, 20L);
        assertThat(response.items().get(0).unitPrice()).isEqualTo(14990);
        assertThat(response.items().get(0).subtotal()).isEqualTo(29980);
        assertThat(response.items().get(0).productName()).isEqualTo("Unicorn plush");
        assertThat(response.items().get(0).imageBase()).isEqualTo("unicorn-plush");
    }

    @Test
    @DisplayName("an empty priced cart maps to an empty response")
    void toResponseEmpty() {
        CartResponse response = CartRestMapper.toResponse(new PricedCart(List.of(), 0, Money.ofClp(0)));

        assertThat(response.items()).isEmpty();
        assertThat(response.itemCount()).isZero();
        assertThat(response.total()).isZero();
    }

    @Test
    @DisplayName("toIncomingItems unwraps every merge line")
    void toIncomingItems() {
        MergeCartRequest request = new MergeCartRequest(List.of(
                new MergeCartItem(10L, 2), new MergeCartItem(20L, 1)));

        List<IncomingItem> items = CartRestMapper.toIncomingItems(request);

        assertThat(items).containsExactly(new IncomingItem(10, 2), new IncomingItem(20, 1));
    }
}
