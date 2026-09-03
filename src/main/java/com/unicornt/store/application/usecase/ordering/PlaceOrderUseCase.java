package com.unicornt.store.application.usecase.ordering;

import com.unicornt.store.application.usecase.cart.ClearCartUseCase;
import com.unicornt.store.application.usecase.cart.GetCartUseCase;
import com.unicornt.store.application.usecase.cart.PricedCart;
import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.repository.OrderRepository;
import com.unicornt.store.domain.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirms the caller's cart into an order. One transaction, all or nothing:
 * <pre>
 *   load priced cart -&gt; assert non-empty
 *   for each line: decrement stock (fail -&gt; OutOfStockException, rolls everything back)
 *                  snapshot product name + unit price
 *   build Order (total computed in the domain) -&gt; save -&gt; clear the cart
 * </pre>
 * Payment is simulated, so the order is born {@code CONFIRMED}.
 */
@Service
public class PlaceOrderUseCase {

    private final GetCartUseCase getCart;
    private final ClearCartUseCase clearCart;
    private final StockRepository stock;
    private final OrderRepository orders;

    public PlaceOrderUseCase(GetCartUseCase getCart, ClearCartUseCase clearCart,
                             StockRepository stock, OrderRepository orders) {
        this.getCart = getCart;
        this.clearCart = clearCart;
        this.stock = stock;
        this.orders = orders;
    }

    @Transactional
    public OrderConfirmation execute(String userId, ShippingAddress shippingAddress) {
        PricedCart cart = getCart.execute(userId);
        if (cart.items().isEmpty()) {
            throw new IllegalArgumentException("The cart is empty");
        }

        List<OrderItem> lines = new ArrayList<>();
        for (PricedCart.Line line : cart.items()) {
            if (!stock.decreaseStock(line.productId(), line.quantity())) {
                throw new OutOfStockException(line.productId());
            }
            lines.add(new OrderItem(line.productId(), line.productName(), line.unitPrice(), line.quantity()));
        }

        Order saved = orders.save(Order.place(userId, shippingAddress, lines));
        clearCart.execute(userId);

        return new OrderConfirmation(saved.id(), saved.status().name(), saved.total().amount());
    }
}
