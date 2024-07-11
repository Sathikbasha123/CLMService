package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ListCustomField {

	@JsonProperty("configurationType")
	private String configurationType = null;

	@JsonProperty("errorDetails")
	private ErrorDetails errorDetails = null;

	@JsonProperty("fieldId")
	private String fieldId = null;

	@JsonProperty("listItems")
	private java.util.List<String> listItems = null;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("required")
	private String required = null;

	@JsonProperty("show")
	private String show = null;

	@JsonProperty("value")
	private String value = null;

}
