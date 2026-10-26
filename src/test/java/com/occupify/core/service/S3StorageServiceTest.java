package com.occupify.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.config.S3Properties;
import com.occupify.core.exception.storage.StorageErrorCode;
import com.occupify.core.exception.storage.StorageException;
import com.occupify.core.service.impl.S3StorageServiceImpl;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3StorageServiceTest {

  private S3Client s3Client;
  private S3Presigner s3Presigner;
  private S3Properties s3Properties;
  private StorageService storageService;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    s3Presigner = mock(S3Presigner.class);
    s3Properties = new S3Properties();
    s3Properties.setBucketName("test-bucket");
    s3Properties.setRegion("ap-singapore-1");
    storageService = new S3StorageServiceImpl(s3Client, s3Presigner, s3Properties);
  }

  @Test
  @DisplayName("Should upload private document and return object key")
  void shouldUploadPrivateDocument() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contract.pdf", "application/pdf", "dummy pdf content".getBytes());

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    String objectKey = storageService.uploadPrivateDocument("contracts/123", file);
    assertThat(objectKey).startsWith("contracts/123/").endsWith(".pdf");
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("Should reject empty or null files")
  void shouldRejectInvalidFile() {
    MockMultipartFile emptyFile =
        new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    assertThatThrownBy(() -> storageService.uploadPrivateDocument("contracts/123", emptyFile))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorCode())
        .isEqualTo(StorageErrorCode.FILE_EMPTY);

    assertThatThrownBy(() -> storageService.uploadPrivateDocument("contracts/123", null))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorCode())
        .isEqualTo(StorageErrorCode.FILE_EMPTY);
  }

  @Test
  @DisplayName("Should reject oversized files (> 10MB)")
  void shouldRejectOversizedFile() {
    byte[] largeContent = new byte[10 * 1024 * 1024 + 1];
    MockMultipartFile largeFile =
        new MockMultipartFile("file", "large.pdf", "application/pdf", largeContent);
    assertThatThrownBy(() -> storageService.uploadPrivateDocument("contracts/123", largeFile))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorCode())
        .isEqualTo(StorageErrorCode.FILE_TOO_LARGE);
  }

  @Test
  @DisplayName("Should reject disallowed document types")
  void shouldRejectDisallowedFileType() {
    MockMultipartFile badFile =
        new MockMultipartFile("file", "script.exe", "application/x-msdownload", "evil".getBytes());
    assertThatThrownBy(() -> storageService.uploadPrivateDocument("contracts/123", badFile))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorCode())
        .isEqualTo(StorageErrorCode.INVALID_FILE_TYPE);
  }

  @Test
  @DisplayName("Should throw StorageException on S3 upload failure")
  void shouldWrapS3ExceptionInStorageExceptionOnUpload() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contract.pdf", "application/pdf", "dummy pdf content".getBytes());
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(SdkClientException.create("S3 network failure"));

    assertThatThrownBy(() -> storageService.uploadPrivateDocument("contracts/123", file))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorCode())
        .isEqualTo(StorageErrorCode.UPLOAD_FAILED);
  }

  @Test
  @DisplayName("Should generate presigned download URL")
  void shouldGeneratePresignedDownloadUrl() throws Exception {
    PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
    when(presignedRequest.url()).thenReturn(new URL("https://test.oraclecloud.com/signed-url"));
    when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .thenReturn(presignedRequest);

    String signedUrl =
        storageService.generatePresignedDownloadUrl(
            "contracts/123/file.pdf", Duration.ofMinutes(15));
    assertThat(signedUrl).isEqualTo("https://test.oraclecloud.com/signed-url");
  }

  @Test
  @DisplayName("Should throw StorageException when presigning fails")
  void shouldWrapS3ExceptionInStorageExceptionOnPresign() {
    when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .thenThrow(SdkClientException.create("Presign failed"));

    assertThatThrownBy(
            () ->
                storageService.generatePresignedDownloadUrl(
                    "contracts/123/file.pdf", Duration.ofMinutes(15)))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorCode())
        .isEqualTo(StorageErrorCode.PRESIGN_FAILED);
  }

  @Test
  @DisplayName("Should delete file successfully")
  void shouldDeleteFile() {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    storageService.deleteFile("contracts/123/file.pdf");
    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("Should throw StorageException when delete fails")
  void shouldWrapS3ExceptionInStorageExceptionOnDelete() {
    doThrow(SdkClientException.create("Delete failed"))
        .when(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    assertThatThrownBy(() -> storageService.deleteFile("contracts/123/file.pdf"))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorCode())
        .isEqualTo(StorageErrorCode.DELETE_FAILED);
  }
}
