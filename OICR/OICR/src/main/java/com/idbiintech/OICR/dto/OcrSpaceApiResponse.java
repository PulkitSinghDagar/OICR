package com.idbiintech.OICR.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrSpaceApiResponse(
		@JsonProperty("ParsedResults") List<ParsedResult> parsedResults,
		@JsonProperty("IsErroredOnProcessing") boolean isErroredOnProcessing,
		@JsonProperty("ErrorMessage") String errorMessage,
		@JsonProperty("ErrorDetails") String errorDetails
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ParsedResult(@JsonProperty("ParsedText") String parsedText) {
	}
}
