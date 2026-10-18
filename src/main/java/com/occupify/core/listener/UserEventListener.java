package com.occupify.core.listener;

import com.occupify.core.entity.user.User;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.event.UserRegisteredEvent;
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
  public void handleUserRegistered(UserRegisteredEvent event) {
    if (event == null || event.userId() == null || !StringUtils.hasText(event.email())) {
      log.warn("[Event Listener] Received invalid UserRegisteredEvent: {}", event);
      return;
    }

    String email = event.email().trim().toLowerCase();
    log.info(
        "[Event Listener] Processing user registered event for user [{}] ({})",
        event.userId(),
        email);

    if (userRepository.existsById(event.userId())
        || userRepository.findByEmail(email).isPresent()) {
      log.info(
          "[Event Listener] User [{}] ({}) already exists in core database. Skipping sync.",
          event.userId(),
          email);
      return;
    }

    User user = User.builder().email(email).role(UserRole.USER).status(UserStatus.ACTIVE).build();
    user.setId(event.userId());

    userRepository.save(user);
    log.info(
        "[Event Listener] Successfully synced registered user [{}] ({}) to core database",
        event.userId(),
        email);
  }
}
