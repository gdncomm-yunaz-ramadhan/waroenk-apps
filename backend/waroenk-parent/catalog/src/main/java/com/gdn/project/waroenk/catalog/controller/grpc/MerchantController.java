package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.CreateMerchantRequest;
import com.gdn.project.waroenk.catalog.FilterMerchantRequest;
import com.gdn.project.waroenk.catalog.FindMerchantByCodeRequest;
import com.gdn.project.waroenk.catalog.MerchantData;
import com.gdn.project.waroenk.catalog.MerchantServiceGrpc;
import com.gdn.project.waroenk.catalog.MultipleMerchantResponse;
import com.gdn.project.waroenk.catalog.UpdateMerchantRequest;
import com.gdn.project.waroenk.catalog.entity.Merchant;
import com.gdn.project.waroenk.catalog.mapper.MerchantMapper;
import com.gdn.project.waroenk.catalog.service.GrpcValidationService;
import com.gdn.project.waroenk.catalog.service.MerchantService;
import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.Id;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.ObjectUtils;

@GrpcService
@RequiredArgsConstructor
public class MerchantController extends MerchantServiceGrpc.MerchantServiceImplBase {

  private static final MerchantMapper mapper = MerchantMapper.INSTANCE;
  private final MerchantService merchantService;
  private final GrpcValidationService validationService;

  @Override
  public void createMerchant(CreateMerchantRequest request, StreamObserver<MerchantData> responseObserver) {
    validationService.validateCreateMerchant(request.getName(), request.getCode());
    
    Merchant merchant = mapper.toEntity(request);
    MerchantData response = mapper.toResponseGrpc(merchantService.createMerchant(merchant));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateMerchant(UpdateMerchantRequest request, StreamObserver<MerchantData> responseObserver) {
    validationService.validateUpdateMerchant(request.getId(), request.getName(), request.getCode());
    
    Merchant merchant = Merchant.builder()
        .name(request.getName())
        .code(request.getCode())
        .contact(ObjectUtils.isNotEmpty(request.getContact()) ?
            Merchant.ContactInfo.builder()
                .phone(request.getContact().getPhone())
                .email(request.getContact().getEmail())
                .build() :
            null)
        .rating(request.getRating())
        .build();
    MerchantData response = mapper.toResponseGrpc(merchantService.updateMerchant(request.getId(), merchant));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteMerchant(Id request, StreamObserver<Basic> responseObserver) {
    validationService.validateId(request.getValue(), "Merchant ID");
    
    boolean result = merchantService.deleteMerchant(request.getValue());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findMerchantById(Id request, StreamObserver<MerchantData> responseObserver) {
    validationService.validateId(request.getValue(), "Merchant ID");
    
    MerchantData response = mapper.toResponseGrpc(merchantService.findMerchantById(request.getValue()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findMerchantByCode(FindMerchantByCodeRequest request, StreamObserver<MerchantData> responseObserver) {
    validationService.validateRequired(request.getCode(), "Merchant code");
    
    MerchantData response = mapper.toResponseGrpc(merchantService.findMerchantByCode(request.getCode()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterMerchant(FilterMerchantRequest request, StreamObserver<MultipleMerchantResponse> responseObserver) {
    MultipleMerchantResponse response = merchantService.filterMerchants(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}









