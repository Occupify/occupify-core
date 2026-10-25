package com.occupify.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ConfigTest {

  @Test
  @DisplayName("Should initialize S3Client and S3Presigner with explicit credentials and endpoint")
  void shouldInitWithExplicitEndpointAndCredentials() {
    S3Properties properties = new S3Properties();
    properties.setRegion("ap-singapore-1");
    properties.setEndpoint("https://my-custom-endpoint.com");
    properties.setAccessKey("my-access-key");
    properties.setSecretKey("my-secret-key");
    properties.setBucketName("custom-bucket");
    properties.setPathStyleAccessEnabled(true);

    S3Config config = new S3Config(properties);

    try (S3Client client = config.s3Client();
        S3Presigner presigner = config.s3Presigner()) {
      assertThat(client).isNotNull();
      assertThat(presigner).isNotNull();
    }
  }

  @Test
  @DisplayName("Should auto-derive OCI endpoint when bucketNamespace is set")
  void shouldAutoDeriveOciEndpoint() {
    S3Properties properties = new S3Properties();
    properties.setBucketNamespace("my-namespace");
    properties.setRegion("ap-singapore-1");

    S3Config config = new S3Config(properties);

    try (S3Client client = config.s3Client();
        S3Presigner presigner = config.s3Presigner()) {
      assertThat(client).isNotNull();
      assertThat(presigner).isNotNull();
    }
  }

  @Test
  @DisplayName("Should fall back to dummy credentials when credentials not supplied")
  void shouldFallbackToDummyCredentials() {
    S3Properties properties = new S3Properties();
    properties.setAccessKey(null);
    properties.setSecretKey(null);

    S3Config config = new S3Config(properties);

    try (S3Client client = config.s3Client();
        S3Presigner presigner = config.s3Presigner()) {
      assertThat(client).isNotNull();
      assertThat(presigner).isNotNull();
    }
  }
}
