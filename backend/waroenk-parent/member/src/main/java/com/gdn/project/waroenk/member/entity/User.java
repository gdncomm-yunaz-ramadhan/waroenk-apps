package com.gdn.project.waroenk.member.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gdn.project.waroenk.member.constant.Gender;
import com.gdn.project.waroenk.member.entity.converter.GenderConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "fullname", nullable = false)
  private String fullName;

  private LocalDate dob;

  @Column(unique = true)
  private String email;

  @Column(name = "phone_number", unique = true)
  private String phoneNumber;

  @Convert(converter = GenderConverter.class)
  private Gender gender;

  @OneToOne
  @JoinColumn(name = "default_address_id")
  private Address defaultAddress;

  @JsonIgnore
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
