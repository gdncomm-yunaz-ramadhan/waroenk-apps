package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.AdjustStockRequest;
import com.gdn.project.waroenk.catalog.BulkAcquireStockRequest;
import com.gdn.project.waroenk.catalog.BulkAcquireStockResponse;
import com.gdn.project.waroenk.catalog.BulkAdjustStockRequest;
import com.gdn.project.waroenk.catalog.BulkAdjustStockResponse;
import com.gdn.project.waroenk.catalog.BulkLockStockRequest;
import com.gdn.project.waroenk.catalog.BulkLockStockResponse;
import com.gdn.project.waroenk.catalog.BulkReleaseStockRequest;
import com.gdn.project.waroenk.catalog.BulkReleaseStockResponse;
import com.gdn.project.waroenk.catalog.BulkUpdateStockRequest;
import com.gdn.project.waroenk.catalog.BulkUpdateStockResponse;
import com.gdn.project.waroenk.catalog.CreateInventoryRequest;
import com.gdn.project.waroenk.catalog.FilterInventoryRequest;
import com.gdn.project.waroenk.catalog.FindInventoryBySubSkuRequest;
import com.gdn.project.waroenk.catalog.InventoryData;
import com.gdn.project.waroenk.catalog.InventoryServiceGrpc;
import com.gdn.project.waroenk.catalog.MultipleInventoryResponse;
import com.gdn.project.waroenk.catalog.UpdateInventoryRequest;
import com.gdn.project.waroenk.catalog.UpdateStockBySubSkuRequest;
import com.gdn.project.waroenk.catalog.entity.Inventory;
import com.gdn.project.waroenk.catalog.mapper.InventoryMapper;
import com.gdn.project.waroenk.catalog.service.InventoryService;
import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.Id;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryController extends InventoryServiceGrpc.InventoryServiceImplBase {

  private static final InventoryMapper mapper = InventoryMapper.INSTANCE;
  private final InventoryService inventoryService;

  @Override
  public void createInventory(CreateInventoryRequest request, StreamObserver<InventoryData> responseObserver) {
    Inventory inventory = mapper.toEntity(request);
    InventoryData response = mapper.toResponseGrpc(inventoryService.createInventory(inventory));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateInventory(UpdateInventoryRequest request, StreamObserver<InventoryData> responseObserver) {
    Inventory inventory = Inventory.builder()
        .subSku(request.getSubSku())
        .stock(request.getStock())
        .build();
    InventoryData response = mapper.toResponseGrpc(inventoryService.updateInventory(request.getId(), inventory));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateStockBySubSku(UpdateStockBySubSkuRequest request, StreamObserver<InventoryData> responseObserver) {
    InventoryData response = mapper.toResponseGrpc(inventoryService.updateStockBySubSku(request.getSubSku(), request.getStock()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void adjustStock(AdjustStockRequest request, StreamObserver<InventoryData> responseObserver) {
    InventoryData response = mapper.toResponseGrpc(inventoryService.adjustStock(request.getSubSku(), request.getQuantity()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void bulkUpdateStock(BulkUpdateStockRequest request, StreamObserver<BulkUpdateStockResponse> responseObserver) {
    BulkUpdateStockResponse response = inventoryService.bulkUpdateStock(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteInventory(Id request, StreamObserver<Basic> responseObserver) {
    boolean result = inventoryService.deleteInventory(request.getValue());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findInventoryById(Id request, StreamObserver<InventoryData> responseObserver) {
    InventoryData response = mapper.toResponseGrpc(inventoryService.findInventoryById(request.getValue()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findInventoryBySubSku(FindInventoryBySubSkuRequest request, StreamObserver<InventoryData> responseObserver) {
    InventoryData response = mapper.toResponseGrpc(inventoryService.findInventoryBySubSku(request.getSubSku()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterInventory(FilterInventoryRequest request, StreamObserver<MultipleInventoryResponse> responseObserver) {
    MultipleInventoryResponse response = inventoryService.filterInventory(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  // ============================================================
  // Bulk Lock/Acquire/Release Operations for Cart/Checkout
  // ============================================================

  @Override
  public void bulkLockStock(BulkLockStockRequest request, StreamObserver<BulkLockStockResponse> responseObserver) {
    try {
      BulkLockStockResponse response = inventoryService.bulkLockStock(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error in bulkLockStock for checkout {}: {}", request.getCheckoutId(), e.getMessage(), e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Failed to lock stock: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void bulkAcquireStock(BulkAcquireStockRequest request, StreamObserver<BulkAcquireStockResponse> responseObserver) {
    try {
      BulkAcquireStockResponse response = inventoryService.bulkAcquireStock(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error in bulkAcquireStock for checkout {}: {}", request.getCheckoutId(), e.getMessage(), e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Failed to acquire stock: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void bulkReleaseStock(BulkReleaseStockRequest request, StreamObserver<BulkReleaseStockResponse> responseObserver) {
    try {
      BulkReleaseStockResponse response = inventoryService.bulkReleaseStock(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error in bulkReleaseStock for checkout {}: {}", request.getCheckoutId(), e.getMessage(), e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Failed to release stock: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void bulkAdjustStock(BulkAdjustStockRequest request, StreamObserver<BulkAdjustStockResponse> responseObserver) {
    try {
      BulkAdjustStockResponse response = inventoryService.bulkAdjustStock(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error in bulkAdjustStock: {}", e.getMessage(), e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Failed to adjust stock: " + e.getMessage())
          .asRuntimeException());
    }
  }
}




