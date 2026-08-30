package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;

import java.util.List;

public interface CartService {

    List<CartItemEntity> getCartItems(String userEmail);

    void addToCart(String userEmail, int productId, int quantity);

    void updateQuantity(String userEmail, int productId, int quantity);

    void removeFromCart(String userEmail, int productId);

    void clearCart(String userEmail);

    int getCartCount(String userEmail);

    int getCartTotal(String userEmail);
}
