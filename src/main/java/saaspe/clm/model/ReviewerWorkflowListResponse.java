package saaspe.clm.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewerWorkflowListResponse {
	private String projectId;
	private String envelopeId;
	private String creatorName;
	private String senderEmail;
	private String contractName;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "GMT")
	private Date createdOn;
	// @JsonIgnore
	private String status;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date completionDate;
	private Boolean isFilesUploaded;
	private String subsidiary;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date expiryDate;
	private String vendorName;
	private String contractNumber;
	private String tenant;
}
