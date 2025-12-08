package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.Address;

import java.util.List;

public interface AddressCustomRepository {
  List<Address> findAddressLike(String user, String label, String cursor, int size, String sortField, String sortDirection);

  Long countAll(String user);
}
