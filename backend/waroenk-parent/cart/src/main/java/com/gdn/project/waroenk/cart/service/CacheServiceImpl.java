package com.gdn.project.waroenk.cart.service;

import com.gdn.project.waroenk.cart.utility.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    private final CacheUtil<String> cacheUtil;

    @Override
    public boolean flushAll() {
        return cacheUtil.flushAll();
    }

    @Override
    public boolean flushCacheWithPattern(String pattern) {
        return cacheUtil.flushKeysByPattern(pattern);
    }
}



