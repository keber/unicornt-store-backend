package com.unicornt.store.service;

import com.unicornt.store.dao.ProductDAO;
import com.unicornt.store.model.CartItem;
import com.unicornt.store.model.Product;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.CartItemRepository;
import com.unicornt.store.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductDAO productDAO;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           ProductDAO productDAO) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productDAO = productDAO;
    }

    @Override
    public List<CartItem> getCartItems(String userEmail) {
        Long userId = getUserId(userEmail);
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        for (CartItem item : items) {
            Product product = productDAO.findById(item.getProductId());
            item.setProduct(product);
        }
        items.removeIf(item -> item.getProduct() == null);
        return items;
    }

    @Override
    @Transactional
    public void addToCart(String userEmail, int productId, int quantity) {
        Long userId = getUserId(userEmail);
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            cartItemRepository.save(new CartItem(userId, productId, quantity));
        }
    }

    @Override
    @Transactional
    public void updateQuantity(String userEmail, int productId, int quantity) {
        Long userId = getUserId(userEmail);
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            if (quantity <= 0) {
                cartItemRepository.delete(existing.get());
            } else {
                CartItem item = existing.get();
                item.setQuantity(quantity);
                cartItemRepository.save(item);
            }
        }
    }

    @Override
    @Transactional
    public void removeFromCart(String userEmail, int productId) {
        Long userId = getUserId(userEmail);
        cartItemRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(cartItemRepository::delete);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        Long userId = getUserId(userEmail);
        cartItemRepository.deleteByUserId(userId);
    }

    @Override
    public int getCartCount(String userEmail) {
        Long userId = getUserId(userEmail);
        return cartItemRepository.sumQuantityByUserId(userId);
    }

    @Override
    public int getCartTotal(String userEmail) {
        List<CartItem> items = getCartItems(userEmail);
        return items.stream()
                .mapToInt(CartItem::getSubtotal)
                .sum();
    }

    private Long getUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
        return user.getId();
    }
}
