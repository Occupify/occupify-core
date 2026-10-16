package com.occupify.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ScalarConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/docs").setViewName("forward:/scalar.html");
    registry.addViewController("/docs/").setViewName("forward:/scalar.html");
    registry.addViewController("/scalar").setViewName("forward:/scalar.html");
    registry.addViewController("/scalar/").setViewName("forward:/scalar.html");
  }
}
