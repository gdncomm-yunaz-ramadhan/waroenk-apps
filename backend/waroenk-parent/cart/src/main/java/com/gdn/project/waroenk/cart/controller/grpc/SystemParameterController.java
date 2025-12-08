package com.gdn.project.waroenk.cart.controller.grpc;

import com.gdn.project.waroenk.cart.CartSystemParameterServiceGrpc;
import com.gdn.project.waroenk.cart.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.cart.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.cart.OneSystemParameterRequest;
import com.gdn.project.waroenk.cart.SystemParameterData;
import com.gdn.project.waroenk.cart.UpsertSystemParameterRequest;
import com.gdn.project.waroenk.cart.entity.SystemParameter;
import com.gdn.project.waroenk.cart.mapper.SystemParameterMapper;
import com.gdn.project.waroenk.cart.service.SystemParameterService;
import com.gdn.project.waroenk.common.Basic;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class SystemParameterController extends CartSystemParameterServiceGrpc.CartSystemParameterServiceImplBase {

  private static final SystemParameterMapper mapper = SystemParameterMapper.INSTANCE;
  private final SystemParameterService systemParameterService;

  @Override
  public void createSystemParameter(UpsertSystemParameterRequest request,
      StreamObserver<SystemParameterData> responseObserver) {
    SystemParameter entity = mapper.toEntity(request);
    SystemParameter saved = systemParameterService.upsert(entity);
    SystemParameterData response = mapper.toResponseGrpc(saved);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateSystemParameter(UpsertSystemParameterRequest request,
      StreamObserver<SystemParameterData> responseObserver) {
    SystemParameter entity = mapper.toEntity(request);
    SystemParameter updated = systemParameterService.upsert(entity);
    SystemParameterData response = mapper.toResponseGrpc(updated);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteSystemParameter(OneSystemParameterRequest request, StreamObserver<Basic> responseObserver) {
    boolean result = systemParameterService.delete(request.getVariable());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findSystemParameterByVariable(OneSystemParameterRequest request,
      StreamObserver<SystemParameterData> responseObserver) {
    SystemParameter param = systemParameterService.get(request.getVariable());
    SystemParameterData response = mapper.toResponseGrpc(param);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterSystemParameters(MultipleSystemParameterRequest request,
      StreamObserver<MultipleSystemParameterResponse> responseObserver) {
    MultipleSystemParameterResponse response = systemParameterService.filter(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}



