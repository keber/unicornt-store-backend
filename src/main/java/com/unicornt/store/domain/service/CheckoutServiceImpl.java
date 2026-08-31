package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderItemEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.OrderRepository;
import com.unicornt.store.infrastructure.persistence.repository.OrderStockRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final AddressService addressService;
    private final OrderRepository orderRepository;
    private final OrderStockRepository orderStockRepository;
    private final UserRepository userRepository;

    public CheckoutServiceImpl(CartService cartService,
                               AddressService addressService,
                               OrderRepository orderRepository,
                               OrderStockRepository orderStockRepository,
                               UserRepository userRepository) {
        this.cartService = cartService;
        this.addressService = addressService;
        this.orderRepository = orderRepository;
        this.orderStockRepository = orderStockRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OrderEntity confirm(String userEmail, Long addressId) {
        List<CartItemEntity> items = cartService.getCartItems(userEmail);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("The cart is empty");
        }

        AddressEntity address = addressService.findByUserAndId(userEmail, addressId);

        OrderEntity order = new OrderEntity();
        order.setUserId(getUserId(userEmail));
        order.setAddressId(address.getId());
        order.setShippingAddress(address.getFullAddress());
        order.setStatus(OrderEntity.OrderStatus.CONFIRMED);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemEntity item : items) {
            ProductEntity product = item.getProduct();
            reserveStock(product.getId(), item.getQuantity());

            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);
            order.addItem(new OrderItemEntity(product.getId(), product.getName(),
                    unitPrice, item.getQuantity(), subtotal));
        }
        order.setTotal(total);

        OrderEntity saved = orderRepository.save(order);
        cartService.clearCart(userEmail);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> findOrders(String userEmail) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(getUserId(userEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderEntity findOrder(String userEmail, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, getUserId(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    /**
     * Decrements inventory atomically. The conditional update is the stock check: when it
     * affects no row the product had less stock than requested.
     */
    private void reserveStock(int productId, int quantity) {
        if (orderStockRepository.decreaseStock(productId, quantity) == 0) {
            throw new OutOfStockException(productId);
        }
    }

    private Long getUserId(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
