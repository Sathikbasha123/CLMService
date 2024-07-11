package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ErrorDetails {

	@JsonProperty("errorCode")
	private String errorCode = null;

	@JsonProperty("message")
	private String message = null;
}
