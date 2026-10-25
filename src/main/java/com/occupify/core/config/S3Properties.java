package com.occupify.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3Properties {

  private String bucketNamespace;
  private String region;
  private String endpoint;
  private String bucketName;
  private String accessKey;
  private String secretKey;
  private String publicUrlPrefix;
  private boolean pathStyleAccessEnabled;
  private long presignedDurationMinutes;
}
