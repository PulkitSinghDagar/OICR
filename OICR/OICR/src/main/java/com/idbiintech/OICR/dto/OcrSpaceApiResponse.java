package com.idbiintech.OICR.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrSpaceApiResponse(
		@JsonProperty("ParsedResults") List<ParsedResult> parsedResults,
		@JsonProperty("IsErroredOnProcessing") boolean isErroredOnProcessing,
		@JsonProperty("ErrorMessage") JsonNode errorMessage,
		@JsonProperty("ErrorDetails") JsonNode errorDetails
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ParsedResult(@JsonProperty("ParsedText") String parsedText) {
	}
}
