package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.BulkIndexResponse;
import com.gdn.project.waroenk.catalog.IndexBySkusRequest;
import com.gdn.project.waroenk.catalog.SeedResponse;
import com.gdn.project.waroenk.catalog.SeedServiceGrpc;
import com.gdn.project.waroenk.catalog.service.SearchService;
import com.gdn.project.waroenk.common.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC controller for TypeSense indexing operations.
 * 
 * Note: Data seeding is now handled via external Python script (data/seed_mongodb.py).
 * This controller only handles TypeSense indexing.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class SeedController extends SeedServiceGrpc.SeedServiceImplBase {

  private final SearchService searchService;

  @Override
  public void indexMerchantsInTypeSense(Empty request, StreamObserver<SeedResponse> responseObserver) {
    try {
      log.info("Starting TypeSense indexing for merchants");
      
      int indexed = searchService.indexAllMerchants();
      
      SeedResponse response = SeedResponse.newBuilder()
          .setStatus("COMPLETED")
          .setMessage(String.format("Successfully indexed %d merchants in TypeSense", indexed))
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error indexing merchants in TypeSense", e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Merchant indexing failed: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void indexProductsInTypeSense(Empty request, StreamObserver<SeedResponse> responseObserver) {
    try {
      log.info("Starting TypeSense indexing for products");
      
      int indexed = searchService.indexAllProducts();
      
      SeedResponse response = SeedResponse.newBuilder()
          .setStatus("COMPLETED")
          .setMessage(String.format("Successfully indexed %d products in TypeSense", indexed))
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error indexing products in TypeSense", e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Product indexing failed: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void indexAllInTypeSense(Empty request, StreamObserver<SeedResponse> responseObserver) {
    try {
      log.info("Starting TypeSense indexing for all data");
      
      int merchantsIndexed = searchService.indexAllMerchants();
      int productsIndexed = searchService.indexAllProducts();
      
      SeedResponse response = SeedResponse.newBuilder()
          .setStatus("COMPLETED")
          .setMessage(String.format("Successfully indexed %d merchants and %d products in TypeSense", 
              merchantsIndexed, productsIndexed))
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error indexing all data in TypeSense", e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Full indexing failed: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void indexProductsBySkus(IndexBySkusRequest request, StreamObserver<BulkIndexResponse> responseObserver) {
    try {
      log.info("Starting bulk TypeSense indexing for {} SKUs", request.getSkusCount());
      
      SearchService.BulkIndexResult result = searchService.indexProductsBySkus(request.getSkusList());
      
      BulkIndexResponse.Builder responseBuilder = BulkIndexResponse.newBuilder()
          .setStatus(result.totalFailed() == 0 ? "COMPLETED" : "PARTIAL")
          .setMessage(String.format("Indexed %d/%d products, %d failed", 
              result.totalIndexed(), result.totalRequested(), result.totalFailed()))
          .setTotalRequested(result.totalRequested())
          .setTotalIndexed(result.totalIndexed())
          .setTotalFailed(result.totalFailed());
      
      if (result.failedSkus() != null && !result.failedSkus().isEmpty()) {
        responseBuilder.addAllFailedSkus(result.failedSkus());
      }
      
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Error in bulk indexing products in TypeSense", e);
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Bulk indexing failed: " + e.getMessage())
          .asRuntimeException());
    }
  }
}
