package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.catalog.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.catalog.OneSystemParameterRequest;
import com.gdn.project.waroenk.catalog.SystemParameterData;
import com.gdn.project.waroenk.catalog.SystemParameterServiceGrpc;
import com.gdn.project.waroenk.catalog.UpsertSystemParameterRequest;
import com.gdn.project.waroenk.catalog.mapper.SystemParameterMapper;
import com.gdn.project.waroenk.catalog.service.SystemParameterService;
import com.gdn.project.waroenk.common.Basic;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class SystemParameterController extends SystemParameterServiceGrpc.SystemParameterServiceImplBase {

  private static final SystemParameterMapper mapper = SystemParameterMapper.INSTANCE;
  private final SystemParameterService systemParameterService;

  @Override
  public void findOneSystemParameter(OneSystemParameterRequest request,
      StreamObserver<SystemParameterData> responseObserver) {
    SystemParameterData response =
        mapper.toResponseGrpc(systemParameterService.findOneSystemParameter(request.getVariable()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteSystemParameter(OneSystemParameterRequest request, StreamObserver<Basic> responseObserver) {
    boolean result = systemParameterService.deleteSystemParameter(request.getVariable());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void upsertSystemParameter(UpsertSystemParameterRequest request,
      StreamObserver<SystemParameterData> responseObserver) {
    SystemParameterData response =
        mapper.toResponseGrpc(systemParameterService.upsertSystemParameter(mapper.toEntity(request)));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findAllSystemParameter(MultipleSystemParameterRequest request,
      StreamObserver<MultipleSystemParameterResponse> responseObserver) {
    MultipleSystemParameterResponse response = systemParameterService.findAllSystemParameters(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}



