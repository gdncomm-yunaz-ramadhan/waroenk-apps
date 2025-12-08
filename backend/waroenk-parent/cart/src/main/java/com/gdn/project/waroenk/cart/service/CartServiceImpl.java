package com.gdn.project.waroenk.cart.service;

import com.gdn.project.waroenk.cart.FilterCartRequest;
import com.gdn.project.waroenk.cart.MultipleCartResponse;
import com.gdn.project.waroenk.cart.client.CatalogGrpcClient;
import com.gdn.project.waroenk.cart.dto.cart.AddCartItemResult;
import com.gdn.project.waroenk.cart.dto.cart.BulkAddCartItemsResult;
import com.gdn.project.waroenk.cart.entity.Cart;
import com.gdn.project.waroenk.cart.entity.CartItem;
import com.gdn.project.waroenk.cart.exceptions.AuthorizationException;
import com.gdn.project.waroenk.cart.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.cart.mapper.CartMapper;
import com.gdn.project.waroenk.cart.repository.CartRepository;
import com.gdn.project.waroenk.cart.repository.MongoPageAble;
import com.gdn.project.waroenk.cart.repository.model.ResultData;
import com.gdn.project.waroenk.cart.utility.CacheUtil;
import com.gdn.project.waroenk.cart.utility.ParserUtil;
import com.google.protobuf.Struct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CartServiceImpl extends MongoPageAble<Cart, String> implements CartService {

  private static final CartMapper mapper = CartMapper.INSTANCE;
  private static final String CART_PREFIX = "cart";
  private final CartRepository repository;
  private final CacheUtil<Cart> cacheUtil;
  private final CatalogGrpcClient catalogClient;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public CartServiceImpl(CartRepository repository,
      CacheUtil<Cart> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate,
      CatalogGrpcClient catalogClient) {
    super(CART_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Cart.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
    this.catalogClient = catalogClient;
  }

  @Override
  public Cart getCart(String userId) {
    String key = CART_PREFIX + ":user:" + userId;
    Cart cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Cart cart = repository.findByUserId(userId).orElseGet(() -> {
      // Create empty cart if not exists
      Cart newCart = Cart.builder().userId(userId).items(new ArrayList<>()).currency("IDR").build();
      return repository.save(newCart);
    });

    cacheUtil.putValue(key, cart, 1, TimeUnit.HOURS);
    return cart;
  }

  @Override
  public AddCartItemResult addItemWithValidation(String userId, String sku, String subSku, int quantity) {
    log.info("Adding item to cart with stock validation: userId={}, sku={}, subSku={}, quantity={}",
        userId,
        sku,
        subSku,
        quantity);

    Cart cart = getCart(userId);

    // Call catalog service via gRPC to get variant and stock info
    Optional<CatalogGrpcClient.VariantWithStock> variantOpt = catalogClient.getVariantWithStock(subSku);

    if (variantOpt.isEmpty()) {
      log.warn("Product not found for subSku: {}", subSku);
      return AddCartItemResult.productNotFound(cart, subSku);
    }

    CatalogGrpcClient.VariantWithStock variantWithStock = variantOpt.get();
    int availableStock = (int) variantWithStock.availableStock();

    // Check if sufficient stock
    if (availableStock <= 0) {
      log.warn("Item out of stock: subSku={}", subSku);
      return AddCartItemResult.outOfStock(cart);
    }

    // Check if requested quantity is available
    int existingQty = getExistingQuantityInCart(cart, sku);
    int totalRequested = existingQty + quantity;

    if (availableStock < totalRequested) {
      log.warn("Insufficient stock: subSku={}, requested={}, available={}", subSku, totalRequested, availableStock);
      return AddCartItemResult.insufficientStock(cart, availableStock);
    }

    // Create cart item with snapshot data
    CartItem item = createCartItemFromVariant(variantWithStock, quantity);
    cart.addOrUpdateItem(item);

    Cart saved = repository.save(cart);
    updateCache(saved);

    log.info("Item added to cart successfully: userId={}, sku={}, availableStock={}", userId, sku, availableStock);
    return AddCartItemResult.success(saved, availableStock);
  }

  @Override
  public BulkAddCartItemsResult bulkAddItemsWithValidation(String userId, List<CartItemInput> items) {
    log.info("Bulk adding items to cart with stock validation: userId={}, itemCount={}", userId, items.size());

    Cart cart = getCart(userId);
    List<BulkAddCartItemsResult.CartItemStatus> statuses = new ArrayList<>();

    for (CartItemInput input : items) {
      // Call catalog service via gRPC for each item
      Optional<CatalogGrpcClient.VariantWithStock> variantOpt = catalogClient.getVariantWithStock(input.subSku());

      if (variantOpt.isEmpty()) {
        statuses.add(BulkAddCartItemsResult.CartItemStatus.productNotFound(input.sku(), input.subSku()));
        continue;
      }

      CatalogGrpcClient.VariantWithStock variantWithStock = variantOpt.get();
      int availableStock = (int) variantWithStock.availableStock();

      // Check stock
      int existingQty = getExistingQuantityInCart(cart, input.sku());
      int totalRequested = existingQty + input.quantity();

      if (availableStock < totalRequested) {
        statuses.add(BulkAddCartItemsResult.CartItemStatus.insufficientStock(input.sku(),
            input.subSku(),
            availableStock,
            totalRequested));
        continue;
      }

      // Add to cart
      CartItem item = createCartItemFromVariant(variantWithStock, input.quantity());
      cart.addOrUpdateItem(item);

      statuses.add(BulkAddCartItemsResult.CartItemStatus.success(input.sku(), input.subSku(), availableStock));
    }

    Cart saved = repository.save(cart);
    updateCache(saved);

    return BulkAddCartItemsResult.success(saved, statuses);
  }

  @Override
  public AddCartItemResult updateItemQuantityWithValidation(String userId, String sku, String subSku, int quantity) {
    log.info("Updating cart item quantity with validation: userId={}, sku={}, subSku={}, quantity={}",
        userId,
        sku,
        subSku,
        quantity);

    Cart cart = getCart(userId);

    // If quantity is 0 or negative, remove the item
    if (quantity <= 0) {
      cart.removeItem(sku);
      Cart saved = repository.save(cart);
      updateCache(saved);
      return AddCartItemResult.success(saved, 0);
    }

    // Call catalog service via gRPC to check stock
    Optional<CatalogGrpcClient.VariantWithStock> variantOpt = catalogClient.getVariantWithStock(subSku);

    if (variantOpt.isEmpty()) {
      return AddCartItemResult.productNotFound(cart, subSku);
    }

    CatalogGrpcClient.VariantWithStock variantWithStock = variantOpt.get();
    int availableStock = (int) variantWithStock.availableStock();

    if (availableStock < quantity) {
      return AddCartItemResult.insufficientStock(cart, availableStock);
    }

    // Update item with fresh snapshot
    CartItem item = createCartItemFromVariant(variantWithStock, quantity);

    // First remove, then add to ensure we replace with fresh data
    cart.removeItem(sku);
    cart.addOrUpdateItem(item);

    Cart saved = repository.save(cart);
    updateCache(saved);

    return AddCartItemResult.success(saved, availableStock);
  }

  /**
   * Create CartItem from variant data with snapshot
   */
  private CartItem createCartItemFromVariant(CatalogGrpcClient.VariantWithStock variantWithStock, int quantity) {
    return CartItem.builder()
        .sku(variantWithStock.getSku())
        .subSku(variantWithStock.getSubSku())
        .title(variantWithStock.getTitle())
        .priceSnapshot((long) variantWithStock.getPrice()) // Price in IDR (no conversion needed)
        .quantity(quantity)
        .availableStockSnapshot((int) variantWithStock.availableStock())
        .imageUrl(variantWithStock.getThumbnail())
        .attributes(convertStructToMap(variantWithStock.variant().getAttributes()))
        .build();
  }

  /**
   * Convert protobuf Struct to Map
   */
  private Map<String, String> convertStructToMap(Struct struct) {
    Map<String, String> result = new HashMap<>();
    if (struct != null) {
      struct.getFieldsMap().forEach((key, value) -> {
        if (value.hasStringValue()) {
          result.put(key, value.getStringValue());
        } else if (value.hasNumberValue()) {
          result.put(key, String.valueOf(value.getNumberValue()));
        } else if (value.hasBoolValue()) {
          result.put(key, String.valueOf(value.getBoolValue()));
        }
      });
    }
    return result;
  }

  /**
   * Get existing quantity in cart for a SKU
   */
  private int getExistingQuantityInCart(Cart cart, String sku) {
    if (cart.getItems() == null) {
      return 0;
    }
    return cart.getItems()
        .stream()
        .filter(item -> sku.equals(item.getSku()))
        .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
        .sum();
  }

  @Override
  @Deprecated
  public Cart addItem(String userId, CartItem item) {
    Cart cart = getCart(userId);
    cart.addOrUpdateItem(item);
    Cart saved = repository.save(cart);
    updateCache(saved);
    return saved;
  }

  @Override
  @Deprecated
  public Cart bulkAddItems(String userId, List<CartItem> items) {
    Cart cart = getCart(userId);
    for (CartItem item : items) {
      cart.addOrUpdateItem(item);
    }
    Cart saved = repository.save(cart);
    updateCache(saved);
    return saved;
  }

  @Override
  public Cart removeItem(String userId, String sku) {
    Cart cart = getCart(userId);
    boolean removed = cart.removeItem(sku);
    if (!removed) {
      throw new ResourceNotFoundException("Item with SKU " + sku + " not found in cart");
    }
    Cart saved = repository.save(cart);
    updateCache(saved);
    return saved;
  }

  @Override
  public Cart bulkRemoveItems(String userId, List<String> skus) {
    Cart cart = getCart(userId);
    for (String sku : skus) {
      cart.removeItem(sku);
    }
    Cart saved = repository.save(cart);
    updateCache(saved);
    return saved;
  }

  @Override
  @Deprecated
  public Cart updateItemQuantity(String userId, String sku, Integer quantity) {
    Cart cart = getCart(userId);
    boolean updated = cart.updateItemQuantity(sku, quantity);
    if (!updated) {
      throw new ResourceNotFoundException("Item with SKU " + sku + " not found in cart");
    }
    Cart saved = repository.save(cart);
    updateCache(saved);
    return saved;
  }

  @Override
  public boolean clearCart(String userId) {
    Cart cart = getCart(userId);
    cart.clearItems();
    repository.save(cart);
    invalidateCache(userId);
    return true;
  }

  @Override
  public MultipleCartResponse filterCarts(FilterCartRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    // Security: user_id is REQUIRED - users can only see their own carts
    if (StringUtils.isBlank(request.getUserId())) {
      throw new AuthorizationException("User ID is required to filter carts");
    }

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      // Always filter by user_id (security requirement)
      criteriaList.add(Criteria.where("userId").is(request.getUserId()));
      return criteriaList;
    };

    SortInfo sort = ObjectUtils.isEmpty(request.getSortBy()) ?
        SortInfo.defaultSort() :
        SortInfo.of(StringUtils.isBlank(request.getSortBy().getField()) ? "id" : request.getSortBy().getField(),
            StringUtils.isBlank(request.getSortBy().getDirection()) ? "asc" : request.getSortBy().getDirection());
    ResultData<Cart> entries = query(criteriaBuilder, size, request.getCursor(), sort);
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Cart> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleCartResponse.Builder builder = MultipleCartResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  private void updateCache(Cart cart) {
    String key = CART_PREFIX + ":user:" + cart.getUserId();
    cacheUtil.putValue(key, cart, 1, TimeUnit.HOURS);
  }

  private void invalidateCache(String userId) {
    String key = CART_PREFIX + ":user:" + userId;
    cacheUtil.removeValue(key);
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(Cart input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}