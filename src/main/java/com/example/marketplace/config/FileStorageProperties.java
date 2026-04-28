package com.example.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record FileStorageProperties(
        String uploadDir,
        String publicBaseUrl,
        boolean s3Enabled,
        String s3Bucket,
        String s3Region
) {
}
