package com.idbiintech.OICR.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idbiintech.OICR.config.OcrSpaceProperties;
import com.idbiintech.OICR.dto.OcrExtractResponse;
import com.idbiintech.OICR.dto.OcrSpaceApiResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class OcrExtractionServiceImpl implements OcrExtractionService {

	private static final String CRLF = "\r\n";

	private final ObjectMapper objectMapper;
	private final OcrSpaceProperties ocrSpaceProperties;
	private final HttpClient httpClient;

	public OcrExtractionServiceImpl(OcrSpaceProperties ocrSpaceProperties) {
		this.objectMapper = new ObjectMapper();
		this.ocrSpaceProperties = ocrSpaceProperties;
		this.httpClient = HttpClient.newHttpClient();
	}

	@Override
	public OcrExtractResponse extractText(MultipartFile file) throws IOException, InterruptedException {
		validateFile(file);

		HttpRequest request = buildRequest(file);
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() >= 400) {
			throw new ResponseStatusException(BAD_GATEWAY,
					"OCR provider returned HTTP " + response.statusCode());
		}

		OcrSpaceApiResponse apiResponse = objectMapper.readValue(response.body(), OcrSpaceApiResponse.class);
		if (apiResponse.isErroredOnProcessing()) {
			String details = firstNonBlank(readJsonValue(apiResponse.errorMessage()), readJsonValue(apiResponse.errorDetails()),
					"OCR processing failed.");
			throw new ResponseStatusException(BAD_GATEWAY, details);
		}

		String rawText = apiResponse.parsedResults() == null
				? ""
				: apiResponse.parsedResults().stream()
						.map(OcrSpaceApiResponse.ParsedResult::parsedText)
						.filter(Objects::nonNull)
						.collect(Collectors.joining(System.lineSeparator()));

		return new OcrExtractResponse(
				file.getOriginalFilename(),
				file.getContentType(),
				rawText,
				extractKeyValuePairs(rawText));
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(BAD_REQUEST, "Please upload a non-empty image or PDF file.");
		}

		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null) {
			throw new ResponseStatusException(BAD_REQUEST, "Uploaded file name is missing.");
		}

		String lowerCaseName = originalFilename.toLowerCase();
		boolean supported = lowerCaseName.endsWith(".pdf")
				|| lowerCaseName.endsWith(".png")
				|| lowerCaseName.endsWith(".jpg")
				|| lowerCaseName.endsWith(".jpeg")
				|| lowerCaseName.endsWith(".bmp")
				|| lowerCaseName.endsWith(".gif")
				|| lowerCaseName.endsWith(".tif")
				|| lowerCaseName.endsWith(".tiff");

		if (!supported) {
			throw new ResponseStatusException(BAD_REQUEST,
					"Only PDF and common image formats are supported.");
		}
	}

	private HttpRequest buildRequest(MultipartFile file) throws IOException {
		String boundary = "----OICRBoundary" + UUID.randomUUID();
		byte[] body = buildMultipartBody(file, boundary);

		return HttpRequest.newBuilder()
				.uri(URI.create(ocrSpaceProperties.url()))
				.header("apikey", ocrSpaceProperties.key())
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build();
	}

	private byte[] buildMultipartBody(MultipartFile file, String boundary) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		writeTextPart(outputStream, boundary, "language", "eng");
		writeTextPart(outputStream, boundary, "isOverlayRequired", "false");
		writeTextPart(outputStream, boundary, "detectOrientation", "true");
		writeTextPart(outputStream, boundary, "scale", "true");
		writeFilePart(outputStream, boundary, "file", file);

		outputStream.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
		return outputStream.toByteArray();
	}

	private void writeTextPart(ByteArrayOutputStream outputStream, String boundary, String name, String value)
			throws IOException {
		outputStream.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
		outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF + CRLF)
				.getBytes(StandardCharsets.UTF_8));
		outputStream.write((value + CRLF).getBytes(StandardCharsets.UTF_8));
	}

	private void writeFilePart(ByteArrayOutputStream outputStream, String boundary, String name, MultipartFile file)
			throws IOException {
		outputStream.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
		outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
				+ file.getOriginalFilename() + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
		outputStream.write(("Content-Type: " + firstNonBlank(file.getContentType(), "application/octet-stream") + CRLF
				+ CRLF).getBytes(StandardCharsets.UTF_8));
		outputStream.write(file.getBytes());
		outputStream.write(CRLF.getBytes(StandardCharsets.UTF_8));
	}

	private Map<String, String> extractKeyValuePairs(String rawText) {
		Map<String, String> extractedFields = new LinkedHashMap<>();
		Arrays.stream(rawText.split("\\R"))
				.map(String::trim)
				.filter(line -> !line.isBlank())
				.forEach(line -> {
					String[] tokens = line.split("\\s*[:=-]\\s*", 2);
					if (tokens.length == 2 && !tokens[0].isBlank() && !tokens[1].isBlank()) {
						extractedFields.put(normalizeKey(tokens[0]), tokens[1].trim());
					}
				});
		return extractedFields;
	}

	private String normalizeKey(String key) {
		return key.trim()
				.toLowerCase()
				.replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
	}

	private String firstNonBlank(String... values) {
		return Arrays.stream(values)
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.findFirst()
				.orElse("");
	}

	private String readJsonValue(JsonNode node) {
		if (node == null || node.isNull()) {
			return "";
		}
		if (node.isArray()) {
			return Arrays.stream(objectMapper.convertValue(node, String[].class))
					.filter(Objects::nonNull)
					.map(String::trim)
					.filter(value -> !value.isEmpty())
					.collect(Collectors.joining(", "));
		}
		if (node.isTextual()) {
			return node.asText();
		}
		return node.toString();
	}
}
