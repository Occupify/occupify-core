package com.occupify.core.entity;

import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends AbstractAuditEntity {

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "full_name", length = 150)
  private String fullName;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 50)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private UserStatus status;
}
