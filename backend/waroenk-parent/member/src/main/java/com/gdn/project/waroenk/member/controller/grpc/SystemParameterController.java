package com.gdn.project.waroenk.member.controller.grpc;

import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.member.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.member.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.member.OneSystemParameterRequest;
import com.gdn.project.waroenk.member.SystemParameterData;
import com.gdn.project.waroenk.member.SystemParameterServiceGrpc;
import com.gdn.project.waroenk.member.UpsertSystemParameterRequest;
import com.gdn.project.waroenk.member.mapper.SystemParameterMapper;
import com.gdn.project.waroenk.member.service.SystemParameterService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class SystemParameterController extends SystemParameterServiceGrpc.SystemParameterServiceImplBase {

  private static final SystemParameterMapper systemParameterMapper = SystemParameterMapper.INSTANCE;
  private final SystemParameterService systemParameterService;

  @Override
  public void findOneSystemParameter(OneSystemParameterRequest request,
      StreamObserver<SystemParameterData> responseObserver) {

    SystemParameterData response =
        systemParameterMapper.toResponseGrpc(systemParameterService.findOneSystemParameter(request.getVariable()));

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
        systemParameterMapper.toResponseGrpc(systemParameterService.upsertSystemParameter(systemParameterMapper.toSystemParameterEntity(
            request)));
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

