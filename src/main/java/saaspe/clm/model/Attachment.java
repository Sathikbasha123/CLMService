package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Attachment {

	@JsonProperty("accessControl")
	private String accessControl = null;

	@JsonProperty("attachmentId")
	private String attachmentId = null;

	@JsonProperty("attachmentType")
	private String attachmentType = null;

	@JsonProperty("data")
	private String data = null;

	@JsonProperty("label")
	private String label = null;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("remoteUrl")
	private String remoteUrl = null;

}
