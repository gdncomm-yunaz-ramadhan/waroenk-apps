package com.gdn.project.waroenk.catalog.repository;

import com.gdn.project.waroenk.catalog.entity.SystemParameter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SystemParameterRepository extends MongoRepository<SystemParameter, String> {
  Optional<SystemParameter> findByVariable(String variable);
  boolean existsByVariable(String variable);
}












