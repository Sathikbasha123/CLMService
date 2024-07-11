package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Page {

	@JsonProperty("errorDetails")
	private ErrorDetails errorDetails = null;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("originalValue")
	private String originalValue = null;

	@JsonProperty("value")
	private String value = null;

}
