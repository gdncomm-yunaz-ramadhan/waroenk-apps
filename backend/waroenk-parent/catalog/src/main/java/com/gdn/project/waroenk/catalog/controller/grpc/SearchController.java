package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.AggregatedProductData;
import com.gdn.project.waroenk.catalog.BrandDetail;
import com.gdn.project.waroenk.catalog.CategoryDetail;
import com.gdn.project.waroenk.catalog.CheckInventoryRequest;
import com.gdn.project.waroenk.catalog.CheckInventoryResponse;
import com.gdn.project.waroenk.catalog.CombinedSearchRequest;
import com.gdn.project.waroenk.catalog.CombinedSearchResponse;
import com.gdn.project.waroenk.catalog.FacetCount;
import com.gdn.project.waroenk.catalog.FacetCountValue;
import com.gdn.project.waroenk.catalog.FacetStats;
import com.gdn.project.waroenk.catalog.GetProductDetailsRequest;
import com.gdn.project.waroenk.catalog.GetProductDetailsResponse;
import com.gdn.project.waroenk.catalog.GetProductSummaryRequest;
import com.gdn.project.waroenk.catalog.GetProductSummaryResponse;
import com.gdn.project.waroenk.catalog.InventoryCheckItem;
import com.gdn.project.waroenk.catalog.MerchantDetail;
import com.gdn.project.waroenk.catalog.PriceInfo;
import com.gdn.project.waroenk.catalog.ProductDetailData;
import com.gdn.project.waroenk.catalog.ProductMedia;
import com.gdn.project.waroenk.catalog.ProductVariantDetail;
import com.gdn.project.waroenk.catalog.SearchContactInfo;
import com.gdn.project.waroenk.catalog.SearchMerchantData;
import com.gdn.project.waroenk.catalog.SearchMerchantsRequest;
import com.gdn.project.waroenk.catalog.SearchMerchantsResponse;
import com.gdn.project.waroenk.catalog.SearchProductsRequest;
import com.gdn.project.waroenk.catalog.SearchProductsResponse;
import com.gdn.project.waroenk.catalog.SearchServiceGrpc;
import com.gdn.project.waroenk.catalog.StockInfo;
import com.gdn.project.waroenk.catalog.dto.inventory.InventoryCheckItemDto;
import com.gdn.project.waroenk.catalog.dto.merchant.MerchantResponseDto;
import com.gdn.project.waroenk.catalog.dto.product.AggregatedProductDto;
import com.gdn.project.waroenk.catalog.dto.product.ProductDetailDto;
import com.gdn.project.waroenk.catalog.service.SearchService;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.ObjectUtils;
import org.typesense.model.FacetCounts;

import java.time.Instant;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class SearchController extends SearchServiceGrpc.SearchServiceImplBase {

  private final SearchService searchService;

  @Override
  public void search(CombinedSearchRequest request, StreamObserver<CombinedSearchResponse> responseObserver) {
    searchService.search(request.getQuery(),
        request.getSize(),
        request.getCursor(),
        request.getSortBy(),
        request.getSortOrder()).thenAccept(result -> {
      CombinedSearchResponse response = CombinedSearchResponse.newBuilder()
          .setProducts(mapToProductsResponse(result.products()))
          .setMerchants(mapToMerchantsResponse(result.merchants()))
          .setTotalReturned(result.totalReturned())
          .setTotalMatch(result.totalMatch())
          .setTook(result.took())
          .setNextToken(result.nextToken() != null ? result.nextToken() : "")
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }).exceptionally(ex -> {
      log.error("Error in combined search for query: {}", request.getQuery(), ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Search failed: " + ex.getMessage())
          .asRuntimeException());
      return null;
    });
  }

  @Override
  public void searchProducts(SearchProductsRequest request, StreamObserver<SearchProductsResponse> responseObserver) {
    try {
      // Only pass buyable if explicitly set in the request (optional field)
      Boolean buyable = request.hasBuyable() ? request.getBuyable() : null;

      // Build queries map from request fields
      java.util.Map<String, String> queries = new java.util.HashMap<>();
      if (request.hasQuery() && !request.getQuery().isEmpty()) {
        queries.put("q", request.getQuery());
      }
      if (request.hasCategory() && !request.getCategory().isEmpty()) {
        queries.put("category", request.getCategory());
      }
      if (request.hasBrand() && !request.getBrand().isEmpty()) {
        queries.put("brand", request.getBrand());
      }
      if (request.hasMerchant() && !request.getMerchant().isEmpty()) {
        queries.put("merchantCode", request.getMerchant());
      }
      if (request.hasLocation() && !request.getLocation().isEmpty()) {
        queries.put("merchantLocation", request.getLocation());
      }

      SearchService.Result<AggregatedProductDto> result = searchService.searchProducts(queries,
          request.getSize(),
          request.getCursor(),
          request.getSortBy(),
          request.getSortOrder(),
          buyable);

      SearchProductsResponse response = mapToProductsResponse(result);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error searching products for query: {}", request.getQuery(), e);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Product search failed: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void searchMerchants(SearchMerchantsRequest request,
      StreamObserver<SearchMerchantsResponse> responseObserver) {
    try {

      SearchService.Result<MerchantResponseDto> result = searchService.searchMerchants(request.getQuery(),
          request.getSize(),
          request.getCursor(),
          request.getSortBy(),
          request.getSortOrder());

      SearchMerchantsResponse response = mapToMerchantsResponse(result);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error searching merchants for query: {}", request.getQuery(), e);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Merchant search failed: " + e.getMessage())
          .asRuntimeException());
    }
  }

  // Mapping methods for responses

  private SearchProductsResponse mapToProductsResponse(SearchService.Result<AggregatedProductDto> result) {
    SearchProductsResponse.Builder builder = SearchProductsResponse.newBuilder()
        .setTotalReturned(result.totalReturned())
        .setTotalMatch(result.totalMatch())
        .setTotalPage(result.totalPage())
        .setTook(result.took());

    if (result.nextToken() != null) {
      builder.setNextToken(result.nextToken());
    }

    if (result.contents() != null) {
      builder.addAllContents(result.contents().stream().map(this::mapToAggregatedProductData).toList());
    }

    if (result.facetCounts() != null) {
      builder.addAllFacetCounts(result.facetCounts().stream().map(this::mapToFacetCount).toList());
    }

    return builder.build();
  }

  private SearchMerchantsResponse mapToMerchantsResponse(SearchService.Result<MerchantResponseDto> result) {
    SearchMerchantsResponse.Builder builder = SearchMerchantsResponse.newBuilder()
        .setTotalReturned(result.totalReturned())
        .setTotalMatch(result.totalMatch())
        .setTotalPage(result.totalPage())
        .setTook(result.took());

    if (result.nextToken() != null) {
      builder.setNextToken(result.nextToken());
    }

    if (result.contents() != null) {
      builder.addAllContents(result.contents().stream().map(this::mapToSearchMerchantData).toList());
    }

    if (result.facetCounts() != null) {
      builder.addAllFacetCounts(result.facetCounts().stream().map(this::mapToFacetCount).toList());
    }

    return builder.build();
  }

  // Mapping methods for individual items

  private AggregatedProductData mapToAggregatedProductData(AggregatedProductDto dto) {
    AggregatedProductData.Builder builder = AggregatedProductData.newBuilder().setInStock(dto.inStock());

    if (dto.id() != null)
      builder.setId(dto.id());
    if (dto.merchantName() != null)
      builder.setMerchantName(dto.merchantName());
    if (dto.merchantCode() != null)
      builder.setMerchantCode(dto.merchantCode());
    if (dto.merchantLocation() != null)
      builder.setMerchantLocation(dto.merchantLocation());
    if (dto.title() != null)
      builder.setTitle(dto.title());
    if (dto.summary() != null)
      builder.setSummary(dto.summary());
    if (dto.brand() != null)
      builder.setBrand(dto.brand());
    if (dto.category() != null)
      builder.setCategory(dto.category());
    if (dto.categoryCode() != null)
      builder.setCategoryCode(dto.categoryCode());
    if (dto.categoryNames() != null)
      builder.addAllCategoryNames(dto.categoryNames());
    if (dto.categoryCodes() != null)
      builder.addAllCategoryCodes(dto.categoryCodes());
    if (dto.thumbnail() != null)
      builder.setThumbnail(dto.thumbnail());
    if (dto.slug() != null)
      builder.setSlug(dto.slug());
    if (dto.sku() != null)
      builder.setSku(dto.sku());
    if (dto.subSku() != null)
      builder.setSubSku(dto.subSku());
    if (dto.price() != null)
      builder.setPrice(dto.price());
    if (dto.variantKeywords() != null)
      builder.addAllVariantKeywords(dto.variantKeywords());
    if (dto.attributes() != null)
      builder.setAttributes(mapToStruct(dto.attributes()));
    if (dto.createdAt() != null)
      builder.setCreatedAt(toTimestamp(dto.createdAt()));
    if (dto.updatedAt() != null)
      builder.setUpdatedAt(toTimestamp(dto.updatedAt()));

    return builder.build();
  }

  private SearchMerchantData mapToSearchMerchantData(MerchantResponseDto dto) {
    SearchMerchantData.Builder builder = SearchMerchantData.newBuilder();

    if (dto.id() != null)
      builder.setId(dto.id());
    if (dto.name() != null)
      builder.setName(dto.name());
    if (dto.code() != null)
      builder.setCode(dto.code());
    if (dto.iconUrl() != null)
      builder.setIconUrl(dto.iconUrl());
    if (dto.location() != null)
      builder.setLocation(dto.location());
    if (dto.rating() != null)
      builder.setRating(dto.rating());

    if (dto.contact() != null) {
      SearchContactInfo.Builder contactBuilder = SearchContactInfo.newBuilder();
      if (dto.contact().phone() != null)
        contactBuilder.setPhone(dto.contact().phone());
      if (dto.contact().email() != null)
        contactBuilder.setEmail(dto.contact().email());
      builder.setContact(contactBuilder.build());
    }

    return builder.build();
  }

  private FacetCount mapToFacetCount(FacetCounts facetCounts) {
    FacetCount.Builder builder = FacetCount.newBuilder();

    if (facetCounts.getFieldName() != null) {
      builder.setFieldName(facetCounts.getFieldName());
    }

    if (facetCounts.getCounts() != null) {
      builder.addAllCounts(facetCounts.getCounts().stream().map(count -> {
        FacetCountValue.Builder valueBuilder = FacetCountValue.newBuilder();
        if (count.getValue() != null)
          valueBuilder.setValue(count.getValue());
        if (count.getCount() != null)
          valueBuilder.setCount(count.getCount());
        if (count.getHighlighted() != null)
          valueBuilder.setHighlighted(Boolean.parseBoolean(count.getHighlighted()));
        return valueBuilder.build();
      }).toList());
    }

    if (facetCounts.getStats() != null) {
      FacetStats.Builder statsBuilder = FacetStats.newBuilder();
      var stats = facetCounts.getStats();
      if (stats.getMin() != null)
        statsBuilder.setMin(stats.getMin());
      if (stats.getMax() != null)
        statsBuilder.setMax(stats.getMax());
      if (stats.getSum() != null)
        statsBuilder.setSum(stats.getSum());
      if (stats.getAvg() != null)
        statsBuilder.setAvg(stats.getAvg());
      if (stats.getTotalValues() != null)
        statsBuilder.setTotalValues(stats.getTotalValues());
      builder.setStats(statsBuilder.build());
    }

    return builder.build();
  }

  // ============================================================
  // New Product Details, Summary, and Inventory Check Methods
  // ============================================================

  @Override
  public void getProductDetails(GetProductDetailsRequest request,
      StreamObserver<GetProductDetailsResponse> responseObserver) {
    try {
      SearchService.ProductDetailsResult result = searchService.getProductDetails(request.getId());

      GetProductDetailsResponse.Builder responseBuilder = GetProductDetailsResponse.newBuilder().setTook(result.took());

      if (result.product() != null) {
        responseBuilder.setProduct(mapToProductDetailData(result.product()));
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error getting product details for id: {}", request.getId(), e);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(
          "Failed to get product details: " + e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void getProductSummary(GetProductSummaryRequest request,
      StreamObserver<GetProductSummaryResponse> responseObserver) {
    try {
      SearchService.ProductSummaryResult result = searchService.getProductSummary(request.getSubSkusList());

      GetProductSummaryResponse.Builder responseBuilder = GetProductSummaryResponse.newBuilder()
          .setTotalFound(result.totalFound())
          .setTotalRequested(result.totalRequested())
          .setTook(result.took());

      if (result.products() != null) {
        responseBuilder.addAllProducts(result.products().stream().map(this::mapToAggregatedProductData).toList());
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error getting product summary", e);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(
          "Failed to get product summary: " + e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void checkInventory(CheckInventoryRequest request, StreamObserver<CheckInventoryResponse> responseObserver) {
    try {
      SearchService.InventoryCheckResult result = searchService.checkInventory(request.getSubSkusList());

      CheckInventoryResponse.Builder responseBuilder = CheckInventoryResponse.newBuilder()
          .setTotalFound(result.totalFound())
          .setTotalRequested(result.totalRequested())
          .setTook(result.took());

      if (result.items() != null) {
        responseBuilder.addAllItems(result.items().stream().map(this::mapToInventoryCheckItem).toList());
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error checking inventory", e);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Failed to check inventory: " + e.getMessage())
          .asRuntimeException());
    }
  }

  // ============================================================
  // Mapping Methods for New Endpoints
  // ============================================================

  private ProductDetailData mapToProductDetailData(ProductDetailDto dto) {
    StockInfo.Builder stockInfoBuilder = StockInfo.newBuilder();
    if (ObjectUtils.isNotEmpty(dto.stock())) {
      stockInfoBuilder.setHasStock(dto.hasStock()).setTotalStock(dto.stock().totalStock());
    }
    ProductDetailData.Builder builder = ProductDetailData.newBuilder().setStock(stockInfoBuilder.build());

    if (dto.id() != null)
      builder.setId(dto.id());
    if (dto.sku() != null)
      builder.setSku(dto.sku());
    if (dto.title() != null)
      builder.setTitle(dto.title());
    if (dto.shortDescription() != null)
      builder.setShortDescription(dto.shortDescription());
    if (dto.tags() != null)
      builder.addAllTags(dto.tags());
    if (dto.detailRef() != null)
      builder.setDetailRef(dto.detailRef());

    if (dto.merchant() != null) {
      builder.setMerchant(mapToMerchantDetail(dto.merchant()));
    }
    if (dto.brand() != null) {
      builder.setBrand(mapToBrandDetail(dto.brand()));
    }
    if (dto.category() != null) {
      builder.setCategory(mapToCategoryDetail(dto.category()));
    }
    if (dto.variants() != null) {
      builder.addAllVariants(dto.variants().stream().map(this::mapToProductVariantDetail).toList());
    }
    
    // Map medias from selected variant
    if (dto.medias() != null && !dto.medias().isEmpty()) {
      builder.addAllMedias(dto.medias().stream().map(this::mapToProductMedia).toList());
    }
    
    // Map price from selected variant
    if (dto.price() != null) {
      builder.setPrice(mapToPriceInfo(dto.price()));
    }

    return builder.build();
  }
  
  private ProductMedia mapToProductMedia(ProductDetailDto.VariantDetailDto.VariantMediaDto dto) {
    ProductMedia.Builder builder = ProductMedia.newBuilder();
    if (dto.url() != null)
      builder.setUrl(dto.url());
    if (dto.type() != null)
      builder.setType(dto.type());
    if (dto.sortOrder() != null)
      builder.setSortOrder(dto.sortOrder());
    if (dto.altText() != null)
      builder.setAltText(dto.altText());
    return builder.build();
  }
  
  private PriceInfo mapToPriceInfo(ProductDetailDto.PriceDto dto) {
    PriceInfo.Builder builder = PriceInfo.newBuilder();
    if (dto.price() != null)
      builder.setPrice(dto.price());
    if (dto.discount() != null)
      builder.setDiscount(dto.discount());
    return builder.build();
  }

  private MerchantDetail mapToMerchantDetail(ProductDetailDto.MerchantDetailDto dto) {
    MerchantDetail.Builder builder = MerchantDetail.newBuilder();
    if (dto.id() != null)
      builder.setId(dto.id());
    if (dto.name() != null)
      builder.setName(dto.name());
    if (dto.code() != null)
      builder.setCode(dto.code());
    if (dto.iconUrl() != null)
      builder.setIconUrl(dto.iconUrl());
    if (dto.location() != null)
      builder.setLocation(dto.location());
    if (dto.rating() != null)
      builder.setRating(dto.rating());
    return builder.build();
  }

  private BrandDetail mapToBrandDetail(ProductDetailDto.BrandDetailDto dto) {
    BrandDetail.Builder builder = BrandDetail.newBuilder();
    if (dto.id() != null)
      builder.setId(dto.id());
    if (dto.name() != null)
      builder.setName(dto.name());
    if (dto.slug() != null)
      builder.setSlug(dto.slug());
    if (dto.iconUrl() != null)
      builder.setIconUrl(dto.iconUrl());
    return builder.build();
  }

  private CategoryDetail mapToCategoryDetail(ProductDetailDto.CategoryDetailDto dto) {
    CategoryDetail.Builder builder = CategoryDetail.newBuilder();
    if (dto.id() != null)
      builder.setId(dto.id());
    if (dto.name() != null)
      builder.setName(dto.name());
    if (dto.slug() != null)
      builder.setSlug(dto.slug());
    if (dto.iconUrl() != null)
      builder.setIconUrl(dto.iconUrl());
    if (dto.parentId() != null)
      builder.setParentId(dto.parentId());
    return builder.build();
  }

  private ProductVariantDetail mapToProductVariantDetail(ProductDetailDto.VariantDetailDto dto) {
    ProductVariantDetail.Builder builder = ProductVariantDetail.newBuilder();
    if (dto.id() != null)
      builder.setId(dto.id());
    if (dto.subSku() != null)
      builder.setSubSku(dto.subSku());
    if (dto.thumbnail() != null)
      builder.setThumbnail(dto.thumbnail());
    if (dto.isDefault() != null)
      builder.setIsSelected(dto.isDefault());
    if (dto.stockInfo() != null)
      builder.setHasStock(dto.stockInfo().hasStock());

    return builder.build();
  }

  private InventoryCheckItem mapToInventoryCheckItem(InventoryCheckItemDto dto) {
    InventoryCheckItem.Builder builder = InventoryCheckItem.newBuilder()
        .setSubSku(dto.subSku())
        .setStock(dto.stock() != null ? dto.stock() : 0)
        .setHasStock(dto.hasStock());
    if (dto.updatedAt() != null)
      builder.setUpdatedAt(toTimestamp(dto.updatedAt()));
    return builder.build();
  }

  private Timestamp toTimestamp(Instant instant) {
    return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
  }

  private Struct mapToStruct(Map<String, Object> map) {
    Struct.Builder structBuilder = Struct.newBuilder();
    if (map != null) {
      map.forEach((key, value) -> {
        if (value != null) {
          structBuilder.putFields(key, toValue(value));
        }
      });
    }
    return structBuilder.build();
  }

  private Value toValue(Object obj) {
    if (obj == null) {
      return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
    } else if (obj instanceof String) {
      return Value.newBuilder().setStringValue((String) obj).build();
    } else if (obj instanceof Number) {
      return Value.newBuilder().setNumberValue(((Number) obj).doubleValue()).build();
    } else if (obj instanceof Boolean) {
      return Value.newBuilder().setBoolValue((Boolean) obj).build();
    } else {
      return Value.newBuilder().setStringValue(obj.toString()).build();
    }
  }
}
