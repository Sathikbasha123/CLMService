package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CustomFields {

	@JsonProperty("listCustomFields")
	private java.util.List<ListCustomField> listCustomFields = null;

	@JsonProperty("textCustomFields")
	private java.util.List<TextCustomField> textCustomFields = null;

}
