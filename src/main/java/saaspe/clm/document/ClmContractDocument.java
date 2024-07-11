package saaspe.clm.document;

import java.util.Date;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
@Document(collection = "ClmContractDocument")
public class ClmContractDocument {

	@Transient
	public static final String SEQUENCE_NAME = "clmContractDocumentsequence";

	private long id;

	private String opID;

	private String buID;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date createdOn;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date updatedOn;

	private String createdBy;

	private String updatedBy;

	private String contractName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date contractStartDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date contractEndDate;

	private int renewalReminderNotification;

	private String templateId;

	private String envelopeId;

	private int contractPeriod;

	private String senderEmail;

	private String status;

	private String referenceId;

	private String referenceType;

	private Boolean reviewerSigningOrder;

	private Boolean signerSigningOrder;

	private String signers;

	private String reviewers;

	private String newEnvelopeId;

	private int order;

	private String version;

	private String uniqueString;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date lastModifiedDateTime;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date commencementDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date completionDate;
}
