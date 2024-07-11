package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Filter {

	@JsonProperty("actionRequired")
	private String actionRequired = null;

	@JsonProperty("expires")
	private String expires = null;

	@JsonProperty("folderIds")
	private String folderIds = null;

	@JsonProperty("fromDateTime")
	private String fromDateTime = null;

	@JsonProperty("isTemplate")
	private String isTemplate = null;

	@JsonProperty("order")
	private String order = null;

	@JsonProperty("orderBy")
	private String orderBy = null;

	@JsonProperty("searchTarget")
	private String searchTarget = null;

	@JsonProperty("searchText")
	private String searchText = null;

	@JsonProperty("status")
	private String status = null;

	@JsonProperty("toDateTime")
	private String toDateTime = null;

}
