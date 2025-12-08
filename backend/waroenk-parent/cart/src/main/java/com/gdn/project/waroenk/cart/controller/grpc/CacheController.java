package com.gdn.project.waroenk.cart.controller.grpc;

import com.gdn.project.waroenk.cart.service.CacheService;
import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.CacheServiceGrpc;
import com.gdn.project.waroenk.common.Empty;
import com.gdn.project.waroenk.common.FlushPattern;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CacheController extends CacheServiceGrpc.CacheServiceImplBase {
    private final CacheService cacheService;

    @Override
    public void flushAll(Empty request, StreamObserver<Basic> responseObserver) {
        Basic response = Basic.newBuilder().setStatus(cacheService.flushAll()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void flush(FlushPattern request, StreamObserver<Basic> responseObserver) {
        Basic response = Basic.newBuilder().setStatus(cacheService.flushCacheWithPattern(request.getKey())).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}



