package com.occupify.core.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  String uploadDocument(String directory, MultipartFile file);

  void deleteFile(String fileUrlOrKey);
}
