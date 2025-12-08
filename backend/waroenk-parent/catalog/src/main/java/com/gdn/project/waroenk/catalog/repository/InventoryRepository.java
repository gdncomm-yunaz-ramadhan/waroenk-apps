package com.gdn.project.waroenk.catalog.repository;

import com.gdn.project.waroenk.catalog.entity.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends MongoRepository<Inventory, String> {
  Optional<Inventory> findBySubSku(String subSku);
  List<Inventory> findBySubSkuIn(List<String> subSkus);
  boolean existsBySubSku(String subSku);
}


