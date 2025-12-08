package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface AddressRepository extends JpaRepository<Address, UUID>, AddressCustomRepository {

  Optional<Address> findByLabel(String label);

  @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND LOWER(a.label) = LOWER(:label)")
  Optional<Address> findByUserIdAndLabel(@Param("userId") UUID userId, @Param("label") String label);
}
