package com.occupify.core.config;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

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
    builder.region(resolveRegion());
    builder.credentialsProvider(resolveCredentialsProvider());

    URI endpoint = resolveEndpoint();
    if (endpoint != null) {
      builder.endpointOverride(endpoint);
    }

    S3Configuration s3Configuration =
        S3Configuration.builder()
            .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
            .build();
    builder.serviceConfiguration(s3Configuration);

    return builder.build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    log.info(
        "Initializing S3Presigner for region: {}, bucket: {}",
        properties.getRegion(),
        properties.getBucketName());

    S3Presigner.Builder builder = S3Presigner.builder();
    builder.region(resolveRegion());
    builder.credentialsProvider(resolveCredentialsProvider());

    URI endpoint = resolveEndpoint();
    if (endpoint != null) {
      builder.endpointOverride(endpoint);
    }

    S3Configuration s3Configuration =
        S3Configuration.builder()
            .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
            .build();
    builder.serviceConfiguration(s3Configuration);

    return builder.build();
  }

  private Region resolveRegion() {
    if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
      return Region.of(properties.getRegion().trim());
    }
    return Region.AP_SOUTHEAST_1;
  }

  private AwsCredentialsProvider resolveCredentialsProvider() {
    if (properties.getAccessKey() != null
        && !properties.getAccessKey().isBlank()
        && properties.getSecretKey() != null
        && !properties.getSecretKey().isBlank()) {
      AwsBasicCredentials credentials =
          AwsBasicCredentials.create(
              properties.getAccessKey().trim(), properties.getSecretKey().trim());
      return StaticCredentialsProvider.create(credentials);
    }
    log.warn(
        "S3 credentials not fully supplied. Falling back to dummy credentials for dev/testing.");
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create("dummy-access-key", "dummy-secret-key"));
  }

  private URI resolveEndpoint() {
    if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
      return URI.create(properties.getEndpoint().trim());
    }
    if (properties.getBucketNamespace() != null && !properties.getBucketNamespace().isBlank()) {
      String region =
          properties.getRegion() != null && !properties.getRegion().isBlank()
              ? properties.getRegion().trim()
              : "ap-singapore-1";
      String derivedEndpoint =
          String.format(
              "https://%s.compat.objectstorage.%s.oraclecloud.com",
              properties.getBucketNamespace().trim(), region);
      log.info("Auto-derived OCI S3-compatible endpoint: {}", derivedEndpoint);
      return URI.create(derivedEndpoint);
    }
    return null;
  }
}
