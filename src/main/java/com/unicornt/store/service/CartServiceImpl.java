package com.unicornt.store.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.model.CartItem;
import com.unicornt.store.model.Product;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.CartItemRepository;
import com.unicornt.store.repository.ProductRepository;
import com.unicornt.store.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<CartItem> getCartItems(String userEmail) {
        Long userId = getUserId(userEmail);
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        items.forEach(item -> item.setProduct(productRepository.findById(item.getProductId()).orElse(null)));
        items.removeIf(item -> item.getProduct() == null);
        return items;
    }

    @Override
    @Transactional
    public void addToCart(String userEmail, int productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        Long userId = getUserId(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, product.getId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            cartItemRepository.save(new CartItem(userId, product.getId(), quantity));
        }
    }

    @Override
    @Transactional
    public void updateQuantity(String userEmail, int productId, int quantity) {
        Long userId = getUserId(userEmail);
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", productId));
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void removeFromCart(String userEmail, int productId) {
        Long userId = getUserId(userEmail);
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", productId));
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        cartItemRepository.deleteByUserId(getUserId(userEmail));
    }

    @Override
    public int getCartCount(String userEmail) {
        return cartItemRepository.sumQuantityByUserId(getUserId(userEmail));
    }

    @Override
    public int getCartTotal(String userEmail) {
        return getCartItems(userEmail).stream()
                .mapToInt(CartItem::getSubtotal)
                .sum();
    }

    private Long getUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
