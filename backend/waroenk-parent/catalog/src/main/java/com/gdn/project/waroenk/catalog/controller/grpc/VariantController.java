package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.CreateVariantRequest;
import com.gdn.project.waroenk.catalog.FilterVariantRequest;
import com.gdn.project.waroenk.catalog.FindVariantBySubSkuRequest;
import com.gdn.project.waroenk.catalog.FindVariantsBySkuRequest;
import com.gdn.project.waroenk.catalog.MultipleVariantResponse;
import com.gdn.project.waroenk.catalog.SetDefaultVariantRequest;
import com.gdn.project.waroenk.catalog.UpdateVariantRequest;
import com.gdn.project.waroenk.catalog.VariantData;
import com.gdn.project.waroenk.catalog.VariantMedia;
import com.gdn.project.waroenk.catalog.VariantServiceGrpc;
import com.gdn.project.waroenk.catalog.entity.Variant;
import com.gdn.project.waroenk.catalog.mapper.VariantMapper;
import com.gdn.project.waroenk.catalog.service.GrpcValidationService;
import com.gdn.project.waroenk.catalog.service.VariantService;
import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.Id;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class VariantController extends VariantServiceGrpc.VariantServiceImplBase {

  private static final VariantMapper mapper = VariantMapper.INSTANCE;
  private final VariantService variantService;
  private final GrpcValidationService validationService;

  @Override
  public void createVariant(CreateVariantRequest request, StreamObserver<VariantData> responseObserver) {
    validationService.validateCreateVariant(request.getSku(), request.getTitle(), request.getPrice());
    
    Variant variant = mapper.toEntity(request);
    VariantData response = mapper.toResponseGrpc(variantService.createVariant(variant));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateVariant(UpdateVariantRequest request, StreamObserver<VariantData> responseObserver) {
    validationService.validateUpdateVariant(request.getId(), request.getSku(), request.getTitle(), request.getPrice());
    
    Variant.VariantBuilder builder = Variant.builder();
    builder.sku(request.getSku());
    builder.title(request.getTitle());
    builder.price(request.getPrice());
    builder.isDefault(request.getIsDefault());
    builder.attributes(mapper.structToMap(request.getAttributes()));
    builder.thumbnail(request.getThumbnail());
    if (ObjectUtils.isNotEmpty(request.getMediaList())) {
      List<Variant.VariantMedia> mediaList = new ArrayList<>();
      for (VariantMedia media : request.getMediaList()) {
        mediaList.add(Variant.VariantMedia.builder()
            .url(media.getUrl())
            .type(media.getType())
            .sortOrder(media.getSortOrder())
            .altText(media.getAltText())
            .build());
      }
      builder.media(mediaList);
    }

    VariantData response = mapper.toResponseGrpc(variantService.updateVariant(request.getId(), builder.build()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteVariant(Id request, StreamObserver<Basic> responseObserver) {
    validationService.validateId(request.getValue(), "Variant ID");
    
    boolean result = variantService.deleteVariant(request.getValue());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findVariantById(Id request, StreamObserver<VariantData> responseObserver) {
    validationService.validateId(request.getValue(), "Variant ID");
    
    VariantData response = mapper.toResponseGrpc(variantService.findVariantById(request.getValue()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findVariantBySubSku(FindVariantBySubSkuRequest request, StreamObserver<VariantData> responseObserver) {
    validationService.validateSubSku(request.getSubSku());
    
    VariantData response = mapper.toResponseGrpc(variantService.findVariantBySubSku(request.getSubSku()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findVariantsBySku(FindVariantsBySkuRequest request,
      StreamObserver<MultipleVariantResponse> responseObserver) {
    validationService.validateSku(request.getSku());
    
    MultipleVariantResponse response = variantService.findVariantsBySku(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterVariant(FilterVariantRequest request, StreamObserver<MultipleVariantResponse> responseObserver) {
    MultipleVariantResponse response = variantService.filterVariants(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void setDefaultVariant(SetDefaultVariantRequest request, StreamObserver<VariantData> responseObserver) {
    validationService.validateId(request.getVariantId(), "Variant ID");
    
    Variant variant = variantService.setDefaultVariant(request.getVariantId());
    VariantData response = mapper.toResponseGrpc(variant);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
