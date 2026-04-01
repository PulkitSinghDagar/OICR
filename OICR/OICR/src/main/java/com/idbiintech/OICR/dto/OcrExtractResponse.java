package com.idbiintech.OICR.dto;

import java.util.Map;

public record OcrExtractResponse(
		String fileName,
		String contentType,
		String rawText,
		Map<String, String> extractedFields
) {
}
