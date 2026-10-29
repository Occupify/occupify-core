package com.occupify.core.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.entity.user.User;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.event.UserActivatedEvent;
import com.occupify.core.repository.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

  @Mock private UserRepository userRepository;

  private UserEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new UserEventListener(userRepository);
  }

  @Test
  @DisplayName("Should persist new user when receiving UserActivatedEvent")
  void shouldPersistNewUser() {
    UUID userId = UUID.randomUUID();
    UserActivatedEvent event =
        new UserActivatedEvent(
            userId,
            "TEST@OCCUPIFY.COM",
            "Jane Doe",
            "USER",
            "ACTIVE",
            Instant.parse("2026-10-25T10:00:00Z"));

    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    when(userRepository.findByEmail("test@occupify.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    listener.handleUserActivated(event);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();

    assertThat(saved.getId()).isEqualTo(userId);
    assertThat(saved.getEmail()).isEqualTo("test@occupify.com");
    assertThat(saved.getFullName()).isEqualTo("Jane Doe");
    assertThat(saved.getRole()).isEqualTo(UserRole.USER);
    assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should update existing user when receiving UserActivatedEvent")
  void shouldUpdateExistingUser() {
    UUID userId = UUID.randomUUID();
    User existingUser = new User();
    existingUser.setId(userId);
    existingUser.setEmail("old@occupify.com");
    existingUser.setFullName("Old Name");
    existingUser.setRole(UserRole.USER);
    existingUser.setStatus(UserStatus.INACTIVE);

    UserActivatedEvent event =
        new UserActivatedEvent(
            userId,
            "updated@occupify.com",
            "Updated Admin",
            "ADMIN",
            "ACTIVE",
            Instant.parse("2026-10-25T10:00:00Z"));

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    listener.handleUserActivated(event);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User updated = captor.getValue();

    assertThat(updated.getId()).isEqualTo(userId);
    assertThat(updated.getEmail()).isEqualTo("updated@occupify.com");
    assertThat(updated.getFullName()).isEqualTo("Updated Admin");
    assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should ignore invalid or null UserActivatedEvent")
  void shouldIgnoreInvalidEvent() {
    listener.handleUserActivated(null);
    listener.handleUserActivated(
        new UserActivatedEvent(null, "test@occupify.com", "Test", "USER", "ACTIVE", null));
    listener.handleUserActivated(
        new UserActivatedEvent(UUID.randomUUID(), "   ", "Test", "USER", "ACTIVE", null));

    verify(userRepository, never()).save(any());
  }
}
