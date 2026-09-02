package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/** Pure unit test of the cart aggregate's invariants and mutations (PLAN.md 2.4). */
@DisplayName("Cart")
class CartTest {

    private static final String USER = "buyer@unicornt.dev";

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("a cart is scoped to a non-blank user")
        void requiresUser() {
            assertThatIllegalArgumentException().isThrownBy(() -> Cart.empty(" "));
            assertThatIllegalArgumentException().isThrownBy(() -> new Cart(null, List.of()));
        }

        @Test
        @DisplayName("an empty cart has no lines, no units and reports empty")
        void emptyCart() {
            Cart cart = Cart.empty(USER);

            assertThat(cart.items()).isEmpty();
            assertThat(cart.isEmpty()).isTrue();
            assertThat(cart.totalUnits()).isZero();
            assertThat(cart.userId()).isEqualTo(USER);
        }

        @Test
        @DisplayName("a null seed list is treated as no lines")
        void nullSeedList() {
            Cart cart = new Cart(USER, null);

            assertThat(cart.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("duplicate products in the seed list are summed into one line")
        void seedListIsDeduplicated() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(10, 3), CartItem.of(20, 1)));

            assertThat(cart.items()).containsExactly(CartItem.of(10, 5), CartItem.of(20, 1));
        }

        @Test
        @DisplayName("the exposed line list is an unmodifiable snapshot")
        void itemsAreASnapshot() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 1)));

            List<CartItem> snapshot = cart.items();
            cart.addItem(20, Quantity.of(1));

            assertThat(snapshot).hasSize(1);
            assertThat(cart.items()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("adds a new line when the product is not in the cart")
        void addsNewLine() {
            Cart cart = Cart.empty(USER);

            cart.addItem(10, Quantity.of(3));

            assertThat(cart.findItem(10)).contains(CartItem.of(10, 3));
        }

        @Test
        @DisplayName("sums the quantity into the existing line and keeps its position")
        void sumsIntoExistingLine() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(20, 1)));

            cart.addItem(10, Quantity.of(3));

            assertThat(cart.items()).containsExactly(CartItem.of(10, 5), CartItem.of(20, 1));
            assertThat(cart.totalUnits()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("setItemQuantity")
    class SetItemQuantity {

        @Test
        @DisplayName("replaces the quantity of an existing line")
        void replacesQuantity() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 2)));

            cart.setItemQuantity(10, 5);

            assertThat(cart.findItem(10)).contains(CartItem.of(10, 5));
        }

        @Test
        @DisplayName("creates the line when the product is absent")
        void createsWhenAbsent() {
            Cart cart = Cart.empty(USER);

            cart.setItemQuantity(10, 4);

            assertThat(cart.findItem(10)).contains(CartItem.of(10, 4));
        }

        @Test
        @DisplayName("a target of zero removes the line")
        void zeroRemoves() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(20, 1)));

            cart.setItemQuantity(10, 0);

            assertThat(cart.findItem(10)).isEmpty();
            assertThat(cart.items()).containsExactly(CartItem.of(20, 1));
        }

        @Test
        @DisplayName("a negative target removes the line")
        void negativeRemoves() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 2)));

            cart.setItemQuantity(10, -3);

            assertThat(cart.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("removeItem / clear")
    class RemoveAndClear {

        @Test
        @DisplayName("removeItem reports whether a line was actually removed")
        void removeReportsHit() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 2)));

            assertThat(cart.removeItem(10)).isTrue();
            assertThat(cart.removeItem(10)).isFalse();
        }

        @Test
        @DisplayName("clear drops every line")
        void clearEmpties() {
            Cart cart = new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(20, 1)));

            cart.clear();

            assertThat(cart.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("belongsTo is true only for the owner id")
        void belongsToOwner() {
            Cart cart = Cart.empty(USER);

            assertThat(cart.belongsTo(USER)).isTrue();
            assertThat(cart.belongsTo("someone-else@unicornt.dev")).isFalse();
        }
    }

    @Nested
    @DisplayName("CartItem")
    class CartItemRules {

        @Test
        @DisplayName("a line must reference a positive product id")
        void requiresProduct() {
            assertThatIllegalArgumentException().isThrownBy(() -> CartItem.of(0, 1));
        }

        @Test
        @DisplayName("a line quantity is strictly positive")
        void requiresPositiveQuantity() {
            assertThatIllegalArgumentException().isThrownBy(() -> CartItem.of(10, 0));
        }

        @Test
        @DisplayName("a null quantity is rejected")
        void rejectsNullQuantity() {
            assertThatNullPointerException().isThrownBy(() -> new CartItem(10, null));
        }

        @Test
        @DisplayName("equality is by product id and quantity; toString names both")
        void equalityAndToString() {
            CartItem base = CartItem.of(10, 2);

            assertThat(base)
                    .isEqualTo(CartItem.of(10, 2))
                    .hasSameHashCodeAs(CartItem.of(10, 2))
                    .isNotEqualTo(CartItem.of(10, 3))
                    .isNotEqualTo(CartItem.of(20, 2))
                    .isNotEqualTo("not a cart item");
            assertThat(base.toString()).contains("10").contains("2");
        }
    }
}
