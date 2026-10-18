package com.occupify.core.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void testUserBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    User user =
        User.builder()
            .email("test@example.com")
            .fullName("John Doe")
            .avatarUrl("https://example.com/avatar.png")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .build();
    user.setId(id);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    user.setDeletedAt(now);

    assertEquals(id, user.getId());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("John Doe", user.getFullName());
    assertEquals("https://example.com/avatar.png", user.getAvatarUrl());
    assertEquals(UserRole.USER, user.getRole());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
    assertEquals(now, user.getCreatedAt());
    assertEquals(now, user.getUpdatedAt());
    assertEquals(now, user.getDeletedAt());
  }

  @Test
  void testUserWithNullOptionalFields() {
    UUID id = UUID.randomUUID();
    User user =
        User.builder()
            .email("minimal@example.com")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .build();
    user.setId(id);

    assertEquals(id, user.getId());
    assertEquals("minimal@example.com", user.getEmail());
    assertNull(user.getFullName());
    assertNull(user.getAvatarUrl());
    assertEquals(UserRole.USER, user.getRole());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
  }
}
