package com.idbiintech.OICR.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr.space.api")
public record OcrSpaceProperties(String url, String key) {
}
