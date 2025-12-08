package com.gdn.project.waroenk.member.service;

import com.gdn.project.waroenk.member.FilterAddressRequest;
import com.gdn.project.waroenk.member.MultipleAddressResponse;
import com.gdn.project.waroenk.member.entity.Address;
import com.gdn.project.waroenk.member.entity.User;
import com.gdn.project.waroenk.member.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.member.exceptions.ValidationException;
import com.gdn.project.waroenk.member.mapper.AddressMapper;
import com.gdn.project.waroenk.member.repository.AddressRepository;
import com.gdn.project.waroenk.member.repository.PageAble;
import com.gdn.project.waroenk.member.repository.UserRepository;
import com.gdn.project.waroenk.member.repository.model.ResultData;
import com.gdn.project.waroenk.member.utility.CacheUtil;
import com.gdn.project.waroenk.member.utility.ParserUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AddressServiceImpl extends PageAble<Address, UUID> implements AddressService {

  private static final AddressMapper mapper = AddressMapper.INSTANCE;
  private static final String ADDRESS_PREFIX = "address";
  private static final int MIN_FILTER_LENGTH = 3;

  private final AddressRepository addressRepository;
  private final UserRepository userRepository;
  private final CacheUtil<Address> addressCacheUtil;
  private final CacheUtil<String> stringCacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public AddressServiceImpl(AddressRepository addressRepository,
      UserRepository userRepository,
      CacheUtil<Address> addressCacheUtil,
      CacheUtil<String> stringCacheUtil,
      EntityManager entityManager) {
    super(ADDRESS_PREFIX, stringCacheUtil, entityManager, 10, TimeUnit.MINUTES);
    this.addressRepository = addressRepository;
    this.userRepository = userRepository;
    this.addressCacheUtil = addressCacheUtil;
    this.stringCacheUtil = stringCacheUtil;
  }

  @Override
  public Address findAddressById(String id) {
    String key = ADDRESS_PREFIX + ":id:" + id;
    Address cached = addressCacheUtil.getValue(key);

    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    UUID addressId = UUID.fromString(id);
    Address address = addressRepository.findById(addressId)
        .orElseThrow(() -> new ResourceNotFoundException("Address with id: " + id + " not found"));

    addressCacheUtil.putValue(key, address, 1, TimeUnit.HOURS);
    return address;
  }

  @Override
  public Address findUserAddressByLabel(String userId, String label) {
    if (StringUtils.isBlank(userId) || StringUtils.isBlank(label)) {
      throw new ValidationException("User ID and label are required");
    }

    String key = ADDRESS_PREFIX + ":user:" + userId + ":label:" + label.trim().toLowerCase();
    Address cached = addressCacheUtil.getValue(key);

    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    UUID userUuid = UUID.fromString(userId);
    Address address = addressRepository.findByUserIdAndLabel(userUuid, label.trim())
        .orElseThrow(() -> new ResourceNotFoundException("Address with label: " + label + " not found for user: " + userId));

    addressCacheUtil.putValue(key, address, 1, TimeUnit.HOURS);
    return address;
  }

  @Override
  @Transactional
  public Address createAddress(String userId, Address address, boolean setAsDefault) {
    UUID userUuid = UUID.fromString(userId);
    User user = userRepository.findById(userUuid)
        .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " not found"));

    Optional<Address> existingAddress = addressRepository.findByUserIdAndLabel(userUuid, address.getLabel().trim());
    Address saved;

    if (existingAddress.isPresent()) {
      // Update existing address
      Address existing = existingAddress.get();
      existing.setCountry(address.getCountry());
      existing.setProvince(address.getProvince());
      existing.setCity(address.getCity());
      existing.setDistrict(address.getDistrict());
      existing.setSubdistrict(address.getSubdistrict());
      existing.setPostalCode(address.getPostalCode());
      existing.setStreet(address.getStreet());
      existing.setDetails(address.getDetails());
      existing.setLatitude(address.getLatitude());
      existing.setLongitude(address.getLongitude());

      saved = addressRepository.save(existing);
      evictAddressCaches(saved, userId);
    } else {
      // Create new address
      address.setUser(user);
      saved = addressRepository.save(address);
      evictAddressCaches(saved, userId);
    }

    // Handle default address setting:
    // - If setAsDefault is true, set this address as default
    // - If this is the user's first address, auto-set as default
    boolean shouldSetDefault = setAsDefault || user.getDefaultAddress() == null;
    if (shouldSetDefault) {
      user.setDefaultAddress(saved);
      userRepository.save(user);
      evictUserAddressCaches(userId);
    }

    return saved;
  }

  @Override
  public MultipleAddressResponse filterUserAddress(FilterAddressRequest request) {
    String userId = request.getUser();
    String label = request.getLabel();

    if (StringUtils.isBlank(userId)) {
      throw new ValidationException("User ID is required");
    }

    if (StringUtils.isNotBlank(label) && label.trim().length() < MIN_FILTER_LENGTH) {
      throw new ValidationException("Filter query must be at least " + MIN_FILTER_LENGTH + " characters");
    }

    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;
    UUID userUuid = UUID.fromString(userId);

    // Fetch user to get default address ID
    User user = userRepository.findById(userUuid).orElse(null);
    UUID defaultAddressId = (user != null && user.getDefaultAddress() != null) 
        ? user.getDefaultAddress().getId() 
        : null;

    PredicateBuilder<Address> predicateBuilder = (root, criteriaBuilder) -> {
      List<Predicate> predicateList = new ArrayList<>();
      predicateList.add(criteriaBuilder.equal(root.get("user").get("id"), userUuid));

      if (StringUtils.isNotBlank(label)) {
        String likePattern = "%" + label.trim().toLowerCase() + "%";
        Predicate labelLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("label")), likePattern);
        Predicate streetLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("street")), likePattern);
        Predicate detailsLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("details")), likePattern);
        predicateList.add(criteriaBuilder.or(labelLike, streetLike, detailsLike));
      }
      return predicateList;
    };

    ResultData<Address> entries = query(predicateBuilder, size, request.getCursor(), 
        PageAble.SortInfo.of(request.getSortBy().getField(), request.getSortBy().getDirection()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Address> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId().toString());
    }

    MultipleAddressResponse.Builder builder = MultipleAddressResponse.newBuilder();
    // Pass defaultAddressId to mapper to set is_default flag
    UUID finalDefaultAddressId = defaultAddressId;
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toAddressData(item, finalDefaultAddressId)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);

    return builder.build();
  }

  @Override
  @Transactional
  public boolean setDefaultAddress(String userId, String addressId) {
    if (StringUtils.isBlank(userId) || StringUtils.isBlank(addressId)) {
      throw new ValidationException("User ID and address ID are required");
    }

    UUID userUuid = UUID.fromString(userId);
    UUID addressUuid = UUID.fromString(addressId);
    
    User user = userRepository.findById(userUuid)
        .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " not found"));

    Address address = addressRepository.findById(addressUuid)
        .orElseThrow(() -> new ResourceNotFoundException("Address with id: " + addressId + " not found"));

    // Verify the address belongs to the user
    if (!address.getUser().getId().equals(userUuid)) {
      throw new ValidationException("Address does not belong to this user");
    }

    user.setDefaultAddress(address);
    userRepository.save(user);

    evictUserAddressCaches(userId);
    return true;
  }

  @Override
  @Transactional
  public boolean deleteUserAddress(String id) {
    UUID addressId = UUID.fromString(id);
    Address address = addressRepository.findById(addressId)
        .orElseThrow(() -> new ResourceNotFoundException("Address with id: " + id + " not found"));

    String userId = address.getUser().getId().toString();

    User user = address.getUser();
    boolean wasDefaultAddress = user.getDefaultAddress() != null 
        && user.getDefaultAddress().getId().equals(addressId);
    
    if (wasDefaultAddress) {
      // Clear the default address reference before deleting
      user.setDefaultAddress(null);
      userRepository.save(user);
    }

    addressRepository.delete(address);
    evictAddressCaches(address, userId);
    
    // Also invalidate user cache if we deleted the default address
    if (wasDefaultAddress) {
      evictUserAddressCaches(userId);
    }
    
    return true;
  }

  private void evictAddressCaches(Address address, String userId) {
    if (address.getId() != null) {
      addressCacheUtil.removeValue(ADDRESS_PREFIX + ":id:" + address.getId());
    }
    if (address.getLabel() != null && userId != null) {
      addressCacheUtil.removeValue(ADDRESS_PREFIX + ":user:" + userId + ":label:" + address.getLabel().toLowerCase());
    }
    stringCacheUtil.flushKeysByPattern(ADDRESS_PREFIX + ":ids:*");
    stringCacheUtil.flushKeysByPattern(ADDRESS_PREFIX + ":count:*");
  }

  private void evictUserAddressCaches(String userId) {
    stringCacheUtil.flushKeysByPattern(ADDRESS_PREFIX + ":user:" + userId + ":*");
    stringCacheUtil.flushKeysByPattern("user:id:" + userId);
  }

  @Override
  public UUID getDefaultAddressId(String userId) {
    if (StringUtils.isBlank(userId)) {
      return null;
    }
    UUID userUuid = UUID.fromString(userId);
    return userRepository.findById(userUuid)
        .map(User::getDefaultAddress)
        .map(Address::getId)
        .orElse(null);
  }

  @Override
  protected UUID toId(String input) {
    return UUID.fromString(input);
  }

  @Override
  protected UUID getId(Address input) {
    return input.getId();
  }
}

