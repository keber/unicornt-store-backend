package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.CartItemRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final SpringDataProductRepository productRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           SpringDataProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView getCart(String userEmail) {
        List<CartLine> lines = getCartItems(userEmail).stream()
                .map(this::toLine)
                .toList();
        int itemCount = lines.stream().mapToInt(CartLine::quantity).sum();
        BigDecimal total = lines.stream()
                .map(CartLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(lines, itemCount, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemEntity> getCartItems(String userEmail) {
        Long userId = getUserId(userEmail);
        List<CartItemEntity> items = new ArrayList<>(cartItemRepository.findByUserId(userId));
        items.forEach(item -> item.setProduct(productRepository.findById(item.getProductId()).orElse(null)));
        items.removeIf(item -> item.getProduct() == null);
        return items;
    }

    @Override
    @Transactional
    public CartLine addItem(String userEmail, int productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        Long userId = getUserId(userEmail);
        ProductJpaEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        Optional<CartItemEntity> existing = cartItemRepository.findByUserIdAndProductId(userId, product.getId());
        CartItemEntity item = existing
                .map(found -> {
                    found.setQuantity(found.getQuantity() + quantity);
                    return found;
                })
                .orElseGet(() -> new CartItemEntity(userId, product.getId(), quantity));

        CartItemEntity saved = cartItemRepository.save(item);
        saved.setProduct(product);
        return toLine(saved);
    }

    @Override
    @Transactional
    public CartLine updateItemQuantity(String userEmail, Long cartItemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        CartItemEntity item = requireOwnedItem(userEmail, cartItemId);
        item.setQuantity(quantity);
        CartItemEntity saved = cartItemRepository.save(item);
        saved.setProduct(productRepository.findById(saved.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", saved.getProductId())));
        return toLine(saved);
    }

    @Override
    @Transactional
    public void removeItem(String userEmail, Long cartItemId) {
        cartItemRepository.delete(requireOwnedItem(userEmail, cartItemId));
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        cartItemRepository.deleteByUserId(getUserId(userEmail));
    }

    /** A cart line of another user is reported as missing, never as forbidden. */
    private CartItemEntity requireOwnedItem(String userEmail, Long cartItemId) {
        Long userId = getUserId(userEmail);
        return cartItemRepository.findById(cartItemId)
                .filter(item -> userId.equals(item.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));
    }

    private CartLine toLine(CartItemEntity item) {
        ProductJpaEntity product = item.getProduct();
        BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartLine(item.getId(), product.getId(), product.getName(), product.getImageBase(),
                unitPrice, item.getQuantity(), subtotal);
    }

    private Long getUserId(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
