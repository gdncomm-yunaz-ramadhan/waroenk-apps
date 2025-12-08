package com.gdn.project.waroenk.member.service;

import com.gdn.project.waroenk.member.FilterAddressRequest;
import com.gdn.project.waroenk.member.MultipleAddressResponse;
import com.gdn.project.waroenk.member.entity.Address;

import java.util.UUID;

public interface AddressService {

  Address findAddressById(String id);

  Address findUserAddressByLabel(String userId, String label);

  Address createAddress(String userId, Address address, boolean setAsDefault);

  MultipleAddressResponse filterUserAddress(FilterAddressRequest request);

  boolean setDefaultAddress(String userId, String addressId);

  boolean deleteUserAddress(String id);

  /**
   * Get the default address ID for a user
   * @param userId the user's ID
   * @return the default address UUID, or null if no default is set
   */
  UUID getDefaultAddressId(String userId);
}
