package com.gdn.project.waroenk.cart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkout entity - represents validated/locked items for checkout.
 * Lifecycle: WAITING → PAID | CANCELLED | EXPIRED
 * 
 * Status meanings:
 * - WAITING: Checkout created, inventory locked, waiting for payment
 * - PAID: Payment successful, inventory deducted permanently
 * - CANCELLED: User cancelled or system cancelled, inventory released
 * - EXPIRED: Derived status (not stored) - WAITING past expiresAt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "checkout_items")
@CompoundIndexes({
    @CompoundIndex(name = "idx_checkout_user_created", def = "{'userId': 1, 'createdAt': -1}")
})
public class Checkout {
    @Id
    private String id;

    @Indexed(unique = true)
    private String checkoutId;

    @Indexed
    private String userId;

    @Indexed
    private String orderId;          // Readable order ID (e.g., ORD-20241205-XXXX)

    private String paymentCode;      // Unique payment code for this checkout

    private String sourceCartId;     // Reference to original cart

    @Builder.Default
    private List<CheckoutItem> items = new ArrayList<>();

    private Long totalPrice;         // Total price of all items

    private String currency;

    @Builder.Default
    private String status = "WAITING"; // WAITING, PAID, CANCELLED (EXPIRED is derived)

    private AddressSnapshot shippingAddress;  // Snapshot of shipping address

    private Instant lockedAt;        // When inventory was locked

    // Note: Index is managed by migration V002_CheckoutSystemEnhancements
    // Do NOT add @Indexed here - it conflicts with the old TTL index
    private Instant expiresAt;       // When checkout expires (for scheduled cleanup)

    @CreatedDate
    private Instant createdAt;

    private Instant paidAt;          // When payment was confirmed

    private Instant cancelledAt;     // When checkout was cancelled

    /**
     * Calculate total price from items
     */
    public Long calculateTotalPrice() {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getReserved())) // Only count reserved items
                .mapToLong(item -> (item.getPriceSnapshot() != null ? item.getPriceSnapshot() : 0L)
                        * (item.getQuantity() != null ? item.getQuantity() : 0))
                .sum();
    }

    /**
     * Check if checkout has expired (WAITING status past expiresAt)
     */
    public boolean isExpired() {
        return "WAITING".equals(status) && expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Get effective status (returns EXPIRED if WAITING past expiresAt)
     */
    public String getEffectiveStatus() {
        if (isExpired()) {
            return "EXPIRED";
        }
        return status;
    }

    /**
     * Check if all items are successfully reserved
     */
    public boolean isFullyReserved() {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream().allMatch(item -> Boolean.TRUE.equals(item.getReserved()));
    }

    /**
     * Check if at least one item is reserved
     */
    public boolean hasReservedItems() {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream().anyMatch(item -> Boolean.TRUE.equals(item.getReserved()));
    }

    /**
     * Get count of reserved items
     */
    public long getReservedItemCount() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream().filter(item -> Boolean.TRUE.equals(item.getReserved())).count();
    }
}





