package com.occupify.core.config;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class S3Config {

  private final S3Properties properties;

  @Bean
  public S3Client s3Client() {
    log.info(
        "Initializing S3Client for region: {}, bucket: {}, namespace: {}",
        properties.getRegion(),
        properties.getBucketName(),
        properties.getBucketNamespace());

    S3ClientBuilder builder = S3Client.builder();

    // 1. Configure Cloud Region
    if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
      builder.region(Region.of(properties.getRegion().trim()));
    } else {
      builder.region(Region.AP_SOUTHEAST_1);
    }

    // 2. Configure Credentials
    if (properties.getAccessKey() != null
        && !properties.getAccessKey().isBlank()
        && properties.getSecretKey() != null
        && !properties.getSecretKey().isBlank()) {
      log.info("Successfully loaded S3 credentials into StaticCredentialsProvider.");
      AwsBasicCredentials credentials =
          AwsBasicCredentials.create(
              properties.getAccessKey().trim(), properties.getSecretKey().trim());
      builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
    } else {
      log.warn(
          "S3 credentials not fully supplied (accessKey='{}', secretKey='{}'). Falling back to dummy credentials for development/testing.",
          properties.getAccessKey(),
          properties.getSecretKey());
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create("dummy-access-key", "dummy-secret-key")));
    }

    // 3. Configure Endpoint Override (OCI Object Storage, MinIO, or Cloudflare R2)
    if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
      builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
    } else if (properties.getBucketNamespace() != null
        && !properties.getBucketNamespace().isBlank()) {
      String derivedEndpoint =
          String.format(
              "https://%s.compat.objectstorage.%s.oraclecloud.com",
              properties.getBucketNamespace().trim(),
              properties.getRegion() != null ? properties.getRegion().trim() : "ap-singapore-1");
      log.info("Auto-derived OCI S3-compatible endpoint: {}", derivedEndpoint);
      builder.endpointOverride(URI.create(derivedEndpoint));
    }

    // 4. Configure Path-style Access (Required for OCI Object Storage & MinIO)
    S3Configuration s3Configuration =
        S3Configuration.builder()
            .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
            .build();
    builder.serviceConfiguration(s3Configuration);

    return builder.build();
  }
}
