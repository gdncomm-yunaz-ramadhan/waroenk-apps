package com.gdn.project.waroenk.member.service;

import com.gdn.project.waroenk.member.AuthenticateRequest;
import com.gdn.project.waroenk.member.ChangePasswordRequest;
import com.gdn.project.waroenk.member.ChangePasswordResponse;
import com.gdn.project.waroenk.member.FilterUserRequest;
import com.gdn.project.waroenk.member.ForgotPasswordRequest;
import com.gdn.project.waroenk.member.ForgotPasswordResponse;
import com.gdn.project.waroenk.member.LogoutRequest;
import com.gdn.project.waroenk.member.LogoutResponse;
import com.gdn.project.waroenk.member.MultipleUserResponse;
import com.gdn.project.waroenk.member.RefreshTokenRequest;
import com.gdn.project.waroenk.member.RefreshTokenResponse;
import com.gdn.project.waroenk.member.UserTokenResponse;
import com.gdn.project.waroenk.member.entity.User;

public interface UserService {

  User findUserById(String id);

  User findUserByPhoneOrEmail(String query);

  User registerUser(User user);

  User updateUser(User user);

  MultipleUserResponse filterUsers(FilterUserRequest request);

  UserTokenResponse login(AuthenticateRequest request);

  LogoutResponse logout(LogoutRequest request);

  ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

  ChangePasswordResponse changePassword(ChangePasswordRequest request);

  RefreshTokenResponse refreshToken(RefreshTokenRequest request);
}
