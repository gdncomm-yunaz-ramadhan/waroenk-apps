package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {
  Optional<Token> findByUserId(UUID userId);

  Optional<Token> findByRefreshToken(String refreshToken);

  void deleteByUserId(UUID userId);
}













