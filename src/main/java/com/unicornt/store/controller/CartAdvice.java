package com.unicornt.store.controller;

import com.unicornt.store.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CartAdvice {

    private final CartService cartService;

    public CartAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartCount")
    public int cartCount(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                return cartService.getCartCount(authentication.getName());
            } catch (RuntimeException e) {
                return 0;
            }
        }
        return 0;
    }
}
