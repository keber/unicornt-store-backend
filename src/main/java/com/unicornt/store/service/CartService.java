package com.unicornt.store.service;

import com.unicornt.store.model.CartItem;

import java.util.List;

public interface CartService {

    List<CartItem> getCartItems(String userEmail);

    void addToCart(String userEmail, int productId, int quantity);

    void updateQuantity(String userEmail, int productId, int quantity);

    void removeFromCart(String userEmail, int productId);

    void clearCart(String userEmail);

    int getCartCount(String userEmail);

    int getCartTotal(String userEmail);
}
