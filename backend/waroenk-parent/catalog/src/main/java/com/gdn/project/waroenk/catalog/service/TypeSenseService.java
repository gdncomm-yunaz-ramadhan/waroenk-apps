package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.properties.TypeSenseProperties;
import org.typesense.model.MultiSearchResult;
import org.typesense.model.MultiSearchSearchesParameter;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;

import java.util.Map;

public interface TypeSenseService {
  <T> void index(T entity, SearchIndexMapper<T> mapper) throws Exception;
  <T> void delete(String id, SearchIndexMapper<T> mapper) throws Exception;
  <T> SearchResult search(String query, SearchIndexMapper<T> mapper) throws Exception;
  <T> SearchResult search(SearchParameters parameters, SearchIndexMapper<T> mapper) throws Exception;
  MultiSearchResult search(MultiSearchSearchesParameter multiSearchParameters, Map<String, String> commonParameters) throws Exception;
  <T> SearchResult search(String query, SearchIndexMapper<T> mapper, int page, int size) throws Exception;
  <T> SearchResult search(String query, SearchIndexMapper<T> mapper, int page, int size, String sortBy, String sortOrder)
      throws Exception;

  default String getCollection(TypeSenseProperties properties){
    return properties.getClient().getCollectionName();
  }
}
