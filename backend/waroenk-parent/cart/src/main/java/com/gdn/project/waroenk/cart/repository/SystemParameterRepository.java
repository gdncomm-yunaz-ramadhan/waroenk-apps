package com.gdn.project.waroenk.cart.repository;

import com.gdn.project.waroenk.cart.entity.SystemParameter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repository for SystemParameter entity operations.
 */
public interface SystemParameterRepository extends MongoRepository<SystemParameter, String> {
    
    /**
     * Find system parameter by variable name
     */
    Optional<SystemParameter> findByVariable(String variable);
    
    /**
     * Check if variable exists
     */
    boolean existsByVariable(String variable);
    
    /**
     * Delete by variable name
     */
    void deleteByVariable(String variable);
}



