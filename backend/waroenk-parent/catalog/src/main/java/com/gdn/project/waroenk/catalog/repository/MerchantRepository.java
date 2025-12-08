package com.gdn.project.waroenk.catalog.repository;

import com.gdn.project.waroenk.catalog.entity.Merchant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MerchantRepository extends MongoRepository<Merchant, String> {
  Optional<Merchant> findByCode(String code);
  boolean existsByCode(String code);
}













