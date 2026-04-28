package com.example.marketplace.service.storage;

import com.example.marketplace.config.FileStorageProperties;
import com.example.marketplace.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/jpg"
    );

    private final FileStorageProperties fileStorageProperties;

    @Override
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validate(file);

        try {
            Path uploadRoot = Paths.get(fileStorageProperties.uploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            String extension = extractExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
            Path destination = uploadRoot.resolve(filename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            if (fileStorageProperties.s3Enabled()) {
                // Preparado para futura troca por upload no S3.
                // Quando ativar o S3, substitua o retorno abaixo pela URL pública do objeto.
            }

            String baseUrl = normalizeBaseUrl(fileStorageProperties.publicBaseUrl());
            return baseUrl + "/uploads/" + filename;
        } catch (IOException exception) {
            throw new BusinessException("Não foi possível salvar a imagem do produto.");
        }
    }

    private void validate(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Envie uma imagem válida nos formatos JPG, PNG, WEBP ou GIF.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
