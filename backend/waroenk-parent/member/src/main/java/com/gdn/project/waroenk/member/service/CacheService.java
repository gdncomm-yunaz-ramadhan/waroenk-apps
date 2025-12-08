package com.gdn.project.waroenk.member.service;

public interface CacheService {
  boolean flushAll();

  boolean flushCacheWithPattern(String pattern);
}
