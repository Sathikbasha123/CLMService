package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PropertyMetadata {

	@JsonProperty("options")
	private java.util.List<String> options = null;

	@JsonProperty("rights")
	private String rights = null;

}
