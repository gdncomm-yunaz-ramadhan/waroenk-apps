package com.gdn.project.waroenk.cart.repository;

import com.gdn.project.waroenk.cart.entity.Checkout;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Checkout entity operations.
 * Note: Primary storage is Redis, this is for MongoDB audit/persistence.
 */
public interface CheckoutRepository extends MongoRepository<Checkout, String> {
    
    /**
     * Find checkout by checkoutId
     */
    Optional<Checkout> findByCheckoutId(String checkoutId);
    
    /**
     * Find checkout by user ID (get active checkout for user)
     */
    Optional<Checkout> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Find all checkouts by user ID
     */
    List<Checkout> findByUserId(String userId);
    
    /**
     * Check if checkout exists
     */
    boolean existsByCheckoutId(String checkoutId);
    
    /**
     * Delete checkout by checkoutId
     */
    void deleteByCheckoutId(String checkoutId);
    
    /**
     * Find checkouts by status
     */
    List<Checkout> findByStatus(String status);
}











