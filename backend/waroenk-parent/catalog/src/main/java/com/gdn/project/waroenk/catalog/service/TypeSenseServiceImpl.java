package com.gdn.project.waroenk.catalog.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.model.MultiSearchResult;
import org.typesense.model.MultiSearchSearchesParameter;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TypeSenseServiceImpl implements TypeSenseService {
  private static final String SEMANTIC = "semantic:";
  private final Client client;

  @Override
  public <T> void index(T entity, SearchIndexMapper<T> mapper) throws Exception {
    Map<String, Object> doc = new HashMap<>();
    doc.put("id", mapper.id(entity));
    doc.put("type", mapper.type());
    doc.put("title", mapper.title(entity));
    doc.put("body", mapper.body(entity));
    doc.putAll(mapper.extraFields(entity)); // optional metadata

    client.collections(mapper.type()).documents().upsert(doc);
  }

  @Override
  public <T> void delete(String id, SearchIndexMapper<T> mapper) throws Exception {
    client.collections(mapper.type()).documents(id).delete();
  }

  @Override
  public <T> SearchResult search(String query, SearchIndexMapper<T> mapper) throws Exception {
    SearchParameters params = new SearchParameters().q(query).queryBy(getQueryBy(mapper));
    return client.collections(mapper.type()).documents().search(params);
  }

  private <T> String getQueryBy(SearchIndexMapper<T> mapper) {
    Set<String> fields = new LinkedHashSet<>(mapper.queryAbleFields()); // Add mapper fields first (they have the correct order)
    fields.add("title");  // Fallback fields
    fields.add("body");
    return String.join(",", fields);
  }

  @Override
  public <T> SearchResult search(SearchParameters parameters, SearchIndexMapper<T> mapper) throws Exception {
    // Ensure queryBy is set if not already specified
    if (parameters.getQueryBy() == null || parameters.getQueryBy().isEmpty()) {
      parameters.queryBy(getQueryBy(mapper));
    }
    return client.collections(mapper.type()).documents().search(parameters);
  }

  @Override
  public MultiSearchResult search(MultiSearchSearchesParameter multiSearchParameters,
      Map<String, String> commonParameters) throws Exception {
    return client.multiSearch.perform(multiSearchParameters, commonParameters);
  }

  @Override
  public <T> SearchResult search(String query, SearchIndexMapper<T> mapper, int page, int size) throws Exception {
    size = Math.max(0, size);
    page = Math.max(1, page);
    SearchParameters params = new SearchParameters().q(query).queryBy(getQueryBy(mapper)).page(page).perPage(size);
    return client.collections(mapper.type()).documents().search(params);
  }

  @Override
  public <T> SearchResult search(String query,
      SearchIndexMapper<T> mapper,
      int page,
      int size,
      String sortBy,
      String sortOrder) throws Exception {
    size = Math.max(0, size);
    page = Math.max(1, page);
    SearchParameters params = new SearchParameters().q(query).queryBy(getQueryBy(mapper)).page(page).perPage(size);
    if (StringUtils.isNotBlank(sortBy)) {
      sortOrder = StringUtils.isNotBlank(sortOrder) ? sortOrder : "asc";
      params.sortBy(String.format("%s:%s", sortBy.trim(), sortOrder.toLowerCase().trim()));
    }
    return client.collections(mapper.type()).documents().search(params);
  }
}
