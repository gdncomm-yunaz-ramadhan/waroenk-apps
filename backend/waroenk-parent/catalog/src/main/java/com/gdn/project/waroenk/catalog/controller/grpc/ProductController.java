package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.CreateProductRequest;
import com.gdn.project.waroenk.catalog.FilterProductRequest;
import com.gdn.project.waroenk.catalog.FindProductBySkuRequest;
import com.gdn.project.waroenk.catalog.MultipleProductResponse;
import com.gdn.project.waroenk.catalog.ProductData;
import com.gdn.project.waroenk.catalog.ProductServiceGrpc;
import com.gdn.project.waroenk.catalog.UpdateProductRequest;
import com.gdn.project.waroenk.catalog.entity.Product;
import com.gdn.project.waroenk.catalog.mapper.ProductMapper;
import com.gdn.project.waroenk.catalog.service.GrpcValidationService;
import com.gdn.project.waroenk.catalog.service.ProductService;
import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.Id;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.ObjectUtils;

@GrpcService
@RequiredArgsConstructor
public class ProductController extends ProductServiceGrpc.ProductServiceImplBase {

  private static final ProductMapper mapper = ProductMapper.INSTANCE;
  private final ProductService productService;
  private final GrpcValidationService validationService;

  @Override
  public void createProduct(CreateProductRequest request, StreamObserver<ProductData> responseObserver) {
    validationService.validateCreateProduct(request.getTitle(), request.getSku(), request.getMerchantCode());
    
    Product product = mapper.toEntity(request);
    ProductData response = mapper.toResponseGrpc(productService.createProduct(product));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateProduct(UpdateProductRequest request, StreamObserver<ProductData> responseObserver) {
    validationService.validateUpdateProduct(request.getId(), request.getTitle(), request.getSku(), request.getMerchantCode());
    
    Product.ProductBuilder builder = Product.builder();
    builder.title(request.getTitle());
    builder.sku(request.getSku());
    builder.merchantCode(request.getMerchantCode());
    builder.categoryId(request.getCategoryId());
    builder.brandId(request.getBrandId());
    if (ObjectUtils.isNotEmpty(request.getSummary())) {
      builder.summary(Product.ProductSummary.builder()
          .shortDescription(request.getSummary().getShortDescription())
          .tags(request.getSummary().getTagsList())
          .build());
    }
    builder.detailRef(request.getDetailRef());

    ProductData response = mapper.toResponseGrpc(productService.updateProduct(request.getId(), builder.build()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteProduct(Id request, StreamObserver<Basic> responseObserver) {
    validationService.validateId(request.getValue(), "Product ID");
    
    boolean result = productService.deleteProduct(request.getValue());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findProductById(Id request, StreamObserver<ProductData> responseObserver) {
    validationService.validateId(request.getValue(), "Product ID");
    
    ProductData response = mapper.toResponseGrpc(productService.findProductById(request.getValue()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findProductBySku(FindProductBySkuRequest request, StreamObserver<ProductData> responseObserver) {
    validationService.validateSku(request.getSku());
    
    ProductData response = mapper.toResponseGrpc(productService.findProductBySku(request.getSku()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterProduct(FilterProductRequest request, StreamObserver<MultipleProductResponse> responseObserver) {
    MultipleProductResponse response = productService.filterProducts(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}

