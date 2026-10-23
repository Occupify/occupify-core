package com.occupify.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3Properties {

  @Nullable
  private String bucketNamespace;

  private String region = "ap-singapore-1";

  @Nullable
  private String endpoint;

  private String bucketName = "occupify-storage";

  @Nullable
  private String accessKey;

  @Nullable
  private String secretKey;

  @Nullable
  private String publicUrlPrefix;

  private boolean pathStyleAccessEnabled = true;
}
