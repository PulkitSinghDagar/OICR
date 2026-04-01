package com.idbiintech.OICR.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.idbiintech.OICR.dto.OcrExtractResponse;

public interface OcrExtractionService {

	OcrExtractResponse extractText(MultipartFile file) throws IOException, InterruptedException;
}
