package com.occupify.core.security;

import com.occupify.core.enums.UserRole;
import java.util.UUID;

public record UserContext(UUID userId, UserRole role, String email) {
  public UUID getUserId() {
    return userId;
  }

  public boolean isAdmin() {
    return role == UserRole.ADMIN;
  }

  public boolean isUser() {
    return role == UserRole.USER;
  }
}
