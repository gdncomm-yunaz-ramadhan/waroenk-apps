package com.gdn.project.waroenk.catalog.repository;

import com.gdn.project.waroenk.catalog.entity.Brand;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BrandRepository extends MongoRepository<Brand, String> {
  Optional<Brand> findBySlug(String slug);
  boolean existsBySlug(String slug);
}












