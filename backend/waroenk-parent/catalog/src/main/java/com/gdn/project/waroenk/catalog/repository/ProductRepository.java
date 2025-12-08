package com.gdn.project.waroenk.catalog.repository;

import com.gdn.project.waroenk.catalog.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
  Optional<Product> findBySku(String sku);
  boolean existsBySku(String sku);
  List<Product> findByMerchantCode(String merchantCode);
  List<Product> findByCategoryId(String categoryId);
  List<Product> findByBrandId(String brandId);
}












