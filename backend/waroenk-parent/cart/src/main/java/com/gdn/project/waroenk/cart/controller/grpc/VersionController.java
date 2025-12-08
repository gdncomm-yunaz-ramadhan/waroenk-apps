package com.gdn.project.waroenk.cart.controller.grpc;

import com.gdn.project.waroenk.common.Empty;
import com.gdn.project.waroenk.common.VersionResponse;
import com.gdn.project.waroenk.common.VersionServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;

@GrpcService
@RequiredArgsConstructor
public class VersionController extends VersionServiceGrpc.VersionServiceImplBase {

  @Value("${spring.application.name}")
  private String appName;

  @Value("${info.app.version}")
  private String version;

  @Override
  public void getVersion(Empty request, StreamObserver<VersionResponse> responseObserver) {
    VersionResponse response = VersionResponse.newBuilder().setName(appName).setVersion(version).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}



