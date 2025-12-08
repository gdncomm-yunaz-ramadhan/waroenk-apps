package com.gdn.project.waroenk.cart.repository;

import com.gdn.project.waroenk.cart.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repository for Cart entity operations.
 */
public interface CartRepository extends MongoRepository<Cart, String> {
    
    /**
     * Find cart by user ID
     */
    Optional<Cart> findByUserId(String userId);
    
    /**
     * Check if cart exists for user
     */
    boolean existsByUserId(String userId);
    
    /**
     * Delete cart by user ID
     */
    void deleteByUserId(String userId);
}


