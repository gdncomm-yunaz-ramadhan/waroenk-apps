package com.gdn.project.waroenk.member.controller.grpc;

import com.gdn.project.waroenk.common.Id;
import com.gdn.project.waroenk.member.AuthenticateRequest;
import com.gdn.project.waroenk.member.ChangePasswordRequest;
import com.gdn.project.waroenk.member.ChangePasswordResponse;
import com.gdn.project.waroenk.member.CreateUserRequest;
import com.gdn.project.waroenk.member.CreateUserResponse;
import com.gdn.project.waroenk.member.FilterUserRequest;
import com.gdn.project.waroenk.member.ForgotPasswordRequest;
import com.gdn.project.waroenk.member.ForgotPasswordResponse;
import com.gdn.project.waroenk.member.LogoutRequest;
import com.gdn.project.waroenk.member.LogoutResponse;
import com.gdn.project.waroenk.member.MultipleUserResponse;
import com.gdn.project.waroenk.member.PhoneOrEmailRequest;
import com.gdn.project.waroenk.member.RefreshTokenRequest;
import com.gdn.project.waroenk.member.RefreshTokenResponse;
import com.gdn.project.waroenk.member.UpdateUserRequest;
import com.gdn.project.waroenk.member.UserData;
import com.gdn.project.waroenk.member.UserServiceGrpc;
import com.gdn.project.waroenk.member.UserTokenResponse;
import com.gdn.project.waroenk.member.entity.User;
import com.gdn.project.waroenk.member.exceptions.ValidationException;
import com.gdn.project.waroenk.member.mapper.UserMapper;
import com.gdn.project.waroenk.member.service.GrpcValidationService;
import com.gdn.project.waroenk.member.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@GrpcService
@RequiredArgsConstructor
public class UserController extends UserServiceGrpc.UserServiceImplBase {

  private static final UserMapper mapper = UserMapper.INSTANCE;
  private static final int MIN_FILTER_LENGTH = 3;

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final GrpcValidationService validationService;

  @Override
  public void register(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
    validateCreateUserRequest(request);

    User user = mapper.toUserEntity(request);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

    User savedUser = userService.registerUser(user);
    CreateUserResponse response = mapper.toCreateUserResponse(savedUser);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getOneUserById(Id request, StreamObserver<UserData> responseObserver) {
    if (StringUtils.isBlank(request.getValue())) {
      throw new ValidationException("User ID is required");
    }

    User user = userService.findUserById(request.getValue());
    UserData response = mapper.toUserData(user);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getOneUserByPhoneOrEmail(PhoneOrEmailRequest request, StreamObserver<UserData> responseObserver) {
    if (StringUtils.isBlank(request.getPhoneOrEmail())) {
      throw new ValidationException("Phone or email is required");
    }

    User user = userService.findUserByPhoneOrEmail(request.getPhoneOrEmail());
    UserData response = mapper.toUserData(user);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterUser(FilterUserRequest request, StreamObserver<MultipleUserResponse> responseObserver) {
    if (StringUtils.isNotBlank(request.getQuery()) && request.getQuery().trim().length() < MIN_FILTER_LENGTH) {
      throw new ValidationException("Filter query must be at least " + MIN_FILTER_LENGTH + " characters");
    }

    MultipleUserResponse response = userService.filterUsers(request);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void authenticate(AuthenticateRequest request, StreamObserver<UserTokenResponse> responseObserver) {
    if (StringUtils.isBlank(request.getUser())) {
      throw new ValidationException("User (email or phone) is required");
    }
    if (StringUtils.isBlank(request.getPassword())) {
      throw new ValidationException("Password is required");
    }

    UserTokenResponse response = userService.login(request);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateUser(UpdateUserRequest request, StreamObserver<UserData> responseObserver) {
    validateUpdateUserRequest(request);

    User user = mapper.toUserEntity(request);
    User updatedUser = userService.updateUser(user);
    UserData response = mapper.toUserData(updatedUser);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private void validateCreateUserRequest(CreateUserRequest request) {
    validationService.validateRequired(request.getFullName(), "Full name");
    validationService.validateAtLeastOneContact(request.getEmail(), request.getPhone());
    validationService.validateStrongPassword(request.getPassword());
  }

  private void validateUpdateUserRequest(UpdateUserRequest request) {
    validationService.validateRequired(request.getId(), "User ID");
    validationService.validateRequired(request.getFullName(), "Full name");
    validationService.validateAtLeastOneContact(request.getEmail(), request.getPhone());
  }

  @Override
  public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
    if (StringUtils.isBlank(request.getUserId())) {
      throw new ValidationException("User ID is required");
    }

    LogoutResponse response = userService.logout(request);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void forgotPassword(ForgotPasswordRequest request, StreamObserver<ForgotPasswordResponse> responseObserver) {
    if (StringUtils.isBlank(request.getPhoneOrEmail())) {
      throw new ValidationException("Phone or email is required");
    }

    ForgotPasswordResponse response = userService.forgotPassword(request);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void changePassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> responseObserver) {
    if (StringUtils.isBlank(request.getResetToken())) {
      throw new ValidationException("Reset token is required");
    }
    if (StringUtils.isBlank(request.getNewPassword())) {
      throw new ValidationException("New password is required");
    }
    if (StringUtils.isBlank(request.getConfirmPassword())) {
      throw new ValidationException("Confirm password is required");
    }
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new ValidationException("Passwords do not match");
    }

    ChangePasswordResponse response = userService.changePassword(request);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
    if (StringUtils.isBlank(request.getRefreshToken())) {
      throw new ValidationException("Refresh token is required");
    }

    RefreshTokenResponse response = userService.refreshToken(request);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
