package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.BrandData;
import com.gdn.project.waroenk.catalog.BrandServiceGrpc;
import com.gdn.project.waroenk.catalog.CreateBrandRequest;
import com.gdn.project.waroenk.catalog.FilterBrandRequest;
import com.gdn.project.waroenk.catalog.FindBrandBySlugRequest;
import com.gdn.project.waroenk.catalog.MultipleBrandResponse;
import com.gdn.project.waroenk.catalog.UpdateBrandRequest;
import com.gdn.project.waroenk.catalog.entity.Brand;
import com.gdn.project.waroenk.catalog.mapper.BrandMapper;
import com.gdn.project.waroenk.catalog.service.BrandService;
import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.Id;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class BrandController extends BrandServiceGrpc.BrandServiceImplBase {

  private static final BrandMapper mapper = BrandMapper.INSTANCE;
  private final BrandService brandService;

  @Override
  public void createBrand(CreateBrandRequest request, StreamObserver<BrandData> responseObserver) {
    Brand brand = mapper.toEntity(request);
    BrandData response = mapper.toResponseGrpc(brandService.createBrand(brand));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateBrand(UpdateBrandRequest request, StreamObserver<BrandData> responseObserver) {
    Brand brand = Brand.builder().name(request.getName()).slug(request.getSlug()).build();
    BrandData response = mapper.toResponseGrpc(brandService.updateBrand(request.getId(), brand));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteBrand(Id request, StreamObserver<Basic> responseObserver) {
    boolean result = brandService.deleteBrand(request.getValue());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findBrandById(Id request, StreamObserver<BrandData> responseObserver) {
    BrandData response = mapper.toResponseGrpc(brandService.findBrandById(request.getValue()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findBrandBySlug(FindBrandBySlugRequest request, StreamObserver<BrandData> responseObserver) {
    BrandData response = mapper.toResponseGrpc(brandService.findBrandBySlug(request.getSlug()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterBrand(FilterBrandRequest request, StreamObserver<MultipleBrandResponse> responseObserver) {
    MultipleBrandResponse response = brandService.filterBrands(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}













