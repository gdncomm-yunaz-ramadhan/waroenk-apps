package com.gdn.project.waroenk.member.controller.grpc;

import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.Id;
import com.gdn.project.waroenk.member.AddressData;
import com.gdn.project.waroenk.member.AddressServiceGrpc;
import com.gdn.project.waroenk.member.FilterAddressRequest;
import com.gdn.project.waroenk.member.FindUserAddressRequest;
import com.gdn.project.waroenk.member.MultipleAddressResponse;
import com.gdn.project.waroenk.member.SetDefaultAddressRequest;
import com.gdn.project.waroenk.member.UpsertAddressRequest;
import com.gdn.project.waroenk.member.entity.Address;
import com.gdn.project.waroenk.member.exceptions.ValidationException;
import com.gdn.project.waroenk.member.mapper.AddressMapper;
import com.gdn.project.waroenk.member.service.AddressService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.StringUtils;

@GrpcService
@RequiredArgsConstructor
public class AddressController extends AddressServiceGrpc.AddressServiceImplBase {

  private static final AddressMapper mapper = AddressMapper.INSTANCE;
  private static final int MIN_FILTER_LENGTH = 3;

  private final AddressService addressService;

  @Override
  public void upsertAddress(UpsertAddressRequest request, StreamObserver<AddressData> responseObserver) {
    validateUpsertAddressRequest(request);

    Address address = mapper.toAddressEntity(request);
    // Pass the isDefault flag to the service - it will handle setting this address as default
    Address savedAddress = addressService.createAddress(request.getUserId(), address, request.getIsDefault());
    // Get the user's default address ID to set isDefault flag in response
    java.util.UUID defaultAddressId = addressService.getDefaultAddressId(request.getUserId());
    AddressData response = mapper.toAddressData(savedAddress, defaultAddressId);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteAddressById(Id request, StreamObserver<Basic> responseObserver) {
    if (StringUtils.isBlank(request.getValue())) {
      throw new ValidationException("Address ID is required");
    }

    boolean result = addressService.deleteUserAddress(request.getValue());
    Basic response = Basic.newBuilder().setStatus(result).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void setUserDefaultAddress(SetDefaultAddressRequest request, StreamObserver<Basic> responseObserver) {
    if (StringUtils.isBlank(request.getUserId())) {
      throw new ValidationException("User ID is required");
    }
    if (StringUtils.isBlank(request.getAddressId())) {
      throw new ValidationException("Address ID is required");
    }

    boolean result = addressService.setDefaultAddress(request.getUserId(), request.getAddressId());
    Basic response = Basic.newBuilder().setStatus(result).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findAddressById(Id request, StreamObserver<AddressData> responseObserver) {
    if (StringUtils.isBlank(request.getValue())) {
      throw new ValidationException("Address ID is required");
    }

    Address address = addressService.findAddressById(request.getValue());
    // Get the user's default address ID to set isDefault flag
    java.util.UUID defaultAddressId = address.getUser() != null 
        ? addressService.getDefaultAddressId(address.getUser().getId().toString())
        : null;
    AddressData response = mapper.toAddressData(address, defaultAddressId);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findOneUserAddressByLabel(FindUserAddressRequest request, StreamObserver<AddressData> responseObserver) {
    if (StringUtils.isBlank(request.getUserId())) {
      throw new ValidationException("User ID is required");
    }
    if (StringUtils.isBlank(request.getLabel())) {
      throw new ValidationException("Address label is required");
    }

    Address address = addressService.findUserAddressByLabel(request.getUserId(), request.getLabel());
    // Get the user's default address ID to set isDefault flag
    java.util.UUID defaultAddressId = addressService.getDefaultAddressId(request.getUserId());
    AddressData response = mapper.toAddressData(address, defaultAddressId);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterUserAddress(FilterAddressRequest request, StreamObserver<MultipleAddressResponse> responseObserver) {
    if (StringUtils.isBlank(request.getUser())) {
      throw new ValidationException("User ID is required");
    }
    if (StringUtils.isNotBlank(request.getLabel()) && request.getLabel().trim().length() < MIN_FILTER_LENGTH) {
      throw new ValidationException("Filter query must be at least " + MIN_FILTER_LENGTH + " characters");
    }

    MultipleAddressResponse response = addressService.filterUserAddress(request);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private void validateUpsertAddressRequest(UpsertAddressRequest request) {
    if (StringUtils.isBlank(request.getUserId())) {
      throw new ValidationException("User ID is required");
    }
    if (StringUtils.isBlank(request.getLabel())) {
      throw new ValidationException("Label is required");
    }
    if (StringUtils.isBlank(request.getCountry())) {
      throw new ValidationException("Country is required");
    }
    if (StringUtils.isBlank(request.getProvince())) {
      throw new ValidationException("Province is required");
    }
    if (StringUtils.isBlank(request.getCity())) {
      throw new ValidationException("City is required");
    }
    if (StringUtils.isBlank(request.getDistrict())) {
      throw new ValidationException("District is required");
    }
    if (StringUtils.isBlank(request.getSubDistrict())) {
      throw new ValidationException("Subdistrict is required");
    }
    if (StringUtils.isBlank(request.getPostalCode())) {
      throw new ValidationException("Postal code is required");
    }
    if (StringUtils.isBlank(request.getStreet())) {
      throw new ValidationException("Street is required");
    }
  }
}









