package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID>, UserCustomRepository {

  @Query("SELECT u FROM User u WHERE u.phoneNumber = :phone OR LOWER(u.email) = :email")
  Optional<User> findByPhoneNumberOrEmail(@Param("phone") String phone, @Param("email") String email);
}
