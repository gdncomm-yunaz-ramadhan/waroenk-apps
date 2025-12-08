package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.CategoryNode;
import com.gdn.project.waroenk.catalog.FindVariantsBySkuRequest;
import com.gdn.project.waroenk.catalog.VariantData;
import com.gdn.project.waroenk.catalog.dto.inventory.InventoryCheckItemDto;
import com.gdn.project.waroenk.catalog.dto.merchant.MerchantResponseDto;
import com.gdn.project.waroenk.catalog.dto.product.AggregatedProductDto;
import com.gdn.project.waroenk.catalog.dto.product.ProductDetailDto;
import com.gdn.project.waroenk.catalog.dto.product.ProductDetailDto.BrandDetailDto;
import com.gdn.project.waroenk.catalog.dto.product.ProductDetailDto.CategoryDetailDto;
import com.gdn.project.waroenk.catalog.dto.product.ProductDetailDto.MerchantDetailDto;
import com.gdn.project.waroenk.catalog.dto.product.ProductDetailDto.VariantDetailDto;
import com.gdn.project.waroenk.catalog.entity.Brand;
import com.gdn.project.waroenk.catalog.entity.Category;
import com.gdn.project.waroenk.catalog.entity.Inventory;
import com.gdn.project.waroenk.catalog.entity.Merchant;
import com.gdn.project.waroenk.catalog.entity.Product;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.VariantMapper;
import com.gdn.project.waroenk.catalog.utility.ParserUtil;
import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.typesense.model.FacetCounts;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;
import org.typesense.model.SearchResultHit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
  private final static VariantMapper variantMapper = VariantMapper.INSTANCE;
  private final TypeSenseService typeSenseService;
  private final ProductSearchIndexMapper productMapper;
  private final MerchantSearchIndexMapper merchantMapper;
  private final ProductService productService;
  private final VariantService variantService;
  private final MerchantService merchantService;
  private final BrandService brandService;
  private final CategoryService categoryService;
  private final InventoryService inventoryService;
  private final Executor typesenseExecutor;
  @Value("${default.item-per-page}")
  private Integer defaultItemPerPage;

  public SearchServiceImpl(TypeSenseService typeSenseService,
      ProductSearchIndexMapper productMapper,
      MerchantSearchIndexMapper merchantMapper,
      ProductService productService,
      VariantService variantService,
      MerchantService merchantService,
      BrandService brandService,
      CategoryService categoryService,
      InventoryService inventoryService,
      @Qualifier("typesenseExecutor") Executor typesenseExecutor) {
    this.typeSenseService = typeSenseService;
    this.productMapper = productMapper;
    this.merchantMapper = merchantMapper;
    this.productService = productService;
    this.variantService = variantService;
    this.merchantService = merchantService;
    this.brandService = brandService;
    this.categoryService = categoryService;
    this.inventoryService = inventoryService;
    this.typesenseExecutor = typesenseExecutor;
  }

  private PageResult toPageResult(SearchResult result, int page, int perPage) {
    List<SearchResultHit> hits = result.getHits();
    Integer totalDocuments = result.getFound();
    int totalPages = (int) Math.ceil((double) totalDocuments / perPage);

    String nextPageCursor = null;
    // Calculate if there is a next page
    if (result.getPage() < totalPages) {
      // Encode the next page number as Base64
      int nextPage = page + 1;
      nextPageCursor = encodeCursor(nextPage, perPage, null);
    }

    return new PageResult(hits,
        result.getFacetCounts(),
        hits.size(),
        totalDocuments,
        totalPages,
        result.getSearchTimeMs(),
        nextPageCursor);
  }

  private String encodeCursor(int page, int size, Map<String, String> additionalCursor) {
    StringBuilder builder = new StringBuilder();
    builder.append("p=").append(page);
    builder.append("&");
    builder.append("s=").append(size);
    if (additionalCursor != null) {
      additionalCursor.forEach((key, value) -> {
        builder.append("&");
        builder.append(key).append("=").append(value);
      });
    }

    return ParserUtil.encodeBase64(builder.toString());
  }


  Map<String, String> decodeCursor(String cursor) {
    String decoded = ParserUtil.decodeBase64(cursor);
    String[] parts = Objects.requireNonNull(decoded).split("&");

    Map<String, String> result = new HashMap<>();
    for (String part : parts) {
      String[] entry = part.split("=", 2);
      result.put(entry[0], entry.length > 1 ? entry[1] : "");
    }

    return result;
  }

  private Query resolveQueries(Map<String, String> queries) {
    if (ObjectUtils.isEmpty(queries)) {
      return new Query("*", null);
    }
    // Parse query text
    String query = queries.getOrDefault("q", queries.getOrDefault("query", "*"));
    query = StringUtils.isNotBlank(query) ? query.trim().toLowerCase() : "*";
    List<String> filterBys = new ArrayList<>();

    if (query.contains(":")) {
      String queryByAttributes = resolveQueryByAttributes(query);
      if (StringUtils.isNotBlank(queryByAttributes)) {
        filterBys.add(queryByAttributes);
        query = "*";
      }
    }

    Set<String> queryAbleFields = productMapper.queryAbleFields();

    queries.forEach((key, value) -> {
      String match = resolveMatchKey(key, queryAbleFields);
      if (StringUtils.isNotBlank(match) && StringUtils.isNotBlank(value)) {
        if (key.toLowerCase().contains("category")) {
          filterBys.add(String.format("(categoryCodes:=[\\\"%s\\\"] || categoryNames:=[\\\"%s\\\"])", value, value));
        } else {
          filterBys.add(String.format("%s:=[\\\"%s\\\"]", match, value));
        }
      }
    });

    return new Query(query, String.join(" && ", filterBys));
  }

  private String resolveQueryByAttributes(String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    String[] parsed = input.trim().toLowerCase().replaceAll("\\s+", " ").replaceAll("\\s+:\\s+", ":").split("\\s+");
    List<String> captured = new ArrayList<>();
    boolean capture = false;
    StringBuilder builder = new StringBuilder();
    for (String section : parsed) {
      if (section.contains(":")) {
        if (capture) {
          captured.add(builder.toString());
          builder.setLength(0);
          builder.append(section);
          capture = false;
        } else {
          builder.append(section);
          capture = true;
        }
      } else {
        builder.append(section);
        builder.append(" ");
      }
    }

    if (!builder.isEmpty()) {
      captured.add(builder.toString());
      builder.setLength(0);
    }

    if (captured.isEmpty()) {
      return null;
    }
    return String.format("attributes:=[%s]",
        captured.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")));
  }


  private String resolveMatchKey(String key, Set<String> inputs) {
    String match = null;
    if (ObjectUtils.isEmpty(inputs)) {
      return null;
    }
    for (String target : inputs) {
      if (StringUtils.isNotBlank(target)) {
        if (key.trim().equalsIgnoreCase(target.trim())) {
          match = target;
          break;
        }
      }
    }
    return match;
  }

  @Override
  public Result<AggregatedProductDto> searchProducts(Map<String, String> queries,
      int size,
      String cursor,
      String sortBy,
      String sortOrder,
      Boolean buyable) throws Exception {
    int page = 1;
    if (StringUtils.isNotBlank(cursor)) {
      Map<String, String> params = decodeCursor(cursor);
      page = Integer.parseInt(params.getOrDefault("p", "1"));
    }
    if (size == 0) {
      size = this.defaultItemPerPage;
    }
    Query query = resolveQueries(queries);
    SearchParameters parameters = new SearchParameters();
    parameters.q(query.query()).page(page).perPage(size);

    if (StringUtils.isNotBlank(query.filterBy())) {
      parameters.filterBy(query.filterBy());
    }

    // Buyable filter
    if (buyable != null) {
      String existing = query.filterBy();
      if (StringUtils.isNotBlank(existing)) {
        parameters.filterBy(String.format("%s && inStock:%s", existing, buyable));
      } else {
        parameters.filterBy(String.format("inStock:%s", buyable));
      }
    }

    if (StringUtils.isNotBlank(sortBy)) {
      sortOrder = StringUtils.isNotBlank(sortOrder) ? sortOrder : "asc";
      parameters.sortBy(String.format("%s:%s", sortBy.trim(), sortOrder.toLowerCase().trim()));
    }
    SearchResult result = typeSenseService.search(parameters, productMapper);
    PageResult pageResult = toPageResult(result, result.getPage(), size);
    List<AggregatedProductDto> contents = mapProductsParallel(result);
    return new Result<>(contents,
        pageResult.facetCounts(),
        pageResult.totalReturned(),
        pageResult.totalMatch(),
        pageResult.totalPage(),
        pageResult.took(),
        pageResult.nextToken());
  }

  @Override
  public Result<MerchantResponseDto> searchMerchants(String query,
      int size,
      String cursor,
      String sortBy,
      String sortOrder) throws Exception {
    int page = 1;
    if (StringUtils.isNotBlank(cursor)) {
      Map<String, String> params = decodeCursor(cursor);
      page = Integer.parseInt(params.getOrDefault("p", "1"));
    }
    if (size == 0) {
      size = this.defaultItemPerPage;
    }
    SearchParameters parameters = new SearchParameters();
    parameters.page(page).perPage(size);
    if (StringUtils.isBlank(query)) {
      parameters.q("*");
    } else {
      parameters.q(query.trim().toLowerCase());
    }
    if (StringUtils.isNotBlank(sortBy)) {
      sortOrder = StringUtils.isNotBlank(sortOrder) ? sortOrder : "asc";
      parameters.sortBy(String.format("%s:%s", sortBy.trim(), sortOrder.toLowerCase().trim()));
    }
    SearchResult result = typeSenseService.search(parameters, merchantMapper);
    PageResult pageResult = toPageResult(result, result.getPage(), size);
    List<MerchantResponseDto> contents = mapMerchantsParallel(result);

    return new Result<>(contents,
        pageResult.facetCounts(),
        pageResult.totalReturned(),
        pageResult.totalMatch(),
        pageResult.totalPage(),
        pageResult.took(),
        pageResult.nextToken());
  }

  @Override
  public CompletableFuture<CombinedResult> search(String query,
      int size,
      String cursor,
      String sortBy,
      String sortDirection) {
    long startTime = System.currentTimeMillis();
    int page = 1;
    String productCursor;
    String merchantCursor;
    if (StringUtils.isNotBlank(cursor)) {
      Map<String, String> params = decodeCursor(cursor);
      page = Integer.parseInt(params.getOrDefault("p", "1"));
      productCursor = params.get("pd");
      merchantCursor = params.get("mc");
    } else {
      merchantCursor = null;
      productCursor = null;
    }

    final int finalPage = page;

    CompletableFuture<Result<AggregatedProductDto>> productFuture = CompletableFuture.supplyAsync(() -> {
      try {
        Map<String, String> queries = new HashMap<>();
        if (StringUtils.isNotBlank(query)) {
          queries.put("q", query);
        }
        if (StringUtils.isNotBlank(cursor)) {
          return StringUtils.isBlank(productCursor) ?
              new Result<>(new ArrayList<>(), new ArrayList<>(), 0, 0, 0, 0, null) :
              searchProducts(queries, size, productCursor, sortBy, sortDirection, null);
        } else {
          return searchProducts(queries, size, null, sortBy, sortDirection, null);
        }
      } catch (Exception e) {
        log.warn("Fail to query product : {}", query, e);
        return new Result<>(new ArrayList<>(), new ArrayList<>(), 0, 0, 0, 0, null);
      }
    }, typesenseExecutor);

    CompletableFuture<Result<MerchantResponseDto>> merchantFuture = CompletableFuture.supplyAsync(() -> {
      try {
        if (StringUtils.isNotBlank(cursor)) {
          return StringUtils.isBlank(merchantCursor) ?
              new Result<>(new ArrayList<>(), new ArrayList<>(), 0, 0, 0, 0, null) :
              searchMerchants(query, size, merchantCursor, sortBy, sortDirection);
        } else {
          return searchMerchants(query, size, null, sortBy, sortDirection);
        }
      } catch (Exception e) {
        log.warn("Fail to query product : {}", query, e);
        return new Result<>(new ArrayList<>(), new ArrayList<>(), 0, 0, 0, 0, null);
      }
    }, typesenseExecutor);

    // Combine the results when both futures complete
    return productFuture.thenCombine(merchantFuture, (productResults, merchantResults) -> {

      long endTime = System.currentTimeMillis();
      long elapsedTime = endTime - startTime;
      Map<String, String> params = new HashMap<>();
      if (StringUtils.isNotBlank(productResults.nextToken())) {
        params.put("pd", productResults.nextToken());
      }
      if (StringUtils.isNotBlank(merchantResults.nextToken())) {
        params.put("mc", productResults.nextToken());
      }
      int totalReturned = productResults.totalReturned() + merchantResults.totalReturned();
      int totalMatch = productResults.totalMatch() + merchantResults.totalMatch();
      String nextToken = encodeCursor(finalPage + 1, size, params);
      return new CombinedResult(productResults, merchantResults, totalReturned, totalMatch, elapsedTime, nextToken);
    });
  }

  @Override
  public void indexProduct(AggregatedProductDto product) {
    try {
      typeSenseService.index(product, productMapper);
    } catch (Exception e) {
      log.warn("Fail to index product {} from typesense", product.subSku());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteProductIndex(String productId) {
    try {
      typeSenseService.delete(productId, productMapper);
    } catch (Exception e) {
      log.warn("Fail to delete product {} from typesense", productId);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void indexMerchant(Merchant merchant) {
    try {
      typeSenseService.index(merchant, merchantMapper);
    } catch (Exception e) {
      log.warn("Fail to index merchant {} from typesense", merchant.getCode());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteMerchantIndex(String merchantId) {
    try {
      typeSenseService.delete(merchantId, merchantMapper);
    } catch (Exception e) {
      log.warn("Fail to delete merchant {} from typesense", merchantId);
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<AggregatedProductDto> buildAggregatedProduct(String sku) {
    List<AggregatedProductDto> results = new ArrayList<>();

    Product product = productService.findProductBySku(sku);

    Merchant merchant = getMerchantSafely(product.getMerchantCode());
    Category category = getCategorySafely(product.getCategoryId());
    CategoryNode categoryNode = category != null ? categoryService.getCategoryNodes(category) : null;

    // Get all the variant
    Map<String, VariantData> variants =
        variantService.findVariantsBySku(FindVariantsBySkuRequest.newBuilder().setSku(sku).build())
            .getDataList()
            .stream()
            .collect(Collectors.toMap(VariantData::getSubSku, Function.identity(), (existing, duplicate) -> duplicate));
    Map<String, Inventory> inventories =
        inventoryService.findBulkInventoriesBySubSkus(variants.keySet().stream().toList())
            .stream()
            .collect(Collectors.toMap(Inventory::getSubSku, Function.identity(), (existing, duplicate) -> duplicate));

    final String brandName = getBrandNameSafely(product);

    variants.forEach((key, variant) -> {
      // Build variant keywords and attributes
      Map<String, Object> attributes = new HashMap<>();
      Set<String> variantKeywords = new HashSet<>();

      if (variant.hasAttributes()) {
        variant.getAttributes().getFieldsMap().forEach((attributeKey, attributeValue) -> {
          if (ObjectUtils.isNotEmpty(attributeValue)) {
            attributes.put(attributeKey, resolveObjectValue(attributeValue));
            variantKeywords.add(attributeValue.getStringValue());
          }
        });
      }

      // Check stock
      boolean inStock = inventories.get(key).getStock() > 0L;
      // Create slug from title
      String slug = product.getTitle() != null ?
          product.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") :
          sku.toLowerCase();


      results.add(new AggregatedProductDto(variant.getSubSku(),
          ObjectUtils.isEmpty(merchant) ? null : merchant.getName(),
          ObjectUtils.isEmpty(merchant) ? null : merchant.getCode(),
          ObjectUtils.isEmpty(merchant) ? null : merchant.getLocation(),
          inStock,
          variant.getTitle(),
          product.getSummary() != null ? product.getSummary().getShortDescription() : null,
          brandName,
          ObjectUtils.isEmpty(category) ? null : category.getName(),
          ObjectUtils.isEmpty(category) ? null : category.getId(),
          getCategoryNames(categoryNode),
          getCategoryCodes(categoryNode),
          variant.getThumbnail(),
          slug,
          attributes.isEmpty() ? null : attributes,
          new ArrayList<>(variantKeywords),
          product.getSku(),
          variant.getSubSku(),
          variant.getPrice(),
          toInstant(variant.getCreatedAt()),
          toInstant(variant.getUpdatedAt())));
    });

    return results;
  }

  private Instant toInstant(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  /**
   * Safely get brand name, returning null if brand doesn't exist
   */
  private String getBrandNameSafely(Product product) {
    if (product.getBrandId() == null) {
      return null;
    }
    try {
      return brandService.findBrandById(product.getBrandId()).getName();
    } catch (ResourceNotFoundException e) {
      log.warn("Brand with id {} not found for product {}, setting brandName to null",
          product.getBrandId(), product.getSku());
      return null;
    }
  }

  /**
   * Safely get category, returning null if category doesn't exist or categoryId is invalid
   */
  private Category getCategorySafely(String categoryId) {
    if (categoryId == null || categoryId.isBlank() || "cat-".equals(categoryId)) {
      return null;
    }
    try {
      return categoryService.findCategoryById(categoryId);
    } catch (ResourceNotFoundException e) {
      log.warn("Category with id {} not found, setting category to null", categoryId);
      return null;
    }
  }

  /**
   * Safely get merchant, returning null if merchant doesn't exist
   */
  private Merchant getMerchantSafely(String merchantCode) {
    if (merchantCode == null || merchantCode.isBlank()) {
      return null;
    }
    try {
      return merchantService.findMerchantByCode(merchantCode);
    } catch (ResourceNotFoundException e) {
      log.warn("Merchant with code {} not found, setting merchant to null", merchantCode);
      return null;
    }
  }

  private Object resolveObjectValue(com.google.protobuf.Value value) {
    Object result = null;
    if (ObjectUtils.isNotEmpty(value)) {
      if (value.hasBoolValue()) {
        result = value.getBoolValue();
      } else if (value.hasStringValue()) {
        result = value.getStringValue();
      } else if (value.hasNumberValue()) {
        result = value.getNumberValue();
      } else if (value.hasStructValue()) {
        Map<String, Object> map = new HashMap<>();
        value.getStructValue().getFieldsMap().forEach((k, v) -> {
          if (ObjectUtils.isNotEmpty(v)) {
            map.put(k, resolveObjectValue(v));
          }
        });
        result = map;
      } else if (value.hasListValue()) {
        List<Object> arr = new ArrayList<>();
        value.getListValue().getValuesList().forEach(item -> {
          arr.add(resolveObjectValue(item));
        });
        result = arr;
      }
    }

    return result;
  }

  private List<String> getCategoryCodes(CategoryNode node) {
    List<String> result = new ArrayList<>();
    if (ObjectUtils.isEmpty(node)) {
      return result;
    }

    result.add(node.getId());
    node.getChildrenList().forEach(child -> {
      result.addAll(getCategoryCodes(child));
    });

    return result;
  }

  private List<String> getCategoryNames(CategoryNode node) {
    List<String> result = new ArrayList<>();
    if (ObjectUtils.isEmpty(node)) {
      return result;
    }

    result.add(node.getName());
    node.getChildrenList().forEach(child -> {
      result.addAll(getCategoryNames(child));
    });

    return result;
  }

  /**
   * Maps search results to AggregatedProductDto using parallel CompletableFuture for performance.
   * Avoids reflection-based ObjectMapper by using direct field extraction.
   */
  private List<AggregatedProductDto> mapProductsParallel(SearchResult result) {
    if (result.getHits() == null || result.getHits().isEmpty()) {
      return List.of();
    }

    List<CompletableFuture<AggregatedProductDto>> futures = result.getHits()
        .stream()
        .map(hit -> CompletableFuture.supplyAsync(() -> mapToProduct(hit.getDocument()), typesenseExecutor))
        .toList();

    return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
  }

  /**
   * Maps search results to MerchantResponseDto using parallel CompletableFuture for performance.
   * Avoids reflection-based ObjectMapper by using direct field extraction.
   */
  private List<MerchantResponseDto> mapMerchantsParallel(SearchResult result) {
    if (result.getHits() == null || result.getHits().isEmpty()) {
      return List.of();
    }

    List<CompletableFuture<MerchantResponseDto>> futures = result.getHits()
        .stream()
        .map(hit -> CompletableFuture.supplyAsync(() -> mapToMerchant(hit.getDocument()), typesenseExecutor))
        .toList();

    return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
  }

  /**
   * Manual mapping from Typesense document to AggregatedProductDto.
   * Direct field extraction is significantly faster than reflection-based ObjectMapper.
   */
  @SuppressWarnings("unchecked")
  private AggregatedProductDto mapToProduct(Map<String, Object> doc) {
    if (doc == null)
      return null;

    return new AggregatedProductDto(getString(doc, "id"),
        getString(doc, "merchantName"),
        getString(doc, "merchantCode"),
        getString(doc, "merchantLocation"),
        getBoolean(doc, "inStock"),
        getString(doc, "title"),
        getString(doc, "body"),
        getString(doc, "brand"),
        getString(doc, "category"),
        getString(doc, "categoryCode"),
        getStringList(doc, "categoryNames"),
        getStringList(doc, "categoryCodes"),
        getString(doc, "thumbnail"),
        getString(doc, "slug"),
        getMappedAttributes(getStringList(doc, "attributes")),
        getStringList(doc, "variantKeywords"),
        getString(doc, "sku"),
        getString(doc, "subSku"),
        getDouble(doc, "price"),
        getInstant(doc, "createdAt"),
        getInstant(doc, "updatedAt"));
  }

  private Map<String, Object> getMappedAttributes(List<String> attributes) {
    Map<String, Object> result = new HashMap<>();
    if (ObjectUtils.isEmpty(attributes)) {
      return result;
    }
    for (String attribute : attributes) {
      if (attribute.contains(":")) {
        String[] arr = attribute.split(":");
        String params = arr.length < 1 ? "" : String.join(" ", Arrays.asList(arr).subList(1, arr.length));
        result.put(arr[0], params);
      }
    }

    return result;
  }

  /**
   * Manual mapping from Typesense document to MerchantResponseDto.
   * Direct field extraction is significantly faster than reflection-based ObjectMapper.
   */
  private MerchantResponseDto mapToMerchant(Map<String, Object> doc) {
    if (doc == null)
      return null;

    MerchantResponseDto.ContactInfoDto contact = null;
    String phone = getString(doc, "phone");
    String email = getString(doc, "email");
    if (phone != null || email != null) {
      contact = new MerchantResponseDto.ContactInfoDto(phone, email);
    }

    return new MerchantResponseDto(getString(doc, "id"),
        getString(doc, "title"),
        // name is stored as title in Typesense
        getString(doc, "code"),
        getString(doc, "iconUrl"),
        getString(doc, "location"),
        contact,
        getFloat(doc, "rating"),
        getInstant(doc, "createdAt"),
        getInstant(doc, "updatedAt"));
  }

  // Helper methods for safe type extraction from Map
  private String getString(Map<String, Object> doc, String key) {
    Object value = doc.get(key);
    return value != null ? value.toString() : null;
  }

  private boolean getBoolean(Map<String, Object> doc, String key) {
    Object value = doc.get(key);
    if (value instanceof Boolean)
      return (Boolean) value;
    if (value instanceof String)
      return Boolean.parseBoolean((String) value);
    return false;
  }

  private Double getDouble(Map<String, Object> doc, String key) {
    Object value = doc.get(key);
    if (value == null)
      return null;
    if (value instanceof Number)
      return ((Number) value).doubleValue();
    try {
      return Double.parseDouble(value.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Float getFloat(Map<String, Object> doc, String key) {
    Object value = doc.get(key);
    if (value == null)
      return null;
    if (value instanceof Number)
      return ((Number) value).floatValue();
    try {
      return Float.parseFloat(value.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Instant getInstant(Map<String, Object> doc, String key) {
    Object value = doc.get(key);
    if (value == null)
      return null;
    if (value instanceof Number) {
      // Assume it's epoch millis
      return Instant.ofEpochMilli(((Number) value).longValue());
    }
    if (value instanceof String) {
      try {
        return Instant.parse((String) value);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private List<String> getStringList(Map<String, Object> doc, String key) {
    Object value = doc.get(key);
    if (value == null)
      return null;
    if (value instanceof List) {
      return ((List<?>) value).stream().filter(Objects::nonNull).map(Object::toString).toList();
    }
    return null;
  }

  @Override
  public ProductDetailsResult getProductDetails(String id) throws Exception {
    long startTime = System.currentTimeMillis();

    // First, try to find by subSku in Typesense for fast lookup
    SearchParameters params =
        new SearchParameters().q("*").filterBy(String.format("subSku:=%s || sku:=%s", id, id)).perPage(1);

    SearchResult result = typeSenseService.search(params, productMapper);
    AggregatedProductDto aggregated =
        result.getFound() > 0 ? mapToProduct(result.getHits().getFirst().getDocument()) : null;

    String productSku = ObjectUtils.isEmpty(aggregated) ? id : aggregated.sku();

    // Parallel fetch: Product and Variants (variants needed for inventory lookup)
    CompletableFuture<Product> productFuture =
        CompletableFuture.supplyAsync(() -> productService.findProductBySku(productSku), typesenseExecutor);

    CompletableFuture<Map<String, VariantData>> variantsFuture =
        CompletableFuture.supplyAsync(() -> variantService.findVariantsBySku(FindVariantsBySkuRequest.newBuilder()
                    .setSku(productSku)
                    .build())
                .getDataList()
                .stream()
                .collect(Collectors.toMap(VariantData::getSubSku, Function.identity(), (existing, duplicate) -> duplicate)),
            typesenseExecutor);

    // Wait for product and variants
    Product product = productFuture.join();
    if (ObjectUtils.isEmpty(product)) {
      throw new ResourceNotFoundException("Product not found for " + id);
    }
    Map<String, VariantData> variants = variantsFuture.join();

    // Parallel fetch: Merchant, Brand, Category, and Inventories
    CompletableFuture<Merchant> merchantFuture =
        CompletableFuture.supplyAsync(() -> getMerchantSafely(product.getMerchantCode()), typesenseExecutor);

    CompletableFuture<Brand> brandFuture = CompletableFuture.supplyAsync(() -> {
      if (product.getBrandId() != null) {
        try {
          return brandService.findBrandById(product.getBrandId());
        } catch (ResourceNotFoundException e) {
          log.warn("Brand with id {} not found for product {}", product.getBrandId(), product.getSku());
          return null;
        }
      }
      return null;
    }, typesenseExecutor);

    CompletableFuture<Category> categoryFuture =
        CompletableFuture.supplyAsync(() -> getCategorySafely(product.getCategoryId()), typesenseExecutor);

    CompletableFuture<Map<String, Inventory>> inventoriesFuture =
        CompletableFuture.supplyAsync(() -> inventoryService.findBulkInventoriesBySubSkus(variants.keySet()
                    .stream()
                    .toList())
                .stream()
                .collect(Collectors.toMap(Inventory::getSubSku, Function.identity(), (existing, duplicate) -> duplicate)),
            typesenseExecutor);

    // Wait for all parallel fetches to complete
    CompletableFuture.allOf(merchantFuture, brandFuture, categoryFuture, inventoriesFuture).join();

    Merchant merchant = merchantFuture.join();
    Brand brand = brandFuture.join();
    Category category = categoryFuture.join();
    Map<String, Inventory> inventories = inventoriesFuture.join();

    // Build the detailed response
    String subSku = ObjectUtils.isEmpty(aggregated) ? null : aggregated.subSku();
    ProductDetailDto detailDto =
        buildProductDetailDto(subSku, product, merchant, brand, category, variants, inventories);

    long took = System.currentTimeMillis() - startTime;
    log.debug("Product details fetched in {}ms for id={}", took, id);
    return new ProductDetailsResult(detailDto, took);
  }

  @Override
  public ProductSummaryResult getProductSummary(List<String> subSkus) throws Exception {
    long startTime = System.currentTimeMillis();

    if (subSkus == null || subSkus.isEmpty()) {
      return new ProductSummaryResult(Collections.emptyList(), 0, 0, 0);
    }

    int totalRequested = subSkus.size();

    // Build filter for exact subSku match
    String filterBy = subSkus.stream().map(sku -> "subSku:=" + sku).collect(Collectors.joining(" || "));

    SearchParameters params =
        new SearchParameters().q("*").filterBy(filterBy).perPage(Math.min(totalRequested, 250)); // Typesense limit

    SearchResult result = typeSenseService.search(params, productMapper);
    List<AggregatedProductDto> products = mapProductsParallel(result);

    long took = System.currentTimeMillis() - startTime;
    return new ProductSummaryResult(products, products.size(), totalRequested, took);
  }

  // ============================================================
  // New Product Details, Summary, and Inventory Check Methods
  // ============================================================

  @Override
  public InventoryCheckResult checkInventory(List<String> subSkus) {
    long startTime = System.currentTimeMillis();

    if (subSkus == null || subSkus.isEmpty()) {
      return new InventoryCheckResult(Collections.emptyList(), 0, 0, 0);
    }

    int totalRequested = subSkus.size();

    // Get inventories with short TTL cache
    Map<String, Inventory> inventoryMap = inventoryService.findBulkInventoriesBySubSkus(subSkus)
        .stream()
        .collect(Collectors.toMap(Inventory::getSubSku, Function.identity(), (existing, duplicate) -> duplicate));

    List<InventoryCheckItemDto> items = subSkus.stream().filter(inventoryMap::containsKey).map(subSku -> {
      Inventory inv = inventoryMap.get(subSku);
      return new InventoryCheckItemDto(subSku,
          inv.getStock(),
          inv.getStock() != null && inv.getStock() > 0,
          inv.getUpdatedAt());
    }).toList();

    long took = System.currentTimeMillis() - startTime;
    return new InventoryCheckResult(items, items.size(), totalRequested, took);
  }

  private ProductDetailDto buildProductDetailDto(String subSku,
      Product product,
      Merchant merchant,
      Brand brand,
      Category category,
      Map<String, VariantData> variants,
      Map<String, Inventory> inventoryMap) {

    // Build merchant detail
    MerchantDetailDto merchantDto = merchant != null ?
        new MerchantDetailDto(merchant.getId(),
            merchant.getName(),
            merchant.getCode(),
            merchant.getIconUrl(),
            merchant.getLocation(),
            merchant.getRating(),
            merchant.getContact() != null ?
                new MerchantDetailDto.ContactInfoDto(merchant.getContact().getPhone(),
                    merchant.getContact().getEmail()) :
                null) :
        null;

    // Build brand detail
    BrandDetailDto brandDto =
        brand != null ? new BrandDetailDto(brand.getId(), brand.getName(), brand.getSlug(), brand.getIconUrl()) : null;

    // Build category detail
    CategoryDetailDto categoryDto = category != null ?
        new CategoryDetailDto(category.getId(),
            category.getName(),
            category.getSlug(),
            category.getIconUrl(),
            category.getParentId()) :
        null;

    // Determine selected variant
    String selectedSubSkuId = subSku;
    if (StringUtils.isBlank(selectedSubSkuId)) {
      // Find default variant or use first one
      selectedSubSkuId = variants.values()
          .stream()
          .filter(VariantData::getIsDefault)
          .map(VariantData::getSubSku)
          .findFirst()
          .orElse(variants.keySet().stream().findFirst().orElse(null));
    }
    final String finalSelectedSubSku = selectedSubSkuId;

    // Build variant details with stock info
    List<VariantDetailDto> variantDtos = new ArrayList<>();
    List<VariantDetailDto.VariantMediaDto> selectedVariantMedias = new ArrayList<>();
    Double selectedVariantPrice = null;

    for (Map.Entry<String, VariantData> entry : variants.entrySet()) {
      String id = entry.getKey();
      VariantData variant = entry.getValue();

      Inventory inventory = inventoryMap.get(id);
      VariantDetailDto.StockInfoDto variantStockInfo = inventory != null ?
          new VariantDetailDto.StockInfoDto(inventory.getStock() != null ? inventory.getStock() : 0,
              inventory.getStock() != null && inventory.getStock() > 0,
              inventory.getUpdatedAt()) :
          new VariantDetailDto.StockInfoDto(0, false, null);
      
      List<VariantDetailDto.VariantMediaDto> medias = new ArrayList<>();
      variant.getMediaList()
          .forEach(media -> medias.add(new VariantDetailDto.VariantMediaDto(media.getUrl(),
              media.getType(),
              media.getSortOrder(),
              media.getAltText())));

      // Track selected variant's media and price
      boolean isSelected = id.equals(finalSelectedSubSku);
      if (isSelected) {
        selectedVariantMedias.addAll(medias);
        selectedVariantPrice = variant.getPrice();
      }

      variantDtos.add(new VariantDetailDto(variant.getId(),
          variant.getSubSku(),
          variant.getTitle(),
          variant.getPrice(),
          variant.getIsDefault() || isSelected,
          // Mark as selected
          variantMapper.structToMap(variant.getAttributes()),
          variant.getThumbnail(),
          medias,
          variantStockInfo,
          variantMapper.toInstant(variant.getCreatedAt()),
          variantMapper.toInstant(variant.getUpdatedAt())));
    }

    // Calculate total stock
    long totalStock =
        inventoryMap.values().stream().mapToLong(inv -> inv.getStock() != null ? inv.getStock() : 0).sum();
    boolean hasStock = totalStock > 0;

    Inventory selectedInventory = inventoryMap.get(finalSelectedSubSku);
    VariantDetailDto.StockInfoDto stockInfo = new VariantDetailDto.StockInfoDto(totalStock,
        hasStock,
        selectedInventory != null ? selectedInventory.getUpdatedAt() : null);

    // Build price DTO
    ProductDetailDto.PriceDto priceDto =
        new ProductDetailDto.PriceDto(selectedVariantPrice, null,  // discount - can be added later if needed
            "IDR");

    // Build product summary
    String shortDescription = product.getSummary() != null ? product.getSummary().getShortDescription() : null;
    List<String> tags = product.getSummary() != null ? product.getSummary().getTags() : null;

    return new ProductDetailDto(product.getId(),
        product.getSku(),
        product.getTitle(),
        shortDescription,
        tags,
        product.getDetailRef(),
        merchantDto,
        brandDto,
        categoryDto,
        variantDtos,
        selectedVariantMedias,
        // Product-level media from selected variant
        stockInfo,
        priceDto,
        // Price from selected variant
        hasStock,
        product.getCreatedAt(),
        product.getUpdatedAt());
  }


  record Query(String query, String filterBy) {

  }

  // ============================================================
  // Bulk Indexing Methods
  // ============================================================

  @Override
  public int indexAllMerchants() {
    log.info("Starting TypeSense indexing for all merchants...");
    List<Merchant> merchants = merchantService.findAllMerchants();
    int indexed = 0;
    int failed = 0;

    for (Merchant merchant : merchants) {
      try {
        indexMerchant(merchant);
        indexed++;
        if (indexed % 1000 == 0) {
          log.info("Indexed {} merchants...", indexed);
        }
      } catch (Exception e) {
        log.warn("Failed to index merchant {}: {}", merchant.getCode(), e.getMessage());
        failed++;
      }
    }

    log.info("TypeSense merchant indexing complete: {} indexed, {} failed", indexed, failed);
    return indexed;
  }

  @Override
  public int indexAllProducts() {
    log.info("Starting TypeSense indexing for all products...");
    List<Product> products = productService.findAllProducts();
    int indexed = 0;
    int failed = 0;

    for (Product product : products) {
      try {
        List<AggregatedProductDto> aggregatedProducts = buildAggregatedProduct(product.getSku());
        if (aggregatedProducts != null) {
          for (AggregatedProductDto dto : aggregatedProducts) {
            try {
              indexProduct(dto);
              indexed++;
            } catch (Exception e) {
              log.warn("Failed to index variant {}: {}", dto.subSku(), e.getMessage());
              failed++;
            }
          }
        }
        if ((indexed + failed) % 1000 == 0) {
          log.info("Progress: {} indexed, {} failed", indexed, failed);
        }
      } catch (Exception e) {
        log.error("Failed to build aggregated product for {}: {}", product.getSku(), e.getMessage());
        failed++;
      }
    }

    log.info("TypeSense product indexing complete: {} indexed, {} failed", indexed, failed);
    return indexed;
  }

  @Override
  public BulkIndexResult indexProductsBySkus(List<String> skus) {
    log.info("Starting TypeSense indexing for {} SKUs...", skus.size());
    int indexed = 0;
    int failed = 0;
    List<String> failedSkus = new ArrayList<>();

    for (String sku : skus) {
      try {
        List<AggregatedProductDto> aggregatedProducts = buildAggregatedProduct(sku);
        if (aggregatedProducts != null && !aggregatedProducts.isEmpty()) {
          boolean allVariantsIndexed = true;
          for (AggregatedProductDto dto : aggregatedProducts) {
            try {
              indexProduct(dto);
            } catch (Exception e) {
              log.warn("Failed to index variant {}: {}", dto.subSku(), e.getMessage());
              allVariantsIndexed = false;
            }
          }
          if (allVariantsIndexed) {
            indexed++;
          } else {
            failed++;
            failedSkus.add(sku);
          }
        } else {
          failed++;
          failedSkus.add(sku);
        }
        
        if ((indexed + failed) % 500 == 0) {
          log.info("Bulk index progress: {} indexed, {} failed out of {}", indexed, failed, skus.size());
        }
      } catch (Exception e) {
        log.error("Failed to build aggregated product for {}: {}", sku, e.getMessage());
        failed++;
        failedSkus.add(sku);
      }
    }

    log.info("Bulk indexing complete: {} indexed, {} failed out of {} requested", indexed, failed, skus.size());
    return new BulkIndexResult(skus.size(), indexed, failed, failedSkus);
  }

  // ============================================================
  // DTO Building Helper Methods
  // ============================================================


  record PageResult(Iterable<SearchResultHit> hits, List<FacetCounts> facetCounts, int totalReturned, int totalMatch,
                    int totalPage, int took, String nextToken) {

  }
}


