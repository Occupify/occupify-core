package com.occupify.core.listener;

import com.occupify.core.entity.user.User;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.event.UserActivatedEvent;
import com.occupify.core.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

  private final UserRepository userRepository;

  @RabbitListener(queues = "${app.rabbitmq.user-queue:occupify.core.user.queue}")
  @Transactional
  public void handleUserActivated(UserActivatedEvent event) {
    if (event == null || event.userId() == null || !StringUtils.hasText(event.email())) {
      log.warn("[Event Listener] Received invalid UserActivatedEvent: {}", event);
      return;
    }

    String email = event.email().trim().toLowerCase();
    log.info(
        "[Event Listener] Processing user activated event for user [{}] ({})",
        event.userId(),
        email);

    User user =
        userRepository
            .findById(event.userId())
            .orElseGet(() -> userRepository.findByEmail(email).orElseGet(User::new));

    user.setId(event.userId());
    user.setEmail(email);
    if (StringUtils.hasText(event.fullName())) {
      user.setFullName(event.fullName().trim());
    }
    user.setRole(parseRole(event.role()));
    user.setStatus(parseStatus(event.status()));

    userRepository.save(user);
    log.info(
        "[Event Listener] Successfully synced activated user [{}] ({}) to core database",
        event.userId(),
        email);
  }

  private UserRole parseRole(String roleStr) {
    if (StringUtils.hasText(roleStr) && UserRole.isAdmin(roleStr)) {
      return UserRole.ADMIN;
    }
    return UserRole.USER;
  }

  private UserStatus parseStatus(String statusStr) {
    if (!StringUtils.hasText(statusStr)) {
      return UserStatus.ACTIVE;
    }
    try {
      return UserStatus.valueOf(statusStr.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("[Event Listener] Unknown user status [{}], defaulting to ACTIVE", statusStr);
      return UserStatus.ACTIVE;
    }
  }
}
