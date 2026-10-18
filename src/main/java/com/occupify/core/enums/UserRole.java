package com.occupify.core.enums;

public enum UserRole {
  ADMIN,
  USER;

  public static boolean isAdmin(String role) {
    return ADMIN.name().equalsIgnoreCase(role);
  }
}
