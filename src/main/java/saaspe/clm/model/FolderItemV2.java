package saaspe.clm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FolderItemV2 {

	@JsonProperty("completedDateTime")
	private String completedDateTime = null;

	@JsonProperty("createdDateTime")
	private String createdDateTime = null;

	@JsonProperty("envelopeId")
	private String envelopeId = null;

	@JsonProperty("envelopeUri")
	private String envelopeUri = null;

	@JsonProperty("expireDateTime")
	private String expireDateTime = null;

	@JsonProperty("folderId")
	private String folderId = null;

	@JsonProperty("folderUri")
	private String folderUri = null;

	@JsonProperty("is21CFRPart11")
	private String is21CFRPart11 = null;

	@JsonProperty("ownerName")
	private String ownerName = null;

	@JsonProperty("recipients")
	private Recipients recipients = null;

	@JsonProperty("recipientsUri")
	private String recipientsUri = null;

	@JsonProperty("senderCompany")
	private String senderCompany = null;

	@JsonProperty("senderEmail")
	private String senderEmail = null;

	@JsonProperty("senderName")
	private String senderName = null;

	@JsonProperty("senderUserId")
	private String senderUserId = null;

	@JsonProperty("sentDateTime")
	private String sentDateTime = null;

	@JsonProperty("status")
	private String status = null;

	@JsonProperty("subject")
	private String subject = null;

	@JsonProperty("templateId")
	private String templateId = null;

	@JsonProperty("templateUri")
	private String templateUri = null;

}
