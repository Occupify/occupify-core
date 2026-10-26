package com.occupify.core.constant;

import java.util.UUID;

public final class StorageConstants {

  private StorageConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static final String FOLDER_RESUMES = "resumes";

  public static final String DEFAULT_OCI_REGION = "ap-singapore-1";
  public static final String DEFAULT_AWS_REGION = "us-east-1";

  public static final String OCI_PUBLIC_URL_FORMAT =
      "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s";
  public static final String AWS_PUBLIC_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";

  /**
   * Builds the directory path for user bid CV/resume document: "resumes/{userId}"
   *
   * @param userId user unique identifier
   * @return directory path formatted as "resumes/{userId}"
   */
  public static String getResumePath(UUID userId) {
    return FOLDER_RESUMES + "/" + userId;
  }
}
