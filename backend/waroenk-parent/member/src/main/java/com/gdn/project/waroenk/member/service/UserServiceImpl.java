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
import com.gdn.project.waroenk.member.entity.Token;
import com.gdn.project.waroenk.member.entity.User;
import com.gdn.project.waroenk.member.exceptions.DuplicateResourceException;
import com.gdn.project.waroenk.member.exceptions.InvalidCredentialsException;
import com.gdn.project.waroenk.member.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.member.exceptions.ValidationException;
import com.gdn.project.waroenk.member.mapper.UserMapper;
import com.gdn.project.waroenk.member.repository.PageAble;
import com.gdn.project.waroenk.member.repository.TokenRepository;
import com.gdn.project.waroenk.member.repository.UserRepository;
import com.gdn.project.waroenk.member.repository.model.ResultData;
import com.gdn.project.waroenk.member.utility.CacheUtil;
import com.gdn.project.waroenk.member.utility.JwtUtil;
import com.gdn.project.waroenk.member.utility.ParserUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends PageAble<User, UUID> implements UserService {

  private static final UserMapper mapper = UserMapper.INSTANCE;
  private static final String USER_PREFIX = "user";
  private static final String RESET_TOKEN_PREFIX = "user:reset:";
  private static final int MIN_FILTER_LENGTH = 3;

  private final UserRepository userRepository;
  private final TokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final CacheUtil<User> userCacheUtil;
  private final CacheUtil<String> stringCacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public UserServiceImpl(UserRepository userRepository,
      TokenRepository tokenRepository,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil,
      CacheUtil<User> userCacheUtil,
      CacheUtil<String> stringCacheUtil,
      EntityManager entityManager) {
    super(USER_PREFIX, stringCacheUtil, entityManager, 10, TimeUnit.MINUTES);
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.userCacheUtil = userCacheUtil;
    this.stringCacheUtil = stringCacheUtil;
  }

  @Override
  public User findUserById(String id) {
    String key = USER_PREFIX + ":id:" + id;
    User cached = userCacheUtil.getValue(key);

    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    UUID userId = UUID.fromString(id);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User with id: " + id + " not found"));

    userCacheUtil.putValue(key, user, 1, TimeUnit.HOURS);
    return user;
  }

  @Override
  public User findUserByPhoneOrEmail(String query) {
    if (StringUtils.isBlank(query)) {
      throw new ValidationException("Phone or email query is required");
    }

    String normalizedQuery = query.trim().toLowerCase();
    String key = USER_PREFIX + ":contact:" + normalizedQuery;
    User cached = userCacheUtil.getValue(key);

    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    User user = userRepository.findByPhoneNumberOrEmail(normalizedQuery, normalizedQuery)
        .orElseThrow(() -> new ResourceNotFoundException("User with phone/email: " + query + " not found"));

    userCacheUtil.putValue(key, user, 1, TimeUnit.HOURS);
    return user;
  }

  @Override
  @Transactional
  public User registerUser(User user) {
    try {
      User saved = userRepository.save(user);
      evictUserCaches(saved);
      return saved;
    } catch (DataIntegrityViolationException e) {
      String message = e.getMessage();
      if (message != null && message.contains("email")) {
        throw new DuplicateResourceException("Email already registered");
      } else if (message != null && message.contains("phone")) {
        throw new DuplicateResourceException("Phone number already registered");
      }
      throw new DuplicateResourceException("User already exists");
    }
  }

  @Override
  public MultipleUserResponse filterUsers(FilterUserRequest request) {
    String filterQuery = request.getQuery();
    if (StringUtils.isNotBlank(filterQuery) && filterQuery.trim().length() < MIN_FILTER_LENGTH) {
      throw new ValidationException("Filter query must be at least " + MIN_FILTER_LENGTH + " characters");
    }

    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    PredicateBuilder<User> predicateBuilder = (root, criteriaBuilder) -> {
      List<Predicate> predicateList = new ArrayList<>();
      if (StringUtils.isNotBlank(filterQuery)) {
        String likePattern = "%" + filterQuery.trim().toLowerCase() + "%";
        Predicate phoneLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), likePattern);
        Predicate emailLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern);
        Predicate fullNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), likePattern);
        predicateList.add(criteriaBuilder.or(phoneLike, emailLike, fullNameLike));
      }
      return predicateList;
    };

    ResultData<User> entries = query(predicateBuilder, size, request.getCursor(),
        PageAble.SortInfo.of(request.getSortBy().getField(), request.getSortBy().getDirection()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<User> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId().toString());
    }

    MultipleUserResponse.Builder builder = MultipleUserResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toUserData(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);

    return builder.build();
  }

  @Override
  @Transactional
  public UserTokenResponse login(AuthenticateRequest request) {
    String userIdentifier = request.getUser();
    String password = request.getPassword();

    if (StringUtils.isBlank(userIdentifier) || StringUtils.isBlank(password)) {
      throw new ValidationException("User and password are required");
    }

    User user = userRepository.findByPhoneNumberOrEmail(userIdentifier.trim(), userIdentifier.trim().toLowerCase())
        .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new InvalidCredentialsException("Invalid credentials");
    }

    String accessToken = jwtUtil.generateAccessToken(user);
    String refreshToken = jwtUtil.generateRefreshToken();
    long expiresIn = jwtUtil.getAccessTokenExpirySeconds();
    long refreshTokenExpiryHours = jwtUtil.getRefreshTokenExpiryHours();

    Optional<Token> existingToken = tokenRepository.findByUserId(user.getId());
    Token token;
    if (existingToken.isPresent()) {
      token = existingToken.get();
      token.setRefreshToken(refreshToken);
      token.setExpiresAt(LocalDateTime.now().plusHours(refreshTokenExpiryHours));
    } else {
      token = Token.builder()
          .user(user)
          .refreshToken(refreshToken)
          .expiresAt(LocalDateTime.now().plusHours(refreshTokenExpiryHours))
          .build();
    }
    tokenRepository.save(token);

    String refreshTokenKey = USER_PREFIX + ":refresh:" + user.getId();
    stringCacheUtil.putValue(refreshTokenKey, refreshToken, refreshTokenExpiryHours, TimeUnit.HOURS);

    return UserTokenResponse.newBuilder()
        .setAccessToken(accessToken)
        .setTokenType("Bearer")
        .setExpiresIn(expiresIn)
        .setUserId(user.getId().toString())
        .build();
  }

  @Override
  @Transactional
  public User updateUser(User updatedUser) {
    User existingUser = findUserById(updatedUser.getId().toString());

    if (StringUtils.isNotBlank(updatedUser.getFullName())) {
      existingUser.setFullName(updatedUser.getFullName());
    }
    if (updatedUser.getEmail() != null) {
      existingUser.setEmail(updatedUser.getEmail());
    }
    if (updatedUser.getPhoneNumber() != null) {
      existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
    }
    if (updatedUser.getDob() != null) {
      existingUser.setDob(updatedUser.getDob());
    }
    if (updatedUser.getGender() != null) {
      existingUser.setGender(updatedUser.getGender());
    }

    try {
      User saved = userRepository.save(existingUser);
      evictUserCaches(saved);
      return saved;
    } catch (DataIntegrityViolationException e) {
      String message = e.getMessage();
      if (message != null && message.contains("email")) {
        throw new DuplicateResourceException("Email already registered");
      } else if (message != null && message.contains("phone")) {
        throw new DuplicateResourceException("Phone number already registered");
      }
      throw new DuplicateResourceException("User data conflict");
    }
  }

  private void evictUserCaches(User user) {
    if (user.getId() != null) {
      userCacheUtil.removeValue(USER_PREFIX + ":id:" + user.getId());
    }
    if (user.getEmail() != null) {
      userCacheUtil.removeValue(USER_PREFIX + ":contact:" + user.getEmail().toLowerCase());
    }
    if (user.getPhoneNumber() != null) {
      userCacheUtil.removeValue(USER_PREFIX + ":contact:" + user.getPhoneNumber());
    }
    stringCacheUtil.flushKeysByPattern(USER_PREFIX + ":ids:*");
    stringCacheUtil.flushKeysByPattern(USER_PREFIX + ":count:*");
  }

  @Override
  @Transactional
  public LogoutResponse logout(LogoutRequest request) {
    String userId = request.getUserId();
    if (StringUtils.isBlank(userId)) {
      throw new ValidationException("User ID is required");
    }

    try {
      UUID userUUID = UUID.fromString(userId);

      // Blacklist the access token if provided
      if (StringUtils.isNotBlank(request.getAccessToken())) {
        jwtUtil.blacklistToken(request.getAccessToken());
      }

      // Delete refresh token from database
      tokenRepository.deleteByUserId(userUUID);

      // Remove refresh token from cache
      stringCacheUtil.removeValue(USER_PREFIX + ":refresh:" + userId);

      log.info("User {} logged out successfully", userId);

      return LogoutResponse.newBuilder()
          .setSuccess(true)
          .setMessage("Logout successful")
          .build();
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Invalid user ID format");
    }
  }

  @Override
  public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
    String phoneOrEmail = request.getPhoneOrEmail();
    if (StringUtils.isBlank(phoneOrEmail)) {
      throw new ValidationException("Phone or email is required");
    }

    // Find user by phone or email
    Optional<User> userOpt = userRepository.findByPhoneNumberOrEmail(
        phoneOrEmail.trim(), phoneOrEmail.trim().toLowerCase());

    if (userOpt.isEmpty()) {
      // Return success even if user not found for security (don't reveal user existence)
      log.info("Forgot password requested for non-existent user: {}", phoneOrEmail);
      return ForgotPasswordResponse.newBuilder()
          .setSuccess(true)
          .setMessage("If your account exists, you will receive a password reset token")
          .setResetToken("")
          .setExpiresInSeconds(0)
          .build();
    }

    User user = userOpt.get();

    // Generate reset token
    String resetToken = jwtUtil.generateResetToken();
    long expirySeconds = jwtUtil.getResetTokenExpirySeconds();

    // Store reset token in Redis with user ID
    String resetKey = RESET_TOKEN_PREFIX + resetToken;
    stringCacheUtil.putValue(resetKey, user.getId().toString(), expirySeconds, TimeUnit.SECONDS);

    log.info("Password reset token generated for user: {}", user.getId());

    return ForgotPasswordResponse.newBuilder()
        .setSuccess(true)
        .setMessage("Password reset token generated successfully")
        .setResetToken(resetToken)
        .setExpiresInSeconds(expirySeconds)
        .build();
  }

  @Override
  @Transactional
  public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
    String resetToken = request.getResetToken();
    String newPassword = request.getNewPassword();
    String confirmPassword = request.getConfirmPassword();

    if (StringUtils.isBlank(resetToken)) {
      throw new ValidationException("Reset token is required");
    }
    if (StringUtils.isBlank(newPassword)) {
      throw new ValidationException("New password is required");
    }
    if (StringUtils.isBlank(confirmPassword)) {
      throw new ValidationException("Confirm password is required");
    }
    if (!newPassword.equals(confirmPassword)) {
      throw new ValidationException("Passwords do not match");
    }

    // Validate reset token from Redis
    String resetKey = RESET_TOKEN_PREFIX + resetToken;
    String userId = stringCacheUtil.getValue(resetKey);

    if (StringUtils.isBlank(userId)) {
      throw new InvalidCredentialsException("Invalid or expired reset token");
    }

    // Find user
    User user = userRepository.findById(UUID.fromString(userId))
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Update password
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    // Invalidate the reset token
    stringCacheUtil.removeValue(resetKey);

    // Invalidate all existing tokens for this user
    tokenRepository.deleteByUserId(user.getId());
    stringCacheUtil.removeValue(USER_PREFIX + ":refresh:" + user.getId());

    log.info("Password changed successfully for user: {}", user.getId());

    return ChangePasswordResponse.newBuilder()
        .setSuccess(true)
        .setMessage("Password changed successfully")
        .build();
  }

  @Override
  @Transactional
  public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();
    if (StringUtils.isBlank(refreshToken)) {
      throw new ValidationException("Refresh token is required");
    }

    // Find token in database
    Token token = tokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

    // Check if token is expired
    if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
      tokenRepository.delete(token);
      throw new InvalidCredentialsException("Refresh token has expired");
    }

    User user = token.getUser();

    // Generate new access token
    String newAccessToken = jwtUtil.generateAccessToken(user);
    String newRefreshToken = jwtUtil.generateRefreshToken();
    long expiresIn = jwtUtil.getAccessTokenExpirySeconds();
    long refreshTokenExpiryHours = jwtUtil.getRefreshTokenExpiryHours();

    // Update refresh token
    token.setRefreshToken(newRefreshToken);
    token.setExpiresAt(LocalDateTime.now().plusHours(refreshTokenExpiryHours));
    tokenRepository.save(token);

    // Update cache
    String refreshTokenKey = USER_PREFIX + ":refresh:" + user.getId();
    stringCacheUtil.putValue(refreshTokenKey, newRefreshToken, refreshTokenExpiryHours, TimeUnit.HOURS);

    log.info("Token refreshed for user: {}", user.getId());

    return RefreshTokenResponse.newBuilder()
        .setAccessToken(newAccessToken)
        .setRefreshToken(newRefreshToken)
        .setTokenType("Bearer")
        .setExpiresIn(expiresIn)
        .setUserId(user.getId().toString())
        .build();
  }

  @Override
  protected UUID toId(String input) {
    return UUID.fromString(input);
  }

  @Override
  protected UUID getId(User input) {
    return input.getId();
  }
}





