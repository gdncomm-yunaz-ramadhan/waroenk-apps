package com.gdn.project.waroenk.cart.controller.grpc;

import com.gdn.project.waroenk.cart.CancelCheckoutRequest;
import com.gdn.project.waroenk.cart.CancelCheckoutResponse;
import com.gdn.project.waroenk.cart.CheckoutData;
import com.gdn.project.waroenk.cart.CheckoutServiceGrpc;
import com.gdn.project.waroenk.cart.FilterCheckoutRequest;
import com.gdn.project.waroenk.cart.FinalizeCheckoutRequest;
import com.gdn.project.waroenk.cart.FinalizeCheckoutResponse;
import com.gdn.project.waroenk.cart.GetCheckoutByUserRequest;
import com.gdn.project.waroenk.cart.GetCheckoutRequest;
import com.gdn.project.waroenk.cart.MultipleCheckoutResponse;
import com.gdn.project.waroenk.cart.PayCheckoutRequest;
import com.gdn.project.waroenk.cart.PayCheckoutResponse;
import com.gdn.project.waroenk.cart.PrepareCheckoutRequest;
import com.gdn.project.waroenk.cart.PrepareCheckoutResponse;
import com.gdn.project.waroenk.cart.SkuLockSummary;
import com.gdn.project.waroenk.cart.dto.checkout.FinalizeCheckoutResult;
import com.gdn.project.waroenk.cart.dto.checkout.PayCheckoutResult;
import com.gdn.project.waroenk.cart.dto.checkout.PrepareCheckoutResult;
import com.gdn.project.waroenk.cart.entity.AddressSnapshot;
import com.gdn.project.waroenk.cart.entity.Checkout;
import com.gdn.project.waroenk.cart.mapper.CheckoutMapper;
import com.gdn.project.waroenk.cart.service.CheckoutService;
import com.gdn.project.waroenk.cart.service.GrpcValidationService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CheckoutController extends CheckoutServiceGrpc.CheckoutServiceImplBase {

  private static final CheckoutMapper mapper = CheckoutMapper.INSTANCE;
  private final CheckoutService checkoutService;
  private final GrpcValidationService validationService;

  // ============================================================
  // New Checkout Flow
  // ============================================================

  @Override
  public void prepareCheckout(PrepareCheckoutRequest request,
      StreamObserver<PrepareCheckoutResponse> responseObserver) {
    validationService.validatePrepareCheckout(request.getUserId());

    PrepareCheckoutResult result = checkoutService.prepareCheckout(request.getUserId());

    PrepareCheckoutResponse.Builder builder =
        PrepareCheckoutResponse.newBuilder().setSuccess(result.success()).setMessage(result.message());

    if (result.checkout() != null) {
      builder.setCheckout(mapper.toResponseGrpc(result.checkout()));
    }

    result.skuLockSummary()
        .forEach(summary -> builder.addSkuLockSummary(SkuLockSummary.newBuilder()
            .setSku(summary.sku())
            .setSubSku(summary.subSku())
            .setLocked(summary.locked())
            .setRequestedQuantity(summary.requestedQuantity())
            .setLockedQuantity(summary.lockedQuantity())
            .setAvailableStock(summary.availableStock())
            .setErrorMessage(summary.errorMessage() != null ? summary.errorMessage() : "")
            .build()));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void finalizeCheckout(FinalizeCheckoutRequest request,
      StreamObserver<FinalizeCheckoutResponse> responseObserver) {
    // Extract address from request
    String addressId = request.hasAddressId() ? request.getAddressId() : null;
    AddressSnapshot newAddress =
        request.hasNewAddress() ? mapper.toAddressSnapshotEntity(request.getNewAddress()) : null;

    // Validate with ownership check
    validationService.validateFinalizeCheckoutWithOwnership(
        request.getCheckoutId(), request.getUserId(), addressId, request.hasNewAddress());

    FinalizeCheckoutResult result = checkoutService.finalizeCheckout(
        request.getCheckoutId(), request.getUserId(), addressId, newAddress);

    FinalizeCheckoutResponse.Builder builder =
        FinalizeCheckoutResponse.newBuilder().setSuccess(result.success()).setMessage(result.message());

    if (result.checkout() != null) {
      builder.setCheckout(mapper.toResponseGrpc(result.checkout()));
    }
    if (result.orderId() != null) {
      builder.setOrderId(result.orderId());
    }
    if (result.paymentCode() != null) {
      builder.setPaymentCode(result.paymentCode());
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void payCheckout(PayCheckoutRequest request, StreamObserver<PayCheckoutResponse> responseObserver) {
    // Validate with ownership check
    validationService.validatePayCheckoutWithOwnership(request.getCheckoutId(), request.getUserId());

    PayCheckoutResult result = checkoutService.payCheckout(request.getCheckoutId(), request.getUserId());

    PayCheckoutResponse.Builder builder =
        PayCheckoutResponse.newBuilder().setSuccess(result.success()).setMessage(result.message());

    if (result.checkout() != null) {
      builder.setCheckout(mapper.toResponseGrpc(result.checkout()));
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void cancelCheckout(CancelCheckoutRequest request, StreamObserver<CancelCheckoutResponse> responseObserver) {
    // Validate with ownership check
    validationService.validateCancelCheckoutWithOwnership(request.getCheckoutId(), request.getUserId());

    String reason = request.hasReason() ? request.getReason() : null;
    boolean result = checkoutService.cancelCheckout(request.getCheckoutId(), request.getUserId(), reason);

    CancelCheckoutResponse response = CancelCheckoutResponse.newBuilder()
        .setSuccess(result)
        .setMessage(result ? "Checkout cancelled" : "Failed to cancel checkout")
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  // ============================================================
  // Retrieval APIs
  // ============================================================

  @Override
  public void getCheckout(GetCheckoutRequest request, StreamObserver<CheckoutData> responseObserver) {
    // Validate with ownership check
    validationService.validateGetCheckoutWithOwnership(request.getCheckoutId(), request.getUserId());

    Checkout checkout = checkoutService.getCheckout(request.getCheckoutId(), request.getUserId());
    CheckoutData response = mapper.toResponseGrpc(checkout);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getCheckoutByUser(GetCheckoutByUserRequest request, StreamObserver<CheckoutData> responseObserver) {
    validationService.validateGetCheckoutByUser(request.getUserId());

    Checkout checkout = checkoutService.getActiveCheckoutByUser(request.getUserId());
    CheckoutData response = mapper.toResponseGrpc(checkout);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterCheckouts(FilterCheckoutRequest request,
      StreamObserver<MultipleCheckoutResponse> responseObserver) {
    // Ensure user_id is always set for security (the Gateway injects it)
    // This prevents users from querying other users' checkouts
    validationService.validateUserId(request.getUserId());
    
    MultipleCheckoutResponse response = checkoutService.filterCheckouts(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}