package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface SystemParameterRepository extends JpaRepository<SystemParameter, UUID>, SystemParameterCustomRepository {
  Optional<SystemParameter> findByVariable(String variable);
}
