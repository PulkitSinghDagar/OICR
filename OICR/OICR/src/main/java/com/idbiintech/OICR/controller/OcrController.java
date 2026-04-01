package com.idbiintech.OICR.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.idbiintech.OICR.dto.OcrExtractResponse;
import com.idbiintech.OICR.service.OcrExtractionService;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

	private final OcrExtractionService ocrExtractionService;

	public OcrController(OcrExtractionService ocrExtractionService) {
		this.ocrExtractionService = ocrExtractionService;
	}

	@PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public OcrExtractResponse extractText(@RequestPart("file") MultipartFile file)
			throws IOException, InterruptedException {
		return ocrExtractionService.extractText(file);
	}
}
