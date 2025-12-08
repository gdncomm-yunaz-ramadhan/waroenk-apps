package com.gdn.project.waroenk.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.project.waroenk.cart.FilterCheckoutRequest;
import com.gdn.project.waroenk.cart.MultipleCheckoutResponse;
import com.gdn.project.waroenk.cart.client.CatalogGrpcClient;
import com.gdn.project.waroenk.cart.client.MemberGrpcClient;
import com.gdn.project.waroenk.cart.dto.checkout.FinalizeCheckoutResult;
import com.gdn.project.waroenk.cart.dto.checkout.PayCheckoutResult;
import com.gdn.project.waroenk.cart.dto.checkout.PrepareCheckoutResult;
import com.gdn.project.waroenk.cart.entity.AddressSnapshot;
import com.gdn.project.waroenk.cart.entity.Cart;
import com.gdn.project.waroenk.cart.entity.CartItem;
import com.gdn.project.waroenk.cart.entity.Checkout;
import com.gdn.project.waroenk.cart.entity.CheckoutItem;
import com.gdn.project.waroenk.cart.exceptions.AuthorizationException;
import com.gdn.project.waroenk.cart.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.cart.exceptions.ValidationException;
import com.gdn.project.waroenk.cart.mapper.CheckoutMapper;
import com.gdn.project.waroenk.cart.repository.CheckoutRepository;
import com.gdn.project.waroenk.cart.repository.MongoPageAble;
import com.gdn.project.waroenk.cart.repository.model.ResultData;
import com.gdn.project.waroenk.cart.utility.CacheUtil;
import com.gdn.project.waroenk.cart.utility.ParserUtil;
import com.gdn.project.waroenk.catalog.BulkAcquireStockResponse;
import com.gdn.project.waroenk.catalog.BulkLockStockResponse;
import com.gdn.project.waroenk.catalog.BulkReleaseStockResponse;
import com.gdn.project.waroenk.catalog.StockOperationItem;
import com.gdn.project.waroenk.catalog.StockOperationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CheckoutServiceImpl extends MongoPageAble<Checkout, String> implements CheckoutService {

  private static final CheckoutMapper mapper = CheckoutMapper.INSTANCE;
  private static final String CHECKOUT_PREFIX = "checkout";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final CheckoutRepository repository;
  private final CartService cartService;
  private final SystemParameterService systemParameterService;
  private final CatalogGrpcClient catalogClient;
  private final MemberGrpcClient memberClient;
  private final CacheUtil<String> cacheUtil;
  private final ObjectMapper objectMapper;

  @Value("${cart.checkout.ttl-seconds:900}")
  private Integer defaultCheckoutTtl;

  @Value("${cart.checkout.use-redis:true}")
  private Boolean useRedis;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public CheckoutServiceImpl(CheckoutRepository repository,
      CartService cartService,
      SystemParameterService systemParameterService,
      CatalogGrpcClient catalogClient,
      MemberGrpcClient memberClient,
      CacheUtil<String> cacheUtil,
      ObjectMapper objectMapper,
      MongoTemplate mongoTemplate) {
    super(CHECKOUT_PREFIX, cacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Checkout.class);
    this.repository = repository;
    this.cartService = cartService;
    this.systemParameterService = systemParameterService;
    this.catalogClient = catalogClient;
    this.memberClient = memberClient;
    this.cacheUtil = cacheUtil;
    this.objectMapper = objectMapper;
  }

  // ============================================================
  // Main Checkout Flow Implementation
  // ============================================================

  @Override
  public PrepareCheckoutResult prepareCheckout(String userId) {
    log.info("Preparing checkout for user: {}", userId);

    // Get cart
    Cart cart = cartService.getCart(userId);

    if (cart.getItems() == null || cart.getItems().isEmpty()) {
      return PrepareCheckoutResult.emptyCart();
    }

    // Check for existing active checkout
    Optional<Checkout> existingOpt = repository.findByUserIdAndStatus(userId, "WAITING");
    if (existingOpt.isPresent()) {
      Checkout existing = existingOpt.get();
      if (!existing.isExpired()) {
        log.info("Existing active checkout found for user: {}", userId);
        return PrepareCheckoutResult.existingCheckout(existing);
      }
      // Existing checkout is expired, cancel it (internal call, ownership already verified)
      cancelCheckoutInternal(existing.getCheckoutId(), "Auto-cancelled: preparing new checkout");
    }

    // Get TTL from system parameters
    int ttl = getCheckoutTtl();

    // Prepare bulk lock request
    List<StockOperationItem> lockItems = cart.getItems()
        .stream()
        .filter(item -> item.getSubSku() != null)
        .map(item -> StockOperationItem.newBuilder()
            .setSubSku(item.getSubSku())
            .setQuantity(item.getQuantity())
            .build())
        .collect(Collectors.toList());

    // Generate checkout ID
    String checkoutId = "chk-" + UUID.randomUUID().toString().substring(0, 8);

    // Call catalog service to bulk lock inventory
    BulkLockStockResponse lockResponse = catalogClient.bulkLockStock(checkoutId, lockItems, ttl);

    // Process lock results
    List<PrepareCheckoutResult.SkuLockSummary> lockSummary = new ArrayList<>();
    List<CheckoutItem> checkoutItems = new ArrayList<>();

    Map<String, StockOperationResult> resultMap =
        lockResponse.getResultsList().stream().collect(Collectors.toMap(StockOperationResult::getSubSku, r -> r));

    for (CartItem cartItem : cart.getItems()) {
      StockOperationResult result = resultMap.get(cartItem.getSubSku());

      CheckoutItem checkoutItem = CheckoutItem.builder()
          .sku(cartItem.getSku())
          .subSku(cartItem.getSubSku())
          .title(cartItem.getTitle())
          .priceSnapshot(cartItem.getPriceSnapshot())
          .quantity(cartItem.getQuantity())
          .availableStockSnapshot(cartItem.getAvailableStockSnapshot())
          .imageUrl(cartItem.getImageUrl())
          .attributes(cartItem.getAttributes())
          .build();

      if (result != null && result.getSuccess()) {
        checkoutItem.setReserved(true);
        lockSummary.add(PrepareCheckoutResult.SkuLockSummary.success(cartItem.getSku(),
            cartItem.getSubSku(),
            cartItem.getQuantity(),
            (int) result.getCurrentStock()));
      } else {
        checkoutItem.setReserved(false);
        String errorMsg = result != null ? result.getMessage() : "Lock failed";
        checkoutItem.setReservationError(errorMsg);

        // Update cart item quantity to 0 if lock failed
        updateCartItemOnLockFailure(userId, cartItem.getSku());

        lockSummary.add(PrepareCheckoutResult.SkuLockSummary.failed(cartItem.getSku(),
            cartItem.getSubSku(),
            cartItem.getQuantity(),
            result != null ? (int) result.getCurrentStock() : 0,
            errorMsg));
      }

      checkoutItems.add(checkoutItem);
    }

    // Check if at least one item was locked
    boolean hasLockedItems = checkoutItems.stream().anyMatch(item -> Boolean.TRUE.equals(item.getReserved()));

    if (!hasLockedItems) {
      log.warn("No items could be locked for checkout: userId={}", userId);
      return PrepareCheckoutResult.noItemsLocked(lockSummary);
    }

    // Create checkout
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(ttl);

    Checkout checkout = Checkout.builder()
        .checkoutId(checkoutId)
        .userId(userId)
        .sourceCartId(cart.getId())
        .items(checkoutItems)
        .totalPrice(calculateTotalPrice(checkoutItems))
        .currency(cart.getCurrency())
        .status("WAITING")
        .lockedAt(now)
        .expiresAt(expiresAt)
        .build();

    // Save to MongoDB
    Checkout saved = repository.save(checkout);

    // Save to Redis if enabled
    if (Boolean.TRUE.equals(useRedis)) {
      saveToRedis(saved, ttl);
    }

    // Remove successfully locked items from cart (move to checkout)
    List<String> lockedSkus = checkoutItems.stream()
        .filter(item -> Boolean.TRUE.equals(item.getReserved()))
        .map(CheckoutItem::getSku)
        .collect(Collectors.toList());
    
    if (!lockedSkus.isEmpty()) {
      try {
        cartService.bulkRemoveItems(userId, lockedSkus);
        log.info("Removed {} items from cart after checkout preparation: userId={}", lockedSkus.size(), userId);
      } catch (Exception e) {
        log.warn("Failed to remove items from cart after checkout: userId={}, error={}", userId, e.getMessage());
      }
    }

    log.info("Checkout prepared: checkoutId={}, lockedItems={}", checkoutId, checkout.getReservedItemCount());

    boolean allLocked = lockResponse.getAllSuccess();
    return allLocked ?
        PrepareCheckoutResult.success(saved, lockSummary) :
        PrepareCheckoutResult.partialSuccess(saved, lockSummary);
  }

  @Override
  public FinalizeCheckoutResult finalizeCheckout(String checkoutId, String userId, String addressId, AddressSnapshot newAddress) {
    log.info("Finalizing checkout: checkoutId={}, userId={}", checkoutId, userId);

    Checkout checkout = getCheckoutInternal(checkoutId);
    
    // Validate ownership
    validateOwnership(checkout, userId);

    // Validate status
    if (!"WAITING".equals(checkout.getStatus())) {
      return FinalizeCheckoutResult.invalidStatus(checkout);
    }

    if (checkout.isExpired()) {
      return FinalizeCheckoutResult.expired(checkout);
    }

    // Check if already finalized (has order ID)
    if (StringUtils.isNotBlank(checkout.getOrderId())) {
      return FinalizeCheckoutResult.alreadyFinalized(checkout);
    }

    // Get or create address
    AddressSnapshot address = null;
    if (StringUtils.isNotBlank(addressId)) {
      // Fetch from member service via gRPC
      Optional<AddressSnapshot> addressOpt = memberClient.getAddressById(addressId);
      if (addressOpt.isEmpty()) {
        return FinalizeCheckoutResult.addressNotFound(addressId);
      }
      address = addressOpt.get();
    } else if (newAddress != null) {
      address = newAddress;
    } else {
      return FinalizeCheckoutResult.addressRequired();
    }

    // Generate order ID and payment code
    String orderId = generateOrderId();
    String paymentCode = generatePaymentCode();

    // Update checkout
    checkout.setShippingAddress(address);
    checkout.setOrderId(orderId);
    checkout.setPaymentCode(paymentCode);
    checkout.setTotalPrice(checkout.calculateTotalPrice());

    Checkout saved = repository.save(checkout);

    // Update Redis if enabled
    if (Boolean.TRUE.equals(useRedis)) {
      int remainingTtl = (int) (checkout.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
      if (remainingTtl > 0) {
        saveToRedis(saved, remainingTtl);
      }
    }

    log.info("Checkout finalized: checkoutId={}, orderId={}, paymentCode={}", checkoutId, orderId, paymentCode);

    return FinalizeCheckoutResult.success(saved, orderId, paymentCode);
  }

  @Override
  public PayCheckoutResult payCheckout(String checkoutId, String userId) {
    log.info("Processing payment for checkout: checkoutId={}, userId={}", checkoutId, userId);

    Checkout checkout = getCheckoutInternal(checkoutId);
    
    // Validate ownership
    validateOwnership(checkout, userId);

    // Validate status
    if (!"WAITING".equals(checkout.getStatus())) {
      if ("PAID".equals(checkout.getStatus())) {
        return PayCheckoutResult.alreadyPaid(checkout);
      }
      return PayCheckoutResult.invalidStatus(checkout);
    }

    if (checkout.isExpired()) {
      return PayCheckoutResult.expired(checkout);
    }

    // Check if finalized (has order ID)
    if (StringUtils.isBlank(checkout.getOrderId())) {
      return PayCheckoutResult.notFinalized(checkout);
    }

    // Prepare bulk acquire request
    List<StockOperationItem> acquireItems = checkout.getItems()
        .stream()
        .filter(item -> Boolean.TRUE.equals(item.getReserved()))
        .map(item -> StockOperationItem.newBuilder()
            .setSubSku(item.getSubSku())
            .setQuantity(item.getQuantity())
            .build())
        .collect(Collectors.toList());

    // Call catalog service to acquire (permanently deduct) inventory
    BulkAcquireStockResponse acquireResponse = catalogClient.bulkAcquireStock(checkoutId, acquireItems);

    if (!acquireResponse.getAllSuccess()) {
      log.error("Failed to acquire inventory for checkout: checkoutId={}", checkoutId);
      return PayCheckoutResult.inventoryAcquireFailed(checkout,
          "Some items could not be acquired. Success: " + acquireResponse.getSuccessCount() + ", Failed: "
              + acquireResponse.getFailureCount());
    }

    // Update checkout status
    checkout.setStatus("PAID");
    checkout.setPaidAt(Instant.now());
    Checkout saved = repository.save(checkout);

    // Remove from Redis
    if (Boolean.TRUE.equals(useRedis)) {
      removeFromRedis(checkoutId);
    }

    // Note: Cart items were already removed during prepareCheckout
    // No need to clear cart here

    log.info("Payment successful: checkoutId={}, orderId={}", checkoutId, checkout.getOrderId());

    return PayCheckoutResult.success(saved);
  }

  @Override
  public boolean cancelCheckout(String checkoutId, String userId, String reason) {
    log.info("Cancelling checkout: checkoutId={}, userId={}, reason={}", checkoutId, userId, reason);

    Checkout checkout = getCheckoutInternal(checkoutId);
    
    // Validate ownership
    validateOwnership(checkout, userId);

    return cancelCheckoutInternal(checkout, reason);
  }

  /**
   * Internal method to cancel checkout without ownership validation.
   * Used for system operations like expiry handling.
   */
  private boolean cancelCheckoutInternal(String checkoutId, String reason) {
    Checkout checkout = getCheckoutInternal(checkoutId);
    return cancelCheckoutInternal(checkout, reason);
  }

  /**
   * Internal method to perform checkout cancellation.
   */
  private boolean cancelCheckoutInternal(Checkout checkout, String reason) {
    if ("PAID".equals(checkout.getStatus())) {
      throw new ValidationException("Cannot cancel a paid checkout");
    }

    if ("CANCELLED".equals(checkout.getStatus())) {
      return true; // Already cancelled
    }

    // Release inventory locks
    releaseInventoryLocks(checkout);

    // Update status
    checkout.setStatus("CANCELLED");
    checkout.setCancelledAt(Instant.now());
    repository.save(checkout);

    // Remove from Redis
    if (Boolean.TRUE.equals(useRedis)) {
      removeFromRedis(checkout.getCheckoutId());
    }

    log.info("Checkout cancelled: checkoutId={}", checkout.getCheckoutId());
    return true;
  }

  // ============================================================
  // Retrieval APIs
  // ============================================================

  @Override
  public Checkout getCheckout(String checkoutId, String userId) {
    Checkout checkout = getCheckoutInternal(checkoutId);
    
    // Validate ownership
    validateOwnership(checkout, userId);
    
    return checkout;
  }

  /**
   * Internal method to get checkout without ownership validation.
   * Used internally when ownership is validated separately.
   */
  private Checkout getCheckoutInternal(String checkoutId) {
    // Try Redis first
    if (Boolean.TRUE.equals(useRedis)) {
      Checkout cached = getFromRedis(checkoutId);
      if (cached != null) {
        return cached;
      }
    }

    // Fallback to MongoDB
    return repository.findByCheckoutId(checkoutId)
        .orElseThrow(() -> new ResourceNotFoundException("Checkout with ID " + checkoutId + " not found"));
  }

  /**
   * Validate that the checkout belongs to the authenticated user.
   *
   * @param checkout The checkout entity
   * @param userId   The authenticated user ID
   * @throws AuthorizationException if the checkout doesn't belong to the user
   */
  private void validateOwnership(Checkout checkout, String userId) {
    if (checkout == null) {
      throw new ResourceNotFoundException("Checkout not found");
    }
    if (StringUtils.isBlank(userId)) {
      throw new AuthorizationException("User ID is required for authorization");
    }
    if (!userId.equals(checkout.getUserId())) {
      log.warn("Unauthorized access attempt: userId={} tried to access checkout owned by {}",
          userId, checkout.getUserId());
      throw new AuthorizationException("You are not authorized to access this checkout");
    }
  }

  @Override
  public Checkout getActiveCheckoutByUser(String userId) {
    return repository.findByUserIdAndStatus(userId, "WAITING")
        .orElseThrow(() -> new ResourceNotFoundException("No active checkout found for user " + userId));
  }

  @Override
  public MultipleCheckoutResponse filterCheckouts(FilterCheckoutRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    // Security: user_id is REQUIRED - users can only see their own checkouts
    if (StringUtils.isBlank(request.getUserId())) {
      throw new AuthorizationException("User ID is required to filter checkouts");
    }

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      // Always filter by user_id (security requirement)
      criteriaList.add(Criteria.where("userId").is(request.getUserId()));
      if (StringUtils.isNotBlank(request.getOrderId())) {
        criteriaList.add(Criteria.where("orderId").regex(request.getOrderId(), "i"));
      }
      if (StringUtils.isNotBlank(request.getStatus())) {
        criteriaList.add(Criteria.where("status").is(request.getStatus()));
      }
      return criteriaList;
    };

    ResultData<Checkout> entries =
        query(criteriaBuilder, size, request.getCursor(), mapper.toSortInfo(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Checkout> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleCheckoutResponse.Builder builder = MultipleCheckoutResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  // ============================================================
  // Helper Methods
  // ============================================================

  private int getCheckoutTtl() {
    try {
      Integer ttl = systemParameterService.getInt("cart.checkout.ttl-seconds", defaultCheckoutTtl);
      return ttl != null ? ttl : defaultCheckoutTtl;
    } catch (Exception e) {
      log.warn("Failed to get checkout TTL from system parameters, using default: {}", defaultCheckoutTtl);
      return defaultCheckoutTtl;
    }
  }

  private String generateOrderId() {
    String prefix = getSystemParam("cart.checkout.order-id-prefix", "ORD");
    String date = LocalDate.now().format(DATE_FORMATTER);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + "-" + date + "-" + random;
  }

  private String generatePaymentCode() {
    String prefix = getSystemParam("cart.checkout.payment-code-prefix", "PAY");
    String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return prefix + "-" + random;
  }

  private String getSystemParam(String key, String defaultValue) {
    try {
      return systemParameterService.getString(key, defaultValue);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private Long calculateTotalPrice(List<CheckoutItem> items) {
    return items.stream()
        .filter(item -> Boolean.TRUE.equals(item.getReserved()))
        .mapToLong(item -> (item.getPriceSnapshot() != null ? item.getPriceSnapshot() : 0L) * (
            item.getQuantity() != null ? item.getQuantity() : 0))
        .sum();
  }

  private void releaseInventoryLocks(Checkout checkout) {
    List<StockOperationItem> releaseItems = checkout.getItems()
        .stream()
        .filter(item -> Boolean.TRUE.equals(item.getReserved()))
        .map(item -> StockOperationItem.newBuilder()
            .setSubSku(item.getSubSku())
            .setQuantity(item.getQuantity())
            .build())
        .collect(Collectors.toList());

    if (!releaseItems.isEmpty()) {
      BulkReleaseStockResponse response = catalogClient.bulkReleaseStock(checkout.getCheckoutId(), releaseItems);
      log.info("Released inventory locks: checkoutId={}, success={}",
          checkout.getCheckoutId(),
          response.getAllSuccess());
    }
  }

  private void updateCartItemOnLockFailure(String userId, String sku) {
    try {
      // Update cart to set quantity to 0 for failed item
      cartService.updateItemQuantity(userId, sku, 0);
    } catch (Exception e) {
      log.warn("Failed to update cart item on lock failure: userId={}, sku={}", userId, sku);
    }
  }

  private void saveToRedis(Checkout checkout, int ttl) {
    try {
      String key = CHECKOUT_PREFIX + ":" + checkout.getCheckoutId();
      String value = objectMapper.writeValueAsString(checkout);
      cacheUtil.putValue(key, value, ttl, TimeUnit.SECONDS);
      log.debug("Saved checkout to Redis: {}", key);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize checkout to Redis: {}", e.getMessage());
    }
  }

  private Checkout getFromRedis(String checkoutId) {
    try {
      String key = CHECKOUT_PREFIX + ":" + checkoutId;
      String value = cacheUtil.getValue(key);
      if (ObjectUtils.isNotEmpty(value)) {
        return objectMapper.readValue(value, Checkout.class);
      }
    } catch (Exception e) {
      log.warn("Failed to deserialize checkout from Redis: {}", e.getMessage());
    }
    return null;
  }

  private void removeFromRedis(String checkoutId) {
    String key = CHECKOUT_PREFIX + ":" + checkoutId;
    cacheUtil.removeValue(key);
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(Checkout input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}
