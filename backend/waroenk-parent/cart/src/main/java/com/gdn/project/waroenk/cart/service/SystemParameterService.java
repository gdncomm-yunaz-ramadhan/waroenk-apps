package com.gdn.project.waroenk.cart.service;

import com.gdn.project.waroenk.cart.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.cart.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.cart.entity.SystemParameter;

/**
 * Service interface for system parameter operations.
 */
public interface SystemParameterService {
    
    /**
     * Create or update system parameter
     */
    SystemParameter upsert(SystemParameter param);
    
    /**
     * Get system parameter by variable name
     */
    SystemParameter get(String variable);
    
    /**
     * Delete system parameter
     */
    boolean delete(String variable);
    
    /**
     * Filter system parameters
     */
    MultipleSystemParameterResponse filter(MultipleSystemParameterRequest request);
    
    /**
     * Get value as String with default
     */
    String getString(String variable, String defaultValue);
    
    /**
     * Get value as Integer with default
     */
    Integer getInt(String variable, Integer defaultValue);
    
    /**
     * Get value as Long with default
     */
    Long getLong(String variable, Long defaultValue);
    
    /**
     * Get value as Boolean with default
     */
    Boolean getBoolean(String variable, Boolean defaultValue);
}










