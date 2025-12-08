package com.gdn.project.waroenk.cart.service;

/**
 * Service interface for cache operations.
 */
public interface CacheService {
    boolean flushAll();
    boolean flushCacheWithPattern(String pattern);
}


