package com.occupify.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.security.AuthenticatedUserArgumentResolver;
import java.lang.reflect.Type;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;
  private final ObjectMapper objectMapper;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(authenticatedUserArgumentResolver);
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    MappingJackson2HttpMessageConverter octetStreamConverter =
        new MappingJackson2HttpMessageConverter(objectMapper) {
          @Override
          public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
          }

          @Override
          public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
            return false;
          }
        };
    octetStreamConverter.setSupportedMediaTypes(
        List.of(
            MediaType.APPLICATION_OCTET_STREAM,
            MediaType.TEXT_PLAIN,
            new MediaType("text", "json")));
    converters.add(octetStreamConverter);
  }
}

