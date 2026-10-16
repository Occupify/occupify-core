package com.occupify.core.config;

import com.occupify.core.security.AuthenticatedUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;

  public WebMvcConfig(AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver) {
    super();
    this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(authenticatedUserArgumentResolver);
  }
}
