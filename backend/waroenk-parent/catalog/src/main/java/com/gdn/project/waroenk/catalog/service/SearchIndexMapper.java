package com.gdn.project.waroenk.catalog.service;

import java.util.Map;
import java.util.Set;

public interface SearchIndexMapper<T> {
  String id(T entity);
  String type();
  String title(T entity);
  String body(T entity);
  Map<String, Object> extraFields(T entity);
  Set<String> queryAbleFields();
}
