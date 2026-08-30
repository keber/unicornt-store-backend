package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final AddressService addressService;

    public CheckoutServiceImpl(CartService cartService, AddressService addressService) {
        this.cartService = cartService;
        this.addressService = addressService;
    }

    @Override
    @Transactional
    public OrderSummary confirm(String userEmail, Long addressId) {
        List<CartItemEntity> items = cartService.getCartItems(userEmail);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("The cart is empty");
        }

        AddressEntity address = addressService.findByUserAndId(userEmail, addressId);
        int total = items.stream().mapToInt(CartItemEntity::getSubtotal).sum();

        cartService.clearCart(userEmail);

        return new OrderSummary(address.getFullAddress(), total, items.size(), List.copyOf(items));
    }
}
