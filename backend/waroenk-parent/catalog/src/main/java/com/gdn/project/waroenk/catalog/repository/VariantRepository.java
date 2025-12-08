package com.gdn.project.waroenk.catalog.repository;

import com.gdn.project.waroenk.catalog.entity.Variant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VariantRepository extends MongoRepository<Variant, String> {
  Optional<Variant> findBySubSku(String subSku);
  List<Variant> findBySku(String sku);
  boolean existsBySubSku(String subSku);
  boolean existsBySku(String sku);
  Optional<Variant> findBySkuAndIsDefault(String sku, Boolean isDefault);
}
