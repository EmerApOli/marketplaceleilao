package com.example.marketplace.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String storeProductImage(MultipartFile file);
}
