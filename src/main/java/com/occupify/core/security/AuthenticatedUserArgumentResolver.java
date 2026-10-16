package com.occupify.core.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.exception.system.SystemErrorCode;
import com.occupify.core.exception.system.SystemException;
import com.occupify.core.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Value("${app.auth.dev-mock.enabled:false}")
  private boolean devMockEnabled;

  @Value("${app.auth.dev-mock.user-id:00000000-0000-0000-0000-000000000001}")
  private String devMockUserId;

  @Value("${app.auth.dev-mock.user-role:CLIENT}")
  private String devMockUserRole;

  @Value("${app.auth.dev-mock.user-email:dev-employer@occupify.local}")
  private String devMockUserEmail;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(Authenticated.class)
        && UserContext.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

    String userIdHeader = request.getHeader("X-User-Id");
    String userRoleHeader = request.getHeader("X-User-Role");
    String userEmailHeader = request.getHeader("X-User-Email");

    // 1. Gateway header resolution (Production / Routed mode)
    if (StringUtils.hasText(userIdHeader)) {
      UUID userId;
      try {
        userId = UUID.fromString(userIdHeader.trim());
      } catch (IllegalArgumentException ex) {
        throw new SystemException(
            SystemErrorCode.INVALID_PARAMETER, "Invalid UUID format in X-User-Id header");
      }

      UserRole role = UserRole.CLIENT;
      if (StringUtils.hasText(userRoleHeader)) {
        try {
          role = UserRole.valueOf(userRoleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
          log.warn("[Security] Unknown X-User-Role '{}', defaulting to CLIENT", userRoleHeader);
        }
      }

      String email = StringUtils.hasText(userEmailHeader) ? userEmailHeader.trim() : "";
      if (devMockEnabled) {
        ensureDevUserExists(userId, email, role);
      }
      return new UserContext(userId, role, email);
    }

    // 2. Direct Bearer token resolution (Scalar / API Documentation / Local Dev mode)
    String authHeader = request.getHeader("Authorization");
    if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
      String token = authHeader.substring(7).trim();
      UserContext tokenContext = parseJwtPayload(token);
      if (tokenContext != null) {
        if (devMockEnabled) {
          ensureDevUserExists(tokenContext.userId(), tokenContext.email(), tokenContext.role());
        }
        return tokenContext;
      }
    }

    // 3. Fallback to mock user in dev environment
    if (devMockEnabled) {
      log.warn(
          "[Security] Missing authentication headers. Dev mock mode is ACTIVE. Falling back to mock user: {}",
          devMockUserId);
      UUID mockId = UUID.fromString(devMockUserId);
      UserRole mockRole = UserRole.valueOf(devMockUserRole.toUpperCase());
      ensureDevUserExists(mockId, devMockUserEmail, mockRole);
      return new UserContext(mockId, mockRole, devMockUserEmail);
    }

    throw new SystemException(
        SystemErrorCode.UNAUTHORIZED, "Missing required authentication header: X-User-Id");
  }

  private UserContext parseJwtPayload(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length < 2) {
        return null;
      }
      byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
      String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
      JsonNode jsonNode = objectMapper.readTree(payloadJson);

      String userIdStr = null;
      if (jsonNode.hasNonNull("userId")) {
        userIdStr = jsonNode.get("userId").asText();
      } else if (jsonNode.hasNonNull("sub")) {
        String sub = jsonNode.get("sub").asText();
        // Check if sub itself is a UUID
        try {
          UUID.fromString(sub);
          userIdStr = sub;
        } catch (IllegalArgumentException ignored) {
          // sub is likely email
        }
      }

      if (!StringUtils.hasText(userIdStr)) {
        log.warn("[Security] JWT payload does not contain a valid userId claim");
        return null;
      }

      UUID userId = UUID.fromString(userIdStr.trim());

      UserRole role = UserRole.CLIENT;
      if (jsonNode.hasNonNull("role")) {
        try {
          role = UserRole.valueOf(jsonNode.get("role").asText().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
          log.warn("[Security] Unknown role in JWT payload, defaulting to CLIENT");
        }
      }

      String email = "";
      if (jsonNode.hasNonNull("email")) {
        email = jsonNode.get("email").asText();
      } else if (jsonNode.hasNonNull("sub")) {
        email = jsonNode.get("sub").asText();
      }

      return new UserContext(userId, role, email);
    } catch (Exception ex) {
      log.warn("[Security] Failed to decode JWT payload from Bearer token: {}", ex.getMessage());
      return null;
    }
  }

  private void ensureDevUserExists(UUID userId, String email, UserRole role) {
    if (!userRepository.existsById(userId)) {
      String resolvedEmail =
          StringUtils.hasText(email) ? email : "user-" + userId + "@occupify.local";
      log.info(
          "[Security] Auto-provisioning mock development user [{}] ({})", userId, resolvedEmail);
      User devUser =
          User.builder()
              .email(resolvedEmail)
              .fullName(
                  "User "
                      + (StringUtils.hasText(email) ? email : userId.toString().substring(0, 8)))
              .avatarUrl("https://occupify.local/dev-avatar.png")
              .role(role)
              .status(UserStatus.ACTIVE)
              .build();
      devUser.setId(userId);
      userRepository.save(devUser);
    }
  }
}
