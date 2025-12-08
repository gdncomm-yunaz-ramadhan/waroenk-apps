package com.gdn.project.waroenk.cart.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart entity - represents the canonical snapshot of a user's cart.
 * Stored in MongoDB collection "cart_items".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "cart_items")
public class Cart {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Builder.Default
    private String currency = "IDR";

    @Version
    private Integer version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Calculate total amount of all items in cart.
     * @JsonIgnore prevents this computed property from being serialized to Redis cache,
     * avoiding deserialization errors (no setter exists for this computed field).
     */
    @JsonIgnore
    public Long getTotalAmount() {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        return items.stream()
                .mapToLong(item -> (item.getPriceSnapshot() != null ? item.getPriceSnapshot() : 0L) 
                        * (item.getQuantity() != null ? item.getQuantity() : 0))
                .sum();
    }

    /**
     * Calculate total number of items in cart.
     * @JsonIgnore prevents this computed property from being serialized to Redis cache,
     * avoiding deserialization errors (no setter exists for this computed field).
     */
    @JsonIgnore
    public Integer getTotalItems() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
    }

    /**
     * Add or update an item in the cart
     */
    public void addOrUpdateItem(CartItem newItem) {
        if (items == null) {
            items = new ArrayList<>();
        }
        
        // Find existing item by SKU
        for (int i = 0; i < items.size(); i++) {
            CartItem existingItem = items.get(i);
            if (existingItem.getSku().equals(newItem.getSku())) {
                // Update existing item
                existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
                if (newItem.getPriceSnapshot() != null) {
                    existingItem.setPriceSnapshot(newItem.getPriceSnapshot());
                }
                if (newItem.getTitle() != null) {
                    existingItem.setTitle(newItem.getTitle());
                }
                if (newItem.getImageUrl() != null) {
                    existingItem.setImageUrl(newItem.getImageUrl());
                }
                if (newItem.getAttributes() != null) {
                    existingItem.setAttributes(newItem.getAttributes());
                }
                return;
            }
        }
        
        // Add new item
        items.add(newItem);
    }

    /**
     * Remove item by SKU
     */
    public boolean removeItem(String sku) {
        if (items == null) {
            return false;
        }
        return items.removeIf(item -> item.getSku().equals(sku));
    }

    /**
     * Update item quantity
     */
    public boolean updateItemQuantity(String sku, Integer quantity) {
        if (items == null) {
            return false;
        }
        
        for (CartItem item : items) {
            if (item.getSku().equals(sku)) {
                if (quantity <= 0) {
                    // Remove item if quantity is 0 or negative
                    items.remove(item);
                } else {
                    item.setQuantity(quantity);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Clear all items
     */
    public void clearItems() {
        if (items != null) {
            items.clear();
        }
    }
}







