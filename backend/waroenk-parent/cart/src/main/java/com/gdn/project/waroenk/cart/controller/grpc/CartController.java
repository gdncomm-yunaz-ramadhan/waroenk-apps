package com.gdn.project.waroenk.cart.controller.grpc;

import com.gdn.project.waroenk.cart.AddCartItemRequest;
import com.gdn.project.waroenk.cart.AddCartItemResponse;
import com.gdn.project.waroenk.cart.BulkAddCartItemsRequest;
import com.gdn.project.waroenk.cart.BulkAddCartItemsResponse;
import com.gdn.project.waroenk.cart.BulkRemoveCartItemsRequest;
import com.gdn.project.waroenk.cart.CartData;
import com.gdn.project.waroenk.cart.CartItemStatus;
import com.gdn.project.waroenk.cart.CartServiceGrpc;
import com.gdn.project.waroenk.cart.ClearCartRequest;
import com.gdn.project.waroenk.cart.FilterCartRequest;
import com.gdn.project.waroenk.cart.GetCartRequest;
import com.gdn.project.waroenk.cart.MultipleCartResponse;
import com.gdn.project.waroenk.cart.RemoveCartItemRequest;
import com.gdn.project.waroenk.cart.UpdateCartItemRequest;
import com.gdn.project.waroenk.cart.dto.cart.AddCartItemResult;
import com.gdn.project.waroenk.cart.dto.cart.BulkAddCartItemsResult;
import com.gdn.project.waroenk.cart.entity.Cart;
import com.gdn.project.waroenk.cart.mapper.CartMapper;
import com.gdn.project.waroenk.cart.service.CartService;
import com.gdn.project.waroenk.cart.service.GrpcValidationService;
import com.gdn.project.waroenk.common.Basic;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class CartController extends CartServiceGrpc.CartServiceImplBase {

  private static final CartMapper mapper = CartMapper.INSTANCE;
  private final CartService cartService;
  private final GrpcValidationService validationService;

  @Override
  public void getCart(GetCartRequest request, StreamObserver<CartData> responseObserver) {
    validationService.validateUserId(request.getUserId());

    Cart cart = cartService.getCart(request.getUserId());
    CartData response = mapper.toResponseGrpc(cart);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void addItem(AddCartItemRequest request, StreamObserver<AddCartItemResponse> responseObserver) {
    validationService.validateAddCartItemWithSubSku(request.getUserId(),
        request.getSku(),
        request.getSubSku(),
        request.getQuantity());

    // Use new method with stock validation
    AddCartItemResult result = cartService.addItemWithValidation(request.getUserId(),
        request.getSku(),
        request.getSubSku(),
        request.getQuantity());

    AddCartItemResponse.Builder builder = AddCartItemResponse.newBuilder()
        .setSuccess(result.success())
        .setMessage(result.message())
        .setAvailableStock(result.availableStock());
    if (result.cart() != null) {
      builder.setCart(mapper.toResponseGrpc(result.cart()));
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void bulkAddItems(BulkAddCartItemsRequest request, StreamObserver<BulkAddCartItemsResponse> responseObserver) {
    validationService.validateBulkAddCartItems(request.getUserId(), request.getItemsList());

    // Convert to service input format
    List<CartService.CartItemInput> items = request.getItemsList()
        .stream()
        .map(item -> new CartService.CartItemInput(item.getSku(), item.getSubSku(), item.getQuantity()))
        .collect(Collectors.toList());

    BulkAddCartItemsResult result = cartService.bulkAddItemsWithValidation(request.getUserId(), items);

    BulkAddCartItemsResponse.Builder builder = BulkAddCartItemsResponse.newBuilder().setAllSuccess(result.allSuccess());
    if (result.cart() != null) {
      builder.setCart(mapper.toResponseGrpc(result.cart()));
    }

    // Add item statuses
    result.itemStatuses()
        .forEach(status -> builder.addItemStatuses(CartItemStatus.newBuilder()
            .setSku(status.sku())
            .setSubSku(status.subSku())
            .setSuccess(status.success())
            .setMessage(status.message())
            .setAvailableStock(status.availableStock())
            .build()));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void removeItem(RemoveCartItemRequest request, StreamObserver<CartData> responseObserver) {
    validationService.validateRemoveCartItem(request.getUserId(), request.getSku());

    Cart cart = cartService.removeItem(request.getUserId(), request.getSku());
    CartData response = mapper.toResponseGrpc(cart);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void bulkRemoveItems(BulkRemoveCartItemsRequest request, StreamObserver<CartData> responseObserver) {
    validationService.validateBulkRemoveCartItems(request.getUserId(), request.getSkusList());

    Cart cart = cartService.bulkRemoveItems(request.getUserId(), request.getSkusList());
    CartData response = mapper.toResponseGrpc(cart);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateItem(UpdateCartItemRequest request, StreamObserver<AddCartItemResponse> responseObserver) {
    validationService.validateUpdateCartItem(request.getUserId(), request.getSku(), request.getQuantity());

    // Use new method with stock validation - need subSku from request
    // For now, we need to get subSku from the cart item itself
    Cart cart = cartService.getCart(request.getUserId());
    String subSku = cart.getItems()
        .stream()
        .filter(item -> request.getSku().equals(item.getSku()))
        .findFirst()
        .map(item -> item.getSubSku())
        .orElse(request.getSku()); // fallback to sku if not found

    AddCartItemResult result = cartService.updateItemQuantityWithValidation(request.getUserId(),
        request.getSku(),
        subSku,
        request.getQuantity());

    AddCartItemResponse.Builder builder = AddCartItemResponse.newBuilder()
        .setSuccess(result.success())
        .setMessage(result.message())
        .setAvailableStock(result.availableStock());
    if (result.cart() != null) {
      builder.setCart(mapper.toResponseGrpc(result.cart()));
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void clearCart(ClearCartRequest request, StreamObserver<Basic> responseObserver) {
    validationService.validateClearCart(request.getUserId());

    boolean result = cartService.clearCart(request.getUserId());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterCarts(FilterCartRequest request, StreamObserver<MultipleCartResponse> responseObserver) {
    MultipleCartResponse response = cartService.filterCarts(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}