package com.unicornt.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(nullable = false)
    private int quantity;

    @Transient
    private Product product;

    public CartItem() {
    }

    public CartItem(Long userId, int productId, int quantity) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getSubtotal() {
        return product != null ? product.getPrice() * quantity : 0;
    }

    public String getFormattedSubtotal() {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.of("es", "CL"));
        return "$" + nf.format(getSubtotal());
    }
}
